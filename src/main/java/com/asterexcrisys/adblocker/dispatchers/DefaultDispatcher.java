package com.asterexcrisys.adblocker.dispatchers;

import com.asterexcrisys.adblocker.models.records.Packet;
import com.asterexcrisys.adblocker.models.types.DispatchType;
import com.asterexcrisys.adblocker.models.types.ProxyMode;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public final class DefaultDispatcher implements Dispatcher {

    private final ExecutorService executor;
    private final BlockingQueue<? extends Packet<?>> requests;
    private final List<Future<?>> handlers;
    private final Supplier<Runnable> task;
    private final int requestsLimit;
    private final int minimumTasks;
    private final int maximumTasks;

    public DefaultDispatcher(ExecutorService executor, BlockingQueue<? extends Packet<?>> requests, List<Future<?>> handlers, Supplier<Runnable> task, int requestsLimit, int minimumTasks, int maximumTasks) {
        this.executor = Objects.requireNonNull(executor);
        this.requests = Objects.requireNonNull(requests);
        this.handlers = Objects.requireNonNull(handlers);
        this.task = Objects.requireNonNull(task);
        this.requestsLimit = requestsLimit;
        this.minimumTasks = minimumTasks;
        this.maximumTasks = maximumTasks;
    }

    @Override
    public ProxyMode mode() {
        return ProxyMode.DEFAULT;
    }

    @Override
    public DispatchType dispatch() {
        if (Thread.currentThread().isInterrupted()) {
            return DispatchType.NONE;
        }
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