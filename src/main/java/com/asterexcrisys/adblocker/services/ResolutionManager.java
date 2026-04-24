package com.asterexcrisys.adblocker.services;

import com.asterexcrisys.adblocker.resolvers.Resolver;
import com.asterexcrisys.adblocker.models.types.ProxyState;
import com.asterexcrisys.adblocker.utilities.GlobalUtility;
import com.asterexcrisys.adblocker.utilities.ResolverUtility;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import java.time.Duration;
import java.util.*;

@SuppressWarnings("unused")
public class ResolutionManager implements AutoCloseable {

    private final boolean isRetryingEnabled;
    private final Map<Resolver, CircuitBreaker> resolvers;
    private ProxyState state;

    public ResolutionManager() {
        isRetryingEnabled = false;
        resolvers = new HashMap<>();
        state = ProxyState.INACTIVE;
    }

    public ResolutionManager(boolean isRetryingEnabled) {
        this.isRetryingEnabled = isRetryingEnabled;
        resolvers = new HashMap<>();
        state = ProxyState.INACTIVE;
    }

    public boolean isRetryingEnabled() {
        return isRetryingEnabled;
    }

    public void addResolver(Resolver resolver) {
        if (state == ProxyState.DISPOSED) {
            throw new IllegalStateException("cannot use proxy in disposed state");
        }
        if (resolver == null) {
            throw new IllegalArgumentException("resolver must not be null");
        }
        resolvers.put(resolver, GlobalUtility.buildCircuitBreaker(
                20,
                10,
                10,
                50,
                Duration.ofMillis(60000),
                Duration.ofMillis(20000)
        ));
        state = ProxyState.ACTIVE;
    }

    public void addResolvers(Collection<Resolver> resolvers) {
        if (state == ProxyState.DISPOSED) {
            throw new IllegalStateException("cannot use proxy in disposed state");
        }
        if (resolvers == null || resolvers.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("resolvers list must not be null or contain null elements");
        }
        for (Resolver resolver : resolvers) {
            this.resolvers.put(resolver, GlobalUtility.buildCircuitBreaker(
                    20,
                    10,
                    10,
                    50,
                    Duration.ofMillis(60000),
                    Duration.ofMillis(20000)
            ));
        }
        if (!this.resolvers.isEmpty()) {
            state = ProxyState.ACTIVE;
        }
    }

    public void removeResolver(Resolver resolver) {
        if (state == ProxyState.DISPOSED || resolver == null) {
            return;
        }
        resolvers.remove(resolver);
        if (resolvers.isEmpty()) {
            state = ProxyState.INACTIVE;
        }
    }

    public void removeResolvers(Collection<Resolver> resolvers) {
        if (state == ProxyState.DISPOSED || resolvers == null || resolvers.stream().allMatch(Objects::isNull)) {
            return;
        }
        for (Resolver resolver : resolvers) {
            this.resolvers.remove(resolver);
        }
        if (this.resolvers.isEmpty()) {
            state = ProxyState.INACTIVE;
        }
    }

    public void clearResolvers() {
        if (state == ProxyState.DISPOSED) {
            return;
        }
        resolvers.clear();
        state = ProxyState.INACTIVE;
    }

    public Message resolve(Message request) {
        Message response = ResolverUtility.buildErrorResponse(
                request,
                Rcode.SERVFAIL,
                500,
                "Failed to resolve the DNS query: no available resolvers were found"
        );
        if (state != ProxyState.ACTIVE) {
            return response;
        }
        for (Map.Entry<Resolver, CircuitBreaker> entry : resolvers.entrySet()) {
            Resolver resolver = entry.getKey();
            CircuitBreaker circuitBreaker = entry.getValue();
            if (circuitBreaker.getState() != State.CLOSED && circuitBreaker.getState() != State.HALF_OPEN) {
                continue;
            }
            response = circuitBreaker.executeSupplier(() -> resolver.resolve(request));
            if (!isRetryingEnabled) {
                break;
            }
            if (response.getHeader().getRcode() != Rcode.SERVFAIL) {
                break;
            }
        }
        return response;
    }

    @Override
    public void close() throws Exception {
        if (state == ProxyState.DISPOSED) {
            return;
        }
        for (Map.Entry<Resolver, CircuitBreaker> entry : resolvers.entrySet()) {
            entry.getKey().close();
            entry.getValue().reset();
        }
        resolvers.clear();
        state = ProxyState.DISPOSED;
    }

}