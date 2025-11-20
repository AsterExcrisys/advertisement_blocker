package com.asterexcrisys.adblocker.services;

import com.asterexcrisys.adblocker.filters.BlacklistFilter;
import com.asterexcrisys.adblocker.filters.Filter;
import com.asterexcrisys.adblocker.matchers.Matcher;
import com.asterexcrisys.adblocker.resolvers.Resolver;
import com.asterexcrisys.adblocker.models.types.ProxyState;
import com.asterexcrisys.adblocker.utilities.GlobalUtility;
import com.asterexcrisys.adblocker.utilities.ResolverUtility;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Rcode;
import java.time.Duration;
import java.util.*;

@SuppressWarnings("unused")
public class ProxyManager implements AutoCloseable {

    private final boolean isRetryingEnabled;
    private final Map<Resolver, CircuitBreaker> resolvers;
    private final DNSCache cache;
    private Filter filter;
    private ProxyState state;

    public ProxyManager() {
        isRetryingEnabled = false;
        resolvers = new HashMap<>();
        cache = new DNSCache();
        filter = new BlacklistFilter();
        state = ProxyState.INACTIVE;
    }

    public ProxyManager(boolean isRetryingEnabled) {
        this.isRetryingEnabled = isRetryingEnabled;
        resolvers = new HashMap<>();
        cache = new DNSCache();
        filter = new BlacklistFilter();
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

    public int getCacheMaximumSize() {
        return cache.getMaximumSize();
    }

    public void setCacheMaximumSize(int size) {
        if (state == ProxyState.DISPOSED) {
            throw new IllegalStateException("cannot use proxy in disposed state");
        }
        cache.setMaximumSize(size);
    }

    public void setFilter(Filter filter) {
        if (state == ProxyState.DISPOSED) {
            throw new IllegalStateException("cannot use proxy in disposed state");
        }
        this.filter = Objects.requireNonNull(filter);
    }

    public void setFilterMatcher(Matcher matcher) {
        if (state == ProxyState.DISPOSED) {
            throw new IllegalStateException("cannot use proxy in disposed state");
        }
        filter.setMatcher(matcher);
    }

    public void addFilteredDomain(String domain) {
        if (state == ProxyState.DISPOSED) {
            throw new IllegalStateException("cannot use proxy in disposed state");
        }
        filter.load(domain);
    }

    public void addFilteredDomains(Collection<String> domains) {
        if (state == ProxyState.DISPOSED) {
            throw new IllegalStateException("cannot use proxy in disposed state");
        }
        filter.load(domains);
    }

    public void removeFilteredDomain(String domain) {
        if (state == ProxyState.DISPOSED) {
            return;
        }
        filter.unload(domain);
    }

    public void removeFilteredDomains(Collection<String> domains) {
        if (state == ProxyState.DISPOSED) {
            return;
        }
        filter.unload(domains);
    }

    public void clearFilteredDomains() {
        if (state == ProxyState.DISPOSED) {
            return;
        }
        filter.clear();
    }

    public Message handle(Message request) {
        if (state != ProxyState.ACTIVE) {
            return ResolverUtility.buildErrorResponse(
                    request,
                    Rcode.SERVFAIL,
                    500,
                    "Failed to resolve the DNS query: no available resolvers were found"
            );
        }
        Name name = request.getQuestion().getName();
        Message response;
        if (filter.isAllowed(name.toString(true).toLowerCase())) {
            Optional<Message> result = cache.get(request);
            if (result.isPresent()) {
                response = result.get();
            } else {
                response = resolve(request);
                cache.put(response);
            }
        } else {
            response = ResolverUtility.buildErrorResponse(
                    request,
                    Rcode.REFUSED,
                    300,
                    "Failed to resolve the DNS query: blocked by the proxy's policy"
            );
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
        cache.clear();
        filter.clear();
        state = ProxyState.DISPOSED;
    }

    private Message resolve(Message request) {
        Message response = ResolverUtility.buildErrorResponse(
                request,
                Rcode.SERVFAIL,
                500,
                "Failed to resolve the DNS query: no available resolvers were found"
        );
        for (Map.Entry<Resolver, CircuitBreaker> entry : resolvers.entrySet()) {
            if (entry.getValue().getState() != State.CLOSED && entry.getValue().getState() != State.HALF_OPEN) {
                continue;
            }
            response = entry.getValue().executeSupplier(() -> entry.getKey().resolve(request));
            if (!isRetryingEnabled) {
                break;
            }
            if (response.getHeader().getRcode() != Rcode.SERVFAIL) {
                break;
            }
        }
        return response;
    }

}