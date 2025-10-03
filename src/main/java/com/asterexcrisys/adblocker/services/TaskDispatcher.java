package com.asterexcrisys.adblocker.services;

import com.asterexcrisys.adblocker.tasks.TCPHandler;
import com.asterexcrisys.adblocker.tasks.UDPHandler;
import com.asterexcrisys.adblocker.models.types.DispatchType;
import com.asterexcrisys.adblocker.models.records.TCPPacket;
import com.asterexcrisys.adblocker.models.records.ThreadContext;
import com.asterexcrisys.adblocker.models.records.UDPPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class TaskDispatcher implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskDispatcher.class);

    private final ExecutorService executor;
    private final BlockingQueue<UDPPacket> udpRequests;
    private final BlockingQueue<UDPPacket> udpResponses;
    private final BlockingQueue<TCPPacket> tcpRequests;
    private final BlockingQueue<TCPPacket> tcpResponses;
    private final List<Future<?>> udpHandlers;
    private final List<Future<?>> tcpHandlers;
    private final ThreadLocal<ThreadContext> contextManager;
    private final int requestsLimit;
    private final int minimumTasks;
    private final int maximumTasks;

    public TaskDispatcher(ExecutorService executor, BlockingQueue<UDPPacket> udpRequests, BlockingQueue<UDPPacket> udpResponses, BlockingQueue<TCPPacket> tcpRequests, BlockingQueue<TCPPacket> tcpResponses, List<Future<?>> udpHandlers, List<Future<?>> tcpHandlers, ThreadLocal<ThreadContext> contextManager, int requestsLimit, int minimumTasks, int maximumTasks) {
        this.executor = Objects.requireNonNull(executor);
        this.udpRequests = Objects.requireNonNull(udpRequests);
        this.udpResponses = Objects.requireNonNull(udpResponses);
        this.tcpRequests = Objects.requireNonNull(tcpRequests);
        this.tcpResponses = Objects.requireNonNull(tcpResponses);
        this.udpHandlers = Objects.requireNonNull(udpHandlers);
        this.tcpHandlers = Objects.requireNonNull(tcpHandlers);
        this.contextManager = Objects.requireNonNull(contextManager);
        this.requestsLimit = requestsLimit;
        this.minimumTasks = minimumTasks;
        this.maximumTasks = maximumTasks;
    }

    @Override
    public void run() {
        if (Thread.currentThread().isInterrupted()) {
            return;
        }
        DispatchType result = handleDispatch(udpRequests, udpHandlers, () -> new UDPHandler(contextManager, udpRequests, udpResponses));
        if (result == DispatchType.ADDED) {
            LOGGER.info("A new UDP handler task was dispatched");
        }
        if (result == DispatchType.REMOVED) {
            LOGGER.info("The last UDP handler task dispatch was reverted");
        }
        result = handleDispatch(tcpRequests, tcpHandlers, () -> new TCPHandler(contextManager, tcpRequests, tcpResponses));
        if (result == DispatchType.ADDED) {
            LOGGER.info("A new TCP handler task was dispatched");
        }
        if (result == DispatchType.REMOVED) {
            LOGGER.info("The last TCP handler task dispatch was reverted");
        }
    }

    private DispatchType handleDispatch(BlockingQueue<?> requests, List<Future<?>> handlers, Supplier<Runnable> task) {
        if (requests.size() < handlers.size() * (requestsLimit - 10)) {
            if (handlers.size() == minimumTasks) {
                return DispatchType.NONE;
            }
            handlers.getLast().cancel(true);
            handlers.removeLast();
            return DispatchType.REMOVED;
        }
        if (requests.size() > (handlers.size() + 1) * (requestsLimit + 10)) {
            if (handlers.size() == maximumTasks) {
                return DispatchType.NONE;
            }
            handlers.add(executor.submit(task.get()));
            return DispatchType.ADDED;
        }
        return DispatchType.NONE;
    }

}