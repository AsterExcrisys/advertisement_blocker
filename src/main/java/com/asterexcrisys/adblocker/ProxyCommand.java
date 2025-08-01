package com.asterexcrisys.adblocker;

import com.asterexcrisys.adblocker.filters.BlacklistFilter;
import com.asterexcrisys.adblocker.filters.WhitelistFilter;
import com.asterexcrisys.adblocker.matchers.ExactMatcher;
import com.asterexcrisys.adblocker.matchers.WildcardMatcher;
import com.asterexcrisys.adblocker.services.ProxyManager;
import com.asterexcrisys.adblocker.threads.Handler;
import com.asterexcrisys.adblocker.threads.Reader;
import com.asterexcrisys.adblocker.threads.Writer;
import com.asterexcrisys.adblocker.types.UDPPacket;
import com.asterexcrisys.adblocker.utility.CommandUtility;
import com.asterexcrisys.adblocker.utility.GlobalUtility;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import java.io.File;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@SuppressWarnings("unused")
@Command(name = "proxy", mixinStandardHelpOptions = true, version = "1.0.0", description = "Starts a DNS Ad-Blocking Proxy with the given, or otherwise default (if applicable), parameters.")
public class ProxyCommand implements Runnable {

    @Parameters(index = "0", description = "The path to the file containing a list of DNS name servers (resolvers) (one per line).", arity = "1")
    private File nameServers;

    @Parameters(index = "1", description = "The path to the file containing a list of filtered domains (one per line).", arity = "1")
    private File filteredDomains;

    @Option(names = {"-wl", "--is-whitelist"}, description = "The flag to signal the proxy whether it should use a blacklist or whitelist filter (optional).", defaultValue = "false")
    private boolean isWhitelist;

    @Option(names = {"-wc", "--is-wildcard"}, description = "The flag to signal the proxy whether it should use an exact or a wildcard matcher (optional).", defaultValue = "false")
    private boolean isWildcard;

    @Option(names = {"-p", "--server-port"}, description = "The port on which the server should receive requests and send responses (optional).", defaultValue = "53")
    private int serverPort;

    @Option(names = {"-c", "--cache-limit"}, description = "The cache limit of the proxy for DNS responses (optional).", defaultValue = "1000")
    private int cacheLimit;

    @Option(names = {"-r", "--requests-limit"}, description = "The requests limit per handler threads (how many they should handle at maximum) (optional).", defaultValue = "100")
    private int requestsLimit;

    @Option(names = {"-min", "--minimum-threads"}, description = "The minimum number of handler threads that exists at any given time (optional).", defaultValue = "1")
    private int minimumThreads;

    @Option(names = {"-max", "--maximum-threads"}, description = "The maximum number of handler threads that can exists at any given time (optional).", defaultValue = "10")
    private int maximumThreads;

    @Override
    public void run() {
        if (!nameServers.exists() || !nameServers.isFile()) {
            throw new IllegalArgumentException("name servers must be a file");
        }
        if (!filteredDomains.exists() || !filteredDomains.isFile()) {
            throw new IllegalArgumentException("filtered domains must be a file");
        }
        if (serverPort < 1 || serverPort > 65535) {
            throw new IllegalArgumentException("server port must be in the range [1, 65535]");
        }
        if (cacheLimit < 0 || cacheLimit > 10000) {
            throw new IllegalArgumentException("cache limit must be in the range [0, 10000]");
        }
        if (requestsLimit < 1 || requestsLimit > 1000) {
            throw new IllegalArgumentException("requests limit must be in the range [1, 1000]");
        }
        if (minimumThreads < 1 || minimumThreads > 10000) {
            throw new IllegalArgumentException("minimum threads must be in the range [1, 10000]");
        }
        if (maximumThreads < 1 || maximumThreads > 10000) {
            throw new IllegalArgumentException("maximum threads must be in the range [1, 10000]");
        }
        if (minimumThreads > maximumThreads) {
            throw new IllegalArgumentException("minimum threads must not exceed maximum threads");
        }
        try (DatagramSocket socket = new DatagramSocket(serverPort)) {
            ProxyManager manager = new ProxyManager();
            manager.setFilter(isWhitelist? new WhitelistFilter():new BlacklistFilter());
            manager.setFilterMatcher(isWildcard? new WildcardMatcher():new ExactMatcher());
            manager.addResolvers(CommandUtility.parseNameServers(nameServers));
            manager.addFilteredDomains(CommandUtility.parseFilteredDomains(filteredDomains));
            manager.setCacheMaximumSize(cacheLimit);
            BlockingQueue<UDPPacket> requests = new LinkedBlockingQueue<>();
            BlockingQueue<UDPPacket> responses = new LinkedBlockingQueue<>();
            Thread reader = new Reader(socket, requests);
            Thread writer = new Writer(socket, responses);
            List<Thread> handlers = new ArrayList<>(GlobalUtility.fillList(
                    () -> new Handler(manager, requests, responses),
                    minimumThreads
            ));
            reader.setDaemon(true);
            reader.start();
            writer.setDaemon(true);
            writer.start();
            for (Thread handler : handlers) {
                handler.setDaemon(true);
                handler.start();
            }
            System.out.println("DNS Ad-Blocking Proxy started on port: " + serverPort);
            while (!Thread.currentThread().isInterrupted()) {
                if (requests.size() < handlers.size() * (requestsLimit - 10)) {
                    if (handlers.size() == minimumThreads) {
                        continue;
                    }
                    handlers.getLast().interrupt();
                    handlers.removeLast();
                    System.err.println("The last handler thread dispatch was reverted");
                    continue;
                }
                if (requests.size() > (handlers.size() + 1) * (requestsLimit + 10)) {
                    if (handlers.size() == maximumThreads) {
                        continue;
                    }
                    handlers.add(new Handler(manager, requests, responses));
                    handlers.getLast().setDaemon(true);
                    handlers.getLast().start();
                    System.err.println("A new handler thread was dispatched");
                }
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            System.out.println("Exception caught: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

}