package com.asterexcrisys.adblocker.services;

import com.asterexcrisys.adblocker.dispatchers.Dispatcher;
import com.asterexcrisys.adblocker.dispatchers.HTTPDispatcher;
import com.asterexcrisys.adblocker.dispatchers.TCPDispatcher;
import com.asterexcrisys.adblocker.dispatchers.UDPDispatcher;
import com.asterexcrisys.adblocker.models.records.HTTPPacket;
import com.asterexcrisys.adblocker.models.records.TCPPacket;
import com.asterexcrisys.adblocker.models.records.ThreadContext;
import com.asterexcrisys.adblocker.models.records.UDPPacket;
import com.asterexcrisys.adblocker.models.types.DispatchType;
import com.asterexcrisys.adblocker.models.types.ProxyMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@SuppressWarnings("unused")
public class TaskDispatcher implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskDispatcher.class);

    private final Map<ProxyMode, Dispatcher> dispatchers;
    private final ExecutorService executor;
    private final ThreadLocal<ThreadContext> contextManager;
    private final int requestsLimit;
    private final int minimumTasks;
    private final int maximumTasks;
    private final boolean hasFallback;

    public TaskDispatcher() {
        dispatchers = new HashMap<>();
        executor = null;
        contextManager = null;
        requestsLimit = 0;
        minimumTasks = 0;
        maximumTasks = 0;
        hasFallback = false;
    }

    public TaskDispatcher(ExecutorService executor, ThreadLocal<ThreadContext> contextManager, int requestsLimit, int minimumTasks, int maximumTasks) {
        dispatchers = new HashMap<>();
        this.executor = Objects.requireNonNull(executor);
        this.contextManager = Objects.requireNonNull(contextManager);
        this.requestsLimit = requestsLimit;
        this.minimumTasks = minimumTasks;
        this.maximumTasks = maximumTasks;
        this.hasFallback = true;
    }

    public void add(Dispatcher dispatcher) {
        if (dispatcher == null) {
            return;
        }
        dispatchers.put(dispatcher.mode(), dispatcher);
    }

    public void addUDP(BlockingQueue<UDPPacket> udpRequests, BlockingQueue<UDPPacket> udpResponses, List<Future<?>> udpHandlers) {
        if (!hasFallback) {
            return;
        }
        if (udpRequests == null || udpResponses == null || udpHandlers == null) {
            return;
        }
        dispatchers.put(ProxyMode.UDP, new UDPDispatcher(
                executor,
                udpRequests,
                udpResponses,
                udpHandlers,
                contextManager,
                requestsLimit,
                minimumTasks,
                maximumTasks
        ));
    }

    public void addTCP(BlockingQueue<TCPPacket> tcpRequests, BlockingQueue<TCPPacket> tcpResponses, List<Future<?>> tcpHandlers, boolean isSecure) {
        if (!hasFallback) {
            return;
        }
        if (tcpRequests == null || tcpResponses == null || tcpHandlers == null) {
            return;
        }
        dispatchers.put(isSecure? ProxyMode.TLS:ProxyMode.TCP, new TCPDispatcher(
                executor,
                tcpRequests,
                tcpResponses,
                tcpHandlers,
                contextManager,
                requestsLimit,
                minimumTasks,
                maximumTasks,
                isSecure
        ));
    }

    public void addHTTP(BlockingQueue<HTTPPacket> httpRequests, BlockingQueue<HTTPPacket> httpResponses, List<Future<?>> httpHandlers, boolean isSecure) {
        if (!hasFallback) {
            return;
        }
        if (httpRequests == null || httpResponses == null || httpHandlers == null) {
            return;
        }
        dispatchers.put(isSecure? ProxyMode.HTTPS:ProxyMode.HTTP, new HTTPDispatcher(
                executor,
                httpRequests,
                httpResponses,
                httpHandlers,
                contextManager,
                requestsLimit,
                minimumTasks,
                maximumTasks,
                isSecure
        ));
    }

    public void remove(ProxyMode mode) {
        if (mode == null) {
            return;
        }
        dispatchers.remove(mode);
    }

    public void clear() {
        dispatchers.clear();
    }

    @Override
    public void run() {
        for (Dispatcher dispatcher : dispatchers.values()) {
            DispatchType result = dispatcher.dispatch();
            if (result == DispatchType.ADDED) {
                LOGGER.info("A new {} handler task was dispatched", dispatcher.mode().name());
            }
            if (result == DispatchType.REMOVED) {
                LOGGER.info("The last {} handler task dispatch was reverted", dispatcher.mode().name());
            }
        }
    }

}