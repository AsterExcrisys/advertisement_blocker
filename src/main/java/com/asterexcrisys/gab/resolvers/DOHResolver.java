package com.asterexcrisys.gab.resolvers;

import com.asterexcrisys.gab.utility.Utility;
import okhttp3.*;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

@SuppressWarnings("unused")
public final class DOHResolver implements Resolver, AutoCloseable {

    private static final MediaType MEDIA_TYPE = MediaType.get("application/dns-message");

    private final Method httpMethod;
    private final String nameServer;
    private final String queryEndpoint;
    private final OkHttpClient client;

    public DOHResolver(Method httpMethod, String nameServer) {
        this.httpMethod = Objects.requireNonNull(httpMethod);
        this.nameServer = Objects.requireNonNull(nameServer);
        queryEndpoint = "dns-query";
        client = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(5))
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(5))
                .retryOnConnectionFailure(true)
                .build();
    }

    public DOHResolver(Method httpMethod, String nameServer, String queryEndpoint) {
        this.httpMethod = Objects.requireNonNull(httpMethod);
        this.nameServer = Objects.requireNonNull(nameServer);
        this.queryEndpoint = Objects.requireNonNull(queryEndpoint);
        client = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(5))
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(5))
                .retryOnConnectionFailure(true)
                .build();
    }

    public Method httpMethod() {
        return httpMethod;
    }

    public String nameServer() {
        return nameServer;
    }

    public String queryEndpoint() {
        return queryEndpoint;
    }

    @Override
    public Message resolve(Message request) {
        try {
            RequestBody body = RequestBody.create(request.toWire(), MEDIA_TYPE);
            Request httpRequest = new Request.Builder()
                    .url("https://%s/%s".formatted(nameServer, queryEndpoint))
                    .addHeader("Accept", MEDIA_TYPE.type())
                    .method(httpMethod.name(), body)
                    .build();
            try (Response httpResponse = client.newCall(httpRequest).execute()) {
                if (!httpResponse.isSuccessful() || httpResponse.body() == null) {
                    throw new IOException("Unexpected response from '%s'".formatted(nameServer));
                }
                return new Message(httpResponse.body().bytes());
            }
        } catch (Exception e) {
            return Utility.buildErrorResponse(
                    request,
                    Rcode.SERVFAIL,
                    2,
                    "Failed to resolve the DNS query: %s".formatted(e.getMessage())
            );
        }
    }

    @Override
    public void close() throws IOException {
        client.connectionPool().evictAll();
        client.dispatcher().executorService().shutdown();
        if (client.cache() != null) {
            client.cache().close();
        }
    }

    public enum Method {
        GET,
        POST
    }

}