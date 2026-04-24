package com.asterexcrisys.adblocker.tasks;

import com.asterexcrisys.adblocker.models.packets.HTTPPacket;
import com.asterexcrisys.adblocker.services.EvaluationManager;
import com.asterexcrisys.adblocker.services.ResolutionManager;
import com.asterexcrisys.adblocker.services.contexts.ContextPool;
import com.asterexcrisys.adblocker.utilities.GlobalUtility;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Message;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;

@SuppressWarnings("unused")
public class HTTPHandler implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(HTTPHandler.class);

    private final EvaluationManager evaluationManager;
    private final ContextPool<ResolutionManager> contextPool;
    private final BlockingQueue<HTTPPacket> requests;
    private final BlockingQueue<HTTPPacket> responses;

    public HTTPHandler(EvaluationManager evaluationManager, ContextPool<ResolutionManager> contextPool, BlockingQueue<HTTPPacket> requests, BlockingQueue<HTTPPacket> responses) {
        this.evaluationManager = Objects.requireNonNull(evaluationManager);
        this.contextPool = Objects.requireNonNull(contextPool);
        this.requests = Objects.requireNonNull(requests);
        this.responses = Objects.requireNonNull(responses);
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                HTTPPacket requestPacket = requests.take();
                HttpExchange exchange = requestPacket.transport();
                Message request = new Message(requestPacket.data());
                Message response = process(request);
                HTTPPacket responsePacket = HTTPPacket.of(
                        exchange,
                        response.toWire()
                );
                if (responses.offer(responsePacket)) {
                    LOGGER.info(
                            "Succeeded to send HTTP response to {}:{}",
                            exchange.getRemoteAddress().getAddress().getHostAddress(),
                            exchange.getRemoteAddress().getPort()
                    );
                } else {
                    LOGGER.warn(
                            "Failed to send HTTP response to {}:{}",
                            exchange.getRemoteAddress().getAddress().getHostAddress(),
                            exchange.getRemoteAddress().getPort()
                    );
                }
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to handle HTTP request: {}", exception.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private Message process(Message request) throws InterruptedException {
        Optional<Message> response = evaluationManager.evaluate(request);
        if (response.isPresent()) {
            return response.get();
        }
        return GlobalUtility.acquireAccess(contextPool, (context) -> {
            return context.resolve(request);
        });
    }

}