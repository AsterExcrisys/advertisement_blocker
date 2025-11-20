package com.asterexcrisys.adblocker.tasks;

import com.asterexcrisys.adblocker.models.records.HTTPPacket;
import com.asterexcrisys.adblocker.models.records.ThreadContext;
import com.asterexcrisys.adblocker.services.ProxyManager;
import com.asterexcrisys.adblocker.utilities.GlobalUtility;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Message;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("unused")
public class HTTPHandler implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(HTTPHandler.class);

    private final ThreadLocal<ThreadContext> contextManager;
    private final ThreadContext context;
    private final BlockingQueue<HTTPPacket> requests;
    private final BlockingQueue<HTTPPacket> responses;

    public HTTPHandler(ThreadLocal<ThreadContext> contextManager, BlockingQueue<HTTPPacket> requests, BlockingQueue<HTTPPacket> responses) {
        this.contextManager = Objects.requireNonNull(contextManager);
        this.context = Objects.requireNonNull(this.contextManager.get());
        this.requests = Objects.requireNonNull(requests);
        this.responses = Objects.requireNonNull(responses);
    }

    @Override
    public void run() {
        try {
            ReentrantLock lock = context.lock();
            ProxyManager manager = context.manager();
            while (!Thread.currentThread().isInterrupted()) {
                HTTPPacket requestPacket = requests.take();
                HttpExchange exchange = requestPacket.transport();
                Message request = new Message(requestPacket.data());
                Message response = GlobalUtility.acquireAccess(lock, () -> manager.handle(request));
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
        } finally {
            contextManager.remove();
        }
    }

}