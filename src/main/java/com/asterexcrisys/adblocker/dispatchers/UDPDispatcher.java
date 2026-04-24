package com.asterexcrisys.adblocker.dispatchers;

import com.asterexcrisys.adblocker.models.types.ProxyMode;
import com.asterexcrisys.adblocker.services.EvaluationManager;
import com.asterexcrisys.adblocker.services.ResolutionManager;
import com.asterexcrisys.adblocker.services.contexts.ContextPool;
import com.asterexcrisys.adblocker.tasks.UDPHandler;
import com.asterexcrisys.adblocker.models.types.DispatchType;
import com.asterexcrisys.adblocker.models.packets.UDPPacket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public final class UDPDispatcher implements Dispatcher {

    private final DefaultDispatcher dispatcher;

    public UDPDispatcher(ExecutorService executor, EvaluationManager evaluationManager, ContextPool<ResolutionManager> contextPool, BlockingQueue<UDPPacket> udpRequests, BlockingQueue<UDPPacket> udpResponses, List<Future<?>> udpHandlers, int requestsLimit, int minimumTasks, int maximumTasks) {
        Supplier<Runnable> task = () -> new UDPHandler(evaluationManager, contextPool, udpRequests, udpResponses);
        dispatcher = new DefaultDispatcher(executor, udpRequests, udpHandlers, task, requestsLimit, minimumTasks, maximumTasks);
    }

    @Override
    public ProxyMode mode() {
        return ProxyMode.UDP;
    }

    @Override
    public DispatchType dispatch() {
        return dispatcher.dispatch();
    }

}