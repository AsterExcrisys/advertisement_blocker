package com.asterexcrisys.adblocker.services;

import com.asterexcrisys.adblocker.tasks.UDPHandler;
import com.asterexcrisys.adblocker.types.ThreadContext;
import com.asterexcrisys.adblocker.types.UDPPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class TaskDispatcher implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskDispatcher.class);

    private final ExecutorService executor;
    private final BlockingQueue<UDPPacket> udpRequests;
    private final BlockingQueue<UDPPacket> udpResponses;
    private final List<Future<?>> udpHandlers;
    private final ThreadLocal<ThreadContext> contextManager;
    private final int requestsLimit;
    private final int minimumTasks;
    private final int maximumTasks;

    public TaskDispatcher(ExecutorService executor, BlockingQueue<UDPPacket> udpRequests, BlockingQueue<UDPPacket> udpResponses, List<Future<?>> udpHandlers, ThreadLocal<ThreadContext> contextManager, int requestsLimit, int minimumTasks, int maximumTasks) {
        this.executor = Objects.requireNonNull(executor);
        this.udpRequests = Objects.requireNonNull(udpRequests);
        this.udpResponses = Objects.requireNonNull(udpResponses);
        this.udpHandlers = Objects.requireNonNull(udpHandlers);
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
        if (udpRequests.size() < udpHandlers.size() * (requestsLimit - 10)) {
            if (udpHandlers.size() == minimumTasks) {
                return;
            }
            udpHandlers.getLast().cancel(true);
            udpHandlers.removeLast();
            LOGGER.info("The last handler thread dispatch was reverted");
            return;
        }
        if (udpRequests.size() > (udpHandlers.size() + 1) * (requestsLimit + 10)) {
            if (udpHandlers.size() == maximumTasks) {
                return;
            }
            udpHandlers.add(executor.submit(new UDPHandler(contextManager, udpRequests, udpResponses)));
            LOGGER.info("A new handler thread was dispatched");
        }
    }

}