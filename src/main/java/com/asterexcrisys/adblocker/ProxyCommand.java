package com.asterexcrisys.adblocker;

import com.asterexcrisys.adblocker.filters.BlacklistFilter;
import com.asterexcrisys.adblocker.filters.Filter;
import com.asterexcrisys.adblocker.filters.WhitelistFilter;
import com.asterexcrisys.adblocker.matchers.ExactMatcher;
import com.asterexcrisys.adblocker.matchers.Matcher;
import com.asterexcrisys.adblocker.matchers.WildcardMatcher;
import com.asterexcrisys.adblocker.resolvers.Resolver;
import com.asterexcrisys.adblocker.services.EvaluationManager;
import com.asterexcrisys.adblocker.services.ResolutionManager;
import com.asterexcrisys.adblocker.services.TaskDispatcher;
import com.asterexcrisys.adblocker.tasks.*;
import com.asterexcrisys.adblocker.models.types.ProxyMode;
import com.asterexcrisys.adblocker.models.packets.TCPPacket;
import com.asterexcrisys.adblocker.models.packets.UDPPacket;
import com.asterexcrisys.adblocker.utilities.CommandUtilities;
import com.asterexcrisys.adblocker.utilities.GlobalUtilities;
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
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

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

    @Option(names = {"-qt", "--queue-timeout"}, description = "The timeout in milliseconds for each incoming packet in the queue (if it does expire, another poll is attempted) (optional).", defaultValue = "5000")
    private int queueTimeout;

    @Option(names = {"-cl", "--cache-limit"}, description = "The cache limit of the proxy for DNS responses (optional).", defaultValue = "1000")
    private int cacheLimit;

    @Option(names = {"-rt", "--request-timeout"}, description = "The timeout in milliseconds for each incoming request (if it does expire, the request is rejected) (optional).", defaultValue = "5000")
    private int requestTimeout;

    @Option(names = {"-rl", "--requests-limit"}, description = "The requests limit per handler task (how many they should handle at maximum) (optional).", defaultValue = "100")
    private int requestsLimit;

    @Option(names = {"-cr", "--concurrent-requests"}, description = "The maximum number of concurrent requests that can exists at any given time (optional).", defaultValue = "10")
    private int concurrentRequests;

    @Option(names = {"-mnt", "--minimum-tasks"}, description = "The minimum number of handler tasks that exists at any given time (optional).", defaultValue = "5")
    private int minimumTasks;

    @Option(names = {"-mxt", "--maximum-tasks"}, description = "The maximum number of handler tasks that can exists at any given time (optional).", defaultValue = "10")
    private int maximumTasks;

    @Override
    public Integer call() throws Exception {
        if (nameServers == null || !Files.exists(nameServers) || !Files.isRegularFile(nameServers)) {
            throw new IllegalArgumentException("name servers must be a valid text file");
        }
        if (nameServers == null || !Files.exists(filteredDomains) || !Files.isRegularFile(filteredDomains)) {
            throw new IllegalArgumentException("filtered domains must be a valid text file");
        }
        if (serverMode == null) {
            throw new IllegalArgumentException("server mode must be specified as either 'UDP', 'TCP', 'TLS', 'HTTP', 'HTTPS', or 'DEFAULT'");
        }
        if (serverPort < 1 || serverPort > 65535) {
            throw new IllegalArgumentException("server port must be in the range [1, 65535]");
        }
        if (queueTimeout < 1000 || queueTimeout > 10000) {
            throw new IllegalArgumentException("requests limit must be in the range [1000, 10000]");
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
        if (concurrentRequests < 1 || concurrentRequests > 100) {
            throw new IllegalArgumentException("concurrent requests must be in the range [1, 100]");
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
        GlobalSettings settings = GlobalSettings.getInstance();
        settings.setQueueTimeout(queueTimeout);
        settings.setRequestTimeout(requestTimeout);
        EvaluationManager evaluationManager = initializeEvaluationManager();
        ResolutionManager resolutionManager = initializeResolutionManager();
        try (
                ExecutorService executor = initializeExecutor();
                ScheduledExecutorService scheduler = initializeScheduler();
                DatagramSocket udpSocket = new DatagramSocket(serverPort);
                ServerSocket tcpSocket = new ServerSocket(serverPort)
        ) {
            BlockingQueue<UDPPacket> udpRequests = new ArrayBlockingQueue<>(requestsLimit * maximumTasks);
            BlockingQueue<UDPPacket> udpResponses = new ArrayBlockingQueue<>(requestsLimit * maximumTasks);
            BlockingQueue<TCPPacket> tcpRequests = new ArrayBlockingQueue<>(requestsLimit * maximumTasks);
            BlockingQueue<TCPPacket> tcpResponses = new ArrayBlockingQueue<>(requestsLimit * maximumTasks);
            List<Thread> readers = new ArrayList<>(5);
            List<Thread> writers = new ArrayList<>(5);
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
                    udpHandlers.add(executor.submit(new UDPHandler(evaluationManager, resolutionManager, udpRequests, udpResponses)));
                }
                if (serverMode == ProxyMode.TCP || serverMode == ProxyMode.DEFAULT) {
                    tcpHandlers.add(executor.submit(new TCPHandler(evaluationManager, resolutionManager, tcpRequests, tcpResponses)));
                }
            }
            LOGGER.info("DNS Ad-Blocking Proxy with mode '{}' started on port {}", serverMode, serverPort);
            TaskDispatcher dispatcher = new TaskDispatcher(executor, evaluationManager, resolutionManager, requestsLimit, minimumTasks, maximumTasks);
            dispatcher.addUDP(udpRequests, udpResponses, udpHandlers);
            dispatcher.addTCP(tcpRequests, tcpResponses, tcpHandlers, false);
            scheduler.scheduleWithFixedDelay(dispatcher, 10000, 10000, TimeUnit.MILLISECONDS);
            Thread.currentThread().join();
            return ExitCode.OK;
        } catch (InterruptedException exception) {
            LOGGER.info("DNS Ad-Blocking Proxy is starting the shutdown sequence due to: {}", exception.getMessage());
            Thread.currentThread().interrupt();
            return ExitCode.SOFTWARE;
        }
    }

    public ExecutorService initializeExecutor() {
        return new ThreadPoolExecutor(
                minimumTasks,
                Runtime.getRuntime().availableProcessors(),
                60000,
                TimeUnit.MILLISECONDS,
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

    public EvaluationManager initializeEvaluationManager() throws IOException {
        List<String> domains = CommandUtilities.parseFilteredDomains(filteredDomains);
        Matcher matcher = GlobalUtilities.switchReturn(isWildcard, () -> {
            return new WildcardMatcher(domains);
        }, () -> {
            return new ExactMatcher(domains);
        });
        Filter filter = GlobalUtilities.switchReturn(isWhitelist, () -> {
            return new WhitelistFilter(matcher);
        }, () -> {
            return new BlacklistFilter(matcher);
        });
        EvaluationManager manager = new EvaluationManager(filter);
        manager.setCacheMaximumSize(cacheLimit);
        return manager;
    }

    public ResolutionManager initializeResolutionManager() throws Exception {
        List<Resolver> resolvers = CommandUtilities.parseNameServers(nameServers);
        return new ResolutionManager(shouldRetry, concurrentRequests, resolvers);
    }

}