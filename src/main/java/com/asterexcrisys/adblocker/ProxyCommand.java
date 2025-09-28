package com.asterexcrisys.adblocker;

import com.asterexcrisys.adblocker.filters.BlacklistFilter;
import com.asterexcrisys.adblocker.filters.WhitelistFilter;
import com.asterexcrisys.adblocker.matchers.ExactMatcher;
import com.asterexcrisys.adblocker.matchers.WildcardMatcher;
import com.asterexcrisys.adblocker.services.ProxyManager;
import com.asterexcrisys.adblocker.services.TaskDispatcher;
import com.asterexcrisys.adblocker.tasks.*;
import com.asterexcrisys.adblocker.types.ServerMode;
import com.asterexcrisys.adblocker.types.ThreadContext;
import com.asterexcrisys.adblocker.types.UDPPacket;
import com.asterexcrisys.adblocker.utility.CommandUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("unused")
@Command(name = "proxy", mixinStandardHelpOptions = true, version = "1.0.0", description = "Starts a DNS Ad-Blocking Proxy with the given, or otherwise default (if applicable), parameters.")
public class ProxyCommand implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyCommand.class);

    @Parameters(index = "0", description = "The path to the file containing a list of DNS name servers (resolvers) (one per line).", arity = "1")
    private File nameServers;

    @Parameters(index = "1", description = "The path to the file containing a list of filtered domains (one per line).", arity = "1")
    private File filteredDomains;

    @Option(names = {"-sm", "--server-mode"}, description = "The mode of the server to use for receiving requests (either UDP-only, TCP-only, or both) (optional).", defaultValue = "BOTH")
    private ServerMode serverMode;

    @Option(names = {"-sp", "--server-port"}, description = "The port on which the server should receive requests and send responses (optional).", defaultValue = "53")
    private int serverPort;

    @Option(names = {"-sr", "--should-retry"}, description = "The flag to signal the proxy whether it should retry failed requests (RC != NO_ERROR) with all the available resolvers (optional).", defaultValue = "false")
    private boolean shouldRetry;

    @Option(names = {"-wl", "--is-whitelist"}, description = "The flag to signal the proxy whether it should use a blacklist or whitelist filter (optional).", defaultValue = "false")
    private boolean isWhitelist;

    @Option(names = {"-wc", "--is-wildcard"}, description = "The flag to signal the proxy whether it should use an exact or a wildcard matcher (optional).", defaultValue = "false")
    private boolean isWildcard;

    @Option(names = {"-cl", "--cache-limit"}, description = "The cache limit of the proxy for DNS responses (optional).", defaultValue = "1000")
    private int cacheLimit;

    @Option(names = {"-rt", "--request-timeout"}, description = "The timeout in milliseconds for each incoming request (if it does expire, the request is rejected) (optional).", defaultValue = "5000")
    private int requestTimeout;

    @Option(names = {"-rl", "--requests-limit"}, description = "The requests limit per handler task (how many they should handle at maximum) (optional).", defaultValue = "100")
    private int requestsLimit;

    @Option(names = {"-mnt", "--minimum-tasks"}, description = "The minimum number of handler tasks that exists at any given time (optional).", defaultValue = "5")
    private int minimumTasks;

    @Option(names = {"-mxt", "--maximum-tasks"}, description = "The maximum number of handler tasks that can exists at any given time (optional).", defaultValue = "10")
    private int maximumTasks;

    @Override
    public Integer call() throws Exception {
        if (!nameServers.exists() || !nameServers.isFile()) {
            throw new IllegalArgumentException("name servers must be a file");
        }
        if (!filteredDomains.exists() || !filteredDomains.isFile()) {
            throw new IllegalArgumentException("filtered domains must be a file");
        }
        if (serverMode == null) {
            throw new IllegalArgumentException("server mode must be specified as either 'UDP', 'TCP', or 'BOTH'");
        }
        if (serverPort < 1 || serverPort > 65535) {
            throw new IllegalArgumentException("server port must be in the range [1, 65535]");
        }
        if (cacheLimit < 0 || cacheLimit > 10000) {
            throw new IllegalArgumentException("cache limit must be in the range [0, 10000]");
        }
        if (requestTimeout < 1000 || requestTimeout > 5000) {
            throw new IllegalArgumentException("requests limit must be in the range [1000, 5000]");
        }
        if (requestsLimit < 1 || requestsLimit > 1000) {
            throw new IllegalArgumentException("requests limit must be in the range [1, 1000]");
        }
        if (minimumTasks < 1 || minimumTasks > 1000) {
            throw new IllegalArgumentException("minimum tasks must be in the range [1, 1000]");
        }
        if (maximumTasks < 1 || maximumTasks > 1000) {
            throw new IllegalArgumentException("maximum tasks must be in the range [1, 1000]");
        }
        if (minimumTasks > maximumTasks) {
            throw new IllegalArgumentException("minimum tasks must not exceed maximum tasks");
        }
        GlobalSettings.getInstance().setRequestTimeout(requestTimeout);
        try (
                ExecutorService executor = initializeExecutor();
                ScheduledExecutorService scheduler = initializeScheduler();
                DatagramSocket udpSocket = new DatagramSocket(serverPort);
                ServerSocket tcpSocket = new ServerSocket(serverPort);
        ) {
            final int contextPoolSize = Math.min(
                    Math.max(Math.floorDiv(maximumTasks, 5), 1),
                    Runtime.getRuntime().availableProcessors()
            );
            final ThreadContext[] availableContexts = new ThreadContext[contextPoolSize];
            for (int i = 0; i < contextPoolSize; i++) {
                availableContexts[i] = initializeContext();
            }
            AtomicInteger taskCounter = new AtomicInteger(0);
            ThreadLocal<ThreadContext> contextManager = ThreadLocal.withInitial(() -> {
                int contextIndex = taskCounter.getAndIncrement() % contextPoolSize;
                return availableContexts[contextIndex];
            });
            BlockingQueue<UDPPacket> udpRequests = new LinkedBlockingQueue<>();
            BlockingQueue<UDPPacket> udpResponses = new LinkedBlockingQueue<>();
            Thread udpReader = new UDPReader(udpSocket, udpRequests);
            Thread udpWriter = new UDPWriter(udpSocket, udpResponses);
            final List<Future<?>> udpHandlers = new ArrayList<>(minimumTasks);
            udpReader.setDaemon(true);
            udpReader.start();
            udpWriter.setDaemon(true);
            udpWriter.start();
            for (int i = 0; i < minimumTasks; i++) {
                udpHandlers.add(executor.submit(new UDPHandler(contextManager, udpRequests, udpResponses)));
            }
            LOGGER.info("DNS Ad-Blocking Proxy started on port {}", serverPort);
            scheduler.scheduleWithFixedDelay(new TaskDispatcher(
                    executor,
                    udpRequests,
                    udpResponses,
                    udpHandlers,
                    contextManager,
                    requestsLimit,
                    minimumTasks,
                    maximumTasks
            ), 10000, 10000, TimeUnit.MILLISECONDS);
            Thread.currentThread().join();
            return ExitCode.OK;
        } finally {
            Thread.currentThread().interrupt();
        }
    }

    public ExecutorService initializeExecutor() {
        return new ThreadPoolExecutor(
                minimumTasks,
                Runtime.getRuntime().availableProcessors(),
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(maximumTasks),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public ScheduledExecutorService initializeScheduler() {
        return Executors.newSingleThreadScheduledExecutor((task) -> {
            Thread thread = new Thread(task);
            thread.setDaemon(false);
            return thread;
        });
    }

    public ThreadContext initializeContext() throws IOException {
        ReentrantLock lock = new ReentrantLock();
        ProxyManager manager = new ProxyManager(shouldRetry);
        manager.setFilter(isWhitelist? new WhitelistFilter():new BlacklistFilter());
        manager.setFilterMatcher(isWildcard? new WildcardMatcher():new ExactMatcher());
        manager.addResolvers(CommandUtility.parseNameServers(nameServers));
        manager.addFilteredDomains(CommandUtility.parseFilteredDomains(filteredDomains));
        manager.setCacheMaximumSize(cacheLimit);
        return ThreadContext.of(lock, manager);
    }

}