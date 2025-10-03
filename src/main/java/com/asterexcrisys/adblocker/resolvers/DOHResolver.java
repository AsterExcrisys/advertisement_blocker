package com.asterexcrisys.adblocker.resolvers;

import com.asterexcrisys.adblocker.models.types.HTTPMethod;
import com.asterexcrisys.adblocker.utility.ResolverUtility;
import okhttp3.*;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;

@SuppressWarnings("unused")
public final class DOHResolver implements Resolver {

    private static final MediaType MEDIA_TYPE = MediaType.get("application/dns-message");

    private final String nameServer;
    private final String queryEndpoint;
    private final HTTPMethod httpMethod;
    private final OkHttpClient client;

    public DOHResolver(String nameServer) {
        this.nameServer = Objects.requireNonNull(nameServer);
        queryEndpoint = "dns-query";
        httpMethod = HTTPMethod.POST;
        client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(3))
                .callTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(5))
                .retryOnConnectionFailure(true)
                .build();
    }

    public DOHResolver(String nameServer, String queryEndpoint) {
        this.nameServer = Objects.requireNonNull(nameServer);
        this.queryEndpoint = Objects.requireNonNull(queryEndpoint);
        httpMethod = HTTPMethod.POST;
        client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(3))
                .callTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(5))
                .retryOnConnectionFailure(true)
                .build();
    }

    public DOHResolver(String nameServer, HTTPMethod httpMethod) {
        this.nameServer = Objects.requireNonNull(nameServer);
        queryEndpoint = "dns-query";
        this.httpMethod = Objects.requireNonNull(httpMethod);
        client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(3))
                .callTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(5))
                .retryOnConnectionFailure(true)
                .build();
    }

    public DOHResolver(String nameServer, String queryEndpoint, HTTPMethod httpMethod) {
        this.nameServer = Objects.requireNonNull(nameServer);
        this.queryEndpoint = Objects.requireNonNull(queryEndpoint);
        this.httpMethod = Objects.requireNonNull(httpMethod);
        client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(3))
                .callTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(5))
                .retryOnConnectionFailure(true)
                .build();
    }

    public String nameServer() {
        return nameServer;
    }

    public String queryEndpoint() {
        return queryEndpoint;
    }

    public HTTPMethod httpMethod() {
        return httpMethod;
    }

    @Override
    public Message resolve(Message request) {
        if (!ResolverUtility.validateRequest(request)) {
            return ResolverUtility.buildErrorResponse(
                    request,
                    Rcode.FORMERR,
                    400,
                    "Failed to resolve the DNS query: request must have a header and question field to be considered valid"
            );
        }
        try {
            ResolverUtility.updatePayloadSize(request);
            Request httpRequest;
            if (httpMethod == HTTPMethod.GET) {
                httpRequest = buildGetRequest(request);
            } else {
                httpRequest = buildPostRequest(request);
            }
            try (Response httpResponse = client.newCall(httpRequest).execute()) {
                if (!httpResponse.isSuccessful()) {
                    throw new IllegalArgumentException("invalid response received from '%s'".formatted(nameServer));
                }
                return new Message(httpResponse.body().bytes());
            }
        } catch (Exception exception) {
            return ResolverUtility.buildErrorResponse(
                    request,
                    Rcode.SERVFAIL,
                    500,
                    "Failed to resolve the DNS query: %s".formatted(exception.getMessage())
            );
        }
    }

    @Override
    public void close() throws IOException {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
        if (client.cache() != null) {
            client.cache().close();
        }
    }

    private Request buildGetRequest(Message request) {
        String dnsRequest = Base64.getEncoder().encodeToString(request.toWire());
        return new Request.Builder()
                .url("https://%s/%s?dns=%s".formatted(nameServer, queryEndpoint, dnsRequest))
                .addHeader("Content-Type", MEDIA_TYPE.type())
                .get().build();
    }

    private Request buildPostRequest(Message request) {
        RequestBody dnsRequest = RequestBody.create(request.toWire(), MEDIA_TYPE);
        return new Request.Builder()
                .url("https://%s/%s".formatted(nameServer, queryEndpoint))
                .addHeader("Content-Type", MEDIA_TYPE.type())
                .addHeader("Accept", MEDIA_TYPE.type())
                .post(dnsRequest).build();
    }

}