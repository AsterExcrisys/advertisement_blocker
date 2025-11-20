package com.asterexcrisys.adblocker.tasks;

import com.asterexcrisys.adblocker.models.records.HTTPPacket;
import com.asterexcrisys.adblocker.models.types.HTTPMethod;
import com.asterexcrisys.adblocker.utilities.GlobalUtility;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;

@SuppressWarnings("unused")
public class HTTPReader extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(HTTPReader.class);

    private final HttpServer server;
    private final BlockingQueue<HTTPPacket> requests;

    public HTTPReader(HttpServer server, BlockingQueue<HTTPPacket> requests) {
        this.server = Objects.requireNonNull(server);
        this.requests = Objects.requireNonNull(requests);
    }

    @Override
    public void run() {
        try {
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            server.createContext("/dns-query/", this::handleRequest);
            server.start();
        } catch (Exception exception) {
            LOGGER.error("Failed to accept HTTP connection: {}", exception.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        try {
            Optional<byte[]> request = parseRequest(exchange);
            if (request.isEmpty()) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }
            HTTPPacket requestPacket = HTTPPacket.of(exchange, request.get());
            if (requests.offer(requestPacket)) {
                LOGGER.info(
                        "Succeeded to receive HTTP request from {}:{}",
                        exchange.getRemoteAddress().getAddress().getHostAddress(),
                        exchange.getRemoteAddress().getPort()
                );
            } else {
                LOGGER.warn(
                        "Failed to receive HTTP request from {}:{}",
                        exchange.getRemoteAddress().getAddress().getHostAddress(),
                        exchange.getRemoteAddress().getPort()
                );
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to read HTTP request: {}", exception.getMessage());
            Thread.currentThread().interrupt();
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        }
    }

    private Optional<byte[]> parseRequest(HttpExchange exchange) throws IOException {
        HTTPMethod method = GlobalUtility.tryOrDefault(() -> {
            return HTTPMethod.valueOf(exchange.getRequestMethod().toUpperCase());
        }, null);
        if (method == null) {
            return Optional.empty();
        }
        return switch (method) {
            case GET -> {
                Optional<Map<String, String>> parameters = parseQuery(exchange.getRequestURI().getQuery());
                if (parameters.isEmpty() || !parameters.get().containsKey("dns")) {
                    yield Optional.empty();
                }
                byte[] request = Base64.getUrlDecoder().decode(parameters.get().get("dns"));
                yield Optional.of(request);
            }
            case POST -> {
                String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                if (contentType == null || !contentType.equalsIgnoreCase("application/dns-message")) {
                    yield Optional.empty();
                }
                int contentLength = GlobalUtility.tryOrDefault(() -> {
                    return Integer.parseInt(exchange.getRequestHeaders().getFirst("Content-Length"));
                }, 0);
                Optional<byte[]> request = parseBody(exchange.getRequestBody(), 65536);
                yield request.filter((value) -> value.length == contentLength);
            }
        };
    }

    private Optional<Map<String, String>> parseQuery(String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }
        Map<String, String> result = new HashMap<>();
        String[] parameters = query.trim().split("&");
        for (String parameter : parameters) {
            if (parameter.isBlank()) {
                return Optional.empty();
            }
            String[] pair = parameter.trim().split("=");
            if (pair.length != 2 || pair[0].isBlank() || pair[1].isBlank()) {
                return Optional.empty();
            }
            result.put(pair[0], pair[1]);
        }
        return Optional.of(result);
    }

    private Optional<byte[]> parseBody(InputStream body, int limit) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[512];
        int total = 0, read;
        while ((read = body.read(chunk)) != -1) {
            total += read;
            if (total > limit) {
                return Optional.empty();
            }
            buffer.write(chunk, 0, read);
        }
        return Optional.of(buffer.toByteArray());
    }

}