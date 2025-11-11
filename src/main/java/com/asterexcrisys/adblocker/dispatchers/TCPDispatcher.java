package com.asterexcrisys.adblocker.dispatchers;

import com.asterexcrisys.adblocker.models.records.TCPPacket;
import com.asterexcrisys.adblocker.models.records.ThreadContext;
import com.asterexcrisys.adblocker.models.types.DispatchType;
import com.asterexcrisys.adblocker.models.types.ProxyMode;
import com.asterexcrisys.adblocker.tasks.TCPHandler;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public final class TCPDispatcher implements Dispatcher {

    private final DefaultDispatcher dispatcher;
    private final boolean isSecure;

    public TCPDispatcher(ExecutorService executor, BlockingQueue<TCPPacket> tcpRequests, BlockingQueue<TCPPacket> tcpResponses, List<Future<?>> tcpHandlers, ThreadLocal<ThreadContext> contextManager, int requestsLimit, int minimumTasks, int maximumTasks, boolean isSecure) {
        Supplier<Runnable> task = () -> new TCPHandler(contextManager, tcpRequests, tcpResponses);
        dispatcher = new DefaultDispatcher(executor, tcpRequests, tcpHandlers, task, requestsLimit, minimumTasks, maximumTasks);
        this.isSecure = isSecure;
    }

    @Override
    public ProxyMode mode() {
        return isSecure? ProxyMode.TLS:ProxyMode.TCP;
    }

    @Override
    public DispatchType dispatch() {
        return dispatcher.dispatch();
    }

}