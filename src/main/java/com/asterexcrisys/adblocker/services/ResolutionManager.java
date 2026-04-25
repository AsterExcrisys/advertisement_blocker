package com.asterexcrisys.adblocker.services;

import com.asterexcrisys.adblocker.GlobalSettings;
import com.asterexcrisys.adblocker.models.types.ResolverType;
import com.asterexcrisys.adblocker.resolvers.Resolver;
import com.asterexcrisys.adblocker.models.types.ProxyState;
import com.asterexcrisys.adblocker.utilities.GlobalUtilities;
import com.asterexcrisys.adblocker.utilities.ResolverUtilities;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class ResolutionManager implements AutoCloseable {

    private final GlobalSettings SETTINGS = GlobalSettings.getInstance();

    private final boolean isRetryingEnabled;
    private final Semaphore controller;
    private final Map<Resolver, CircuitBreaker> resolvers;
    private ProxyState state;

    public ResolutionManager(Collection<Resolver> resolvers) {
        isRetryingEnabled = false;
        controller = new Semaphore(10);
        this.resolvers = initialize(Objects.requireNonNull(resolvers));
        state = ProxyState.ACTIVE;
    }

    public ResolutionManager(boolean isRetryingEnabled, int maximumConcurrency, Collection<Resolver> resolvers) {
        this.isRetryingEnabled = isRetryingEnabled;
        controller = new Semaphore(maximumConcurrency);
        this.resolvers = initialize(Objects.requireNonNull(resolvers));
        state = ProxyState.ACTIVE;
    }

    public boolean isRetryingEnabled() {
        return isRetryingEnabled;
    }

    public int resolverCount() {
        return resolvers.size();
    }

    public List<ResolverType> resolverTypes() {
        List<ResolverType> types = new ArrayList<>();
        for (Resolver resolver : resolvers.keySet()) {
            types.add(resolver.type());
        }
        return List.copyOf(types);
    }

    public Message resolve(Message request) throws InterruptedException {
        Message response = ResolverUtilities.buildErrorResponse(
                request,
                Rcode.SERVFAIL,
                500,
                "Failed to resolve the DNS query: no available resolvers were found"
        );
        if (state != ProxyState.ACTIVE) {
            return response;
        }
        if (!controller.tryAcquire(SETTINGS.getRequestTimeout(), TimeUnit.MILLISECONDS)) {
            return response;
        }
        try {
            for (Map.Entry<Resolver, CircuitBreaker> entry : resolvers.entrySet()) {
                Resolver resolver = entry.getKey();
                CircuitBreaker breaker = entry.getValue();
                if (breaker.getState() != State.CLOSED && breaker.getState() != State.HALF_OPEN) {
                    continue;
                }
                response = breaker.executeSupplier(() -> resolver.resolve(request));
                if (!isRetryingEnabled) {
                    break;
                }
                if (response.getHeader().getRcode() != Rcode.SERVFAIL) {
                    break;
                }
            }
            return response;
        } finally {
            controller.release();
        }
    }

    @Override
    public void close() throws Exception {
        if (state == ProxyState.INACTIVE) {
            return;
        }
        for (Map.Entry<Resolver, CircuitBreaker> entry : resolvers.entrySet()) {
            entry.getKey().close();
            entry.getValue().reset();
        }
        state = ProxyState.INACTIVE;
    }

    private Map<Resolver, CircuitBreaker> initialize(Collection<Resolver> resolvers) {
        Map<Resolver, CircuitBreaker> result = new HashMap<>(resolvers.size());
        for (Resolver resolver : resolvers) {
            result.put(resolver, GlobalUtilities.buildCircuitBreaker(
                    20,
                    10,
                    10,
                    50,
                    Duration.ofMillis(60000),
                    Duration.ofMillis(20000)
            ));
        }
        return Map.copyOf(result);
    }

}