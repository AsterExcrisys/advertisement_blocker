package com.asterexcrisys.adblocker.dispatchers;

import com.asterexcrisys.adblocker.models.packets.HTTPPacket;
import com.asterexcrisys.adblocker.models.types.DispatchType;
import com.asterexcrisys.adblocker.models.types.ProxyMode;
import com.asterexcrisys.adblocker.services.EvaluationManager;
import com.asterexcrisys.adblocker.services.ResolutionManager;
import com.asterexcrisys.adblocker.tasks.HTTPHandler;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public final class HTTPDispatcher implements Dispatcher {

    private final DefaultDispatcher dispatcher;
    private final boolean isSecure;

    public HTTPDispatcher(ExecutorService executor, EvaluationManager evaluationManager, ResolutionManager resolutionManager, BlockingQueue<HTTPPacket> httpRequests, BlockingQueue<HTTPPacket> httpResponses, List<Future<?>> httpHandlers, int requestsLimit, int minimumTasks, int maximumTasks, boolean isSecure) {
        Supplier<Runnable> task = () -> new HTTPHandler(evaluationManager, resolutionManager, httpRequests, httpResponses);
        dispatcher = new DefaultDispatcher(executor, httpRequests, httpHandlers, task, requestsLimit, minimumTasks, maximumTasks);
        this.isSecure = isSecure;
    }

    @Override
    public ProxyMode mode() {
        return isSecure? ProxyMode.HTTPS:ProxyMode.HTTP;
    }

    @Override
    public DispatchType dispatch() {
        return dispatcher.dispatch();
    }

}