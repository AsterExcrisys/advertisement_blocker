package com.asterexcrisys.adblocker;

import com.asterexcrisys.adblocker.filters.BlacklistFilter;
import com.asterexcrisys.adblocker.filters.WhitelistFilter;
import com.asterexcrisys.adblocker.matchers.ExactMatcher;
import com.asterexcrisys.adblocker.matchers.WildcardMatcher;
import com.asterexcrisys.adblocker.services.ProxyManager;
import com.asterexcrisys.adblocker.services.TaskDispatcher;
import com.asterexcrisys.adblocker.tasks.*;
import com.asterexcrisys.adblocker.models.types.ProxyMode;
import com.asterexcrisys.adblocker.models.records.TCPPacket;
import com.asterexcrisys.adblocker.models.records.ThreadContext;
import com.asterexcrisys.adblocker.models.records.UDPPacket;
import com.asterexcrisys.adblocker.utility.CommandUtility;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import javax.net.ssl.*;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
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
    private Path nameServers;

    @Parameters(index = "1", description = "The path to the file containing a list of filtered domains (one per line).", arity = "1")
    private Path filteredDomains;

    @Option(names = {"-sm", "--server-mode"}, description = "The mode of the server to use for receiving requests (either UDP-only, TCP-only, TLS-only, HTTP-only, HTTPS-only, or DEFAULT) (optional).", defaultValue = "UDP")
    private ProxyMode serverMode;

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
        if (!Files.exists(nameServers) || !Files.isRegularFile(nameServers)) {
            throw new IllegalArgumentException("name servers must be a file");
        }
        if (!Files.exists(filteredDomains) || !Files.isRegularFile(filteredDomains)) {
            throw new IllegalArgumentException("filtered domains must be a file");
        }
        if (serverMode == null) {
            throw new IllegalArgumentException("server mode must be specified as either 'UDP', 'TCP', 'TLS', 'HTTP', 'HTTPS', or 'DEFAULT'");
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
        try (
                ExecutorService executor = initializeExecutor();
                ScheduledExecutorService scheduler = initializeScheduler();
                DatagramSocket udpSocket = new DatagramSocket(serverPort);
                ServerSocket tcpSocket = new ServerSocket(serverPort)
        ) {
            BlockingQueue<UDPPacket> udpRequests = new LinkedBlockingQueue<>();
            BlockingQueue<UDPPacket> udpResponses = new LinkedBlockingQueue<>();
            BlockingQueue<TCPPacket> tcpRequests = new LinkedBlockingQueue<>();
            BlockingQueue<TCPPacket> tcpResponses = new LinkedBlockingQueue<>();
            List<Thread> readers = new ArrayList<>(1);
            List<Thread> writers = new ArrayList<>(1);
            final List<Future<?>> udpHandlers = new ArrayList<>(minimumTasks);
            final List<Future<?>> tcpHandlers = new ArrayList<>(minimumTasks);
            if (serverMode == ProxyMode.UDP || serverMode == ProxyMode.DEFAULT) {
                readers.add(new UDPReader(udpSocket, udpRequests));
                writers.add(new UDPWriter(udpSocket, udpResponses));
            }
            if (serverMode == ProxyMode.TCP || serverMode == ProxyMode.DEFAULT) {
                readers.add(new TCPReader(tcpSocket, tcpRequests));
                writers.add(new TCPWriter(tcpResponses));
            }
            for (int i = 0; i < readers.size(); i++) {
                readers.get(i).setDaemon(true);
                readers.get(i).start();
                writers.get(i).setDaemon(true);
                writers.get(i).start();
            }
            for (int i = 0; i < minimumTasks; i++) {
                if (serverMode == ProxyMode.UDP || serverMode == ProxyMode.DEFAULT) {
                    udpHandlers.add(executor.submit(new UDPHandler(contextManager, udpRequests, udpResponses)));
                }
                if (serverMode == ProxyMode.TCP || serverMode == ProxyMode.DEFAULT) {
                    tcpHandlers.add(executor.submit(new TCPHandler(contextManager, tcpRequests, tcpResponses)));
                }
            }
            LOGGER.info("DNS Ad-Blocking Proxy with mode '{}' started on port {}", serverMode, serverPort);
            TaskDispatcher dispatcher = new TaskDispatcher(executor, contextManager, requestsLimit, minimumTasks, maximumTasks);
            dispatcher.addUDP(udpRequests, udpResponses, udpHandlers);
            dispatcher.addTCP(tcpRequests, tcpResponses, tcpHandlers, false);
            scheduler.scheduleWithFixedDelay(dispatcher, 10000, 10000, TimeUnit.MILLISECONDS);
            Thread.currentThread().join();
            return ExitCode.OK;
        } catch (InterruptedException exception) {
            LOGGER.info("DNS Ad-Blocking Proxy is starting the shutdown sequence due to: {}", exception.getMessage());
            Thread.currentThread().interrupt();
            return ExitCode.SOFTWARE;
        } finally {
            for (int i = 0; i < contextPoolSize; i++) {
                availableContexts[i].clear();
            }
            contextManager.remove();
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

    public ServerSocket initializeServer(String certificate, char[] password, int serverPort) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ProxyCommand.class.getResourceAsStream("%s.jks".formatted(certificate)), password);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, password);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagerFactory.getKeyManagers(), null, null);
        SSLServerSocketFactory serverSocketFactory = context.getServerSocketFactory();
        return serverSocketFactory.createServerSocket(serverPort);
    }

    public HttpServer initializeWebServer(String certificate, char[] password, int serverPort) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ProxyCommand.class.getResourceAsStream("%s.jks".formatted(certificate)), password);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, password);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init((KeyStore) null);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
        HttpsServer server = HttpsServer.create(new InetSocketAddress(serverPort), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(context) {
            @Override
            public void configure(HttpsParameters parameters) {
                SSLEngine engine = getSSLContext().createSSLEngine();
                parameters.setNeedClientAuth(false);
                parameters.setProtocols(engine.getEnabledProtocols());
                parameters.setCipherSuites(engine.getEnabledCipherSuites());
                parameters.setSSLParameters(getSSLContext().getDefaultSSLParameters());
            }
        });
        return server;
    }

    public ThreadContext initializeContext() throws Exception {
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