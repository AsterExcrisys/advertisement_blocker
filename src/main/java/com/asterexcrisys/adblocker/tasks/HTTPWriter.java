package com.asterexcrisys.adblocker.tasks;

import com.asterexcrisys.adblocker.models.records.HTTPPacket;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

@SuppressWarnings("unused")
public class HTTPWriter extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(HTTPWriter.class);

    private final BlockingQueue<HTTPPacket> responses;

    public HTTPWriter(BlockingQueue<HTTPPacket> responses) {
        this.responses = Objects.requireNonNull(responses);
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                HTTPPacket responsePacket = responses.take();
                HttpExchange exchange = responsePacket.transport();
                if (exchange.getResponseCode() != -1) {
                    continue;
                }
                try (exchange) {
                    byte[] response = responsePacket.data();
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.getResponseBody().flush();
                }
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to send HTTP response: {}", exception.getMessage());
            Thread.currentThread().interrupt();
        }
    }

}