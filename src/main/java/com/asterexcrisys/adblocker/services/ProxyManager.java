package com.asterexcrisys.adblocker.services;

import com.asterexcrisys.adblocker.filters.BlacklistFilter;
import com.asterexcrisys.adblocker.filters.Filter;
import com.asterexcrisys.adblocker.matchers.Matcher;
import com.asterexcrisys.adblocker.resolvers.Resolver;
import com.asterexcrisys.adblocker.utility.GlobalUtility;
import com.asterexcrisys.adblocker.utility.ResolverUtility;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Rcode;
import java.time.Duration;
import java.util.*;

@SuppressWarnings("unused")
public class ProxyManager {

    private final boolean isRetryingEnabled;
    private final Map<Resolver, CircuitBreaker> resolvers;
    private final DNSCache cache;
    private Filter filter;

    public ProxyManager() {
        isRetryingEnabled = false;
        resolvers = new HashMap<>();
        cache = new DNSCache();
        filter = new BlacklistFilter();
    }

    public ProxyManager(boolean isRetryingEnabled) {
        this.isRetryingEnabled = isRetryingEnabled;
        resolvers = new HashMap<>();
        cache = new DNSCache();
        filter = new BlacklistFilter();
    }

    public boolean isRetryingEnabled() {
        return isRetryingEnabled;
    }

    public void addResolver(Resolver resolver) {
        if (resolver == null) {
            throw new IllegalArgumentException();
        }
        resolvers.put(resolver, GlobalUtility.buildCircuitBreaker(
                20,
                10,
                10,
                50,
                Duration.ofMillis(60000),
                Duration.ofMillis(20000)
        ));
    }

    public void addResolvers(Collection<Resolver> resolvers) {
        if (resolvers == null || resolvers.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException();
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
    }

    public void removeResolver(Resolver resolver) {
        if (resolver == null) {
            return;
        }
        resolvers.remove(resolver);
    }

    public void removeResolvers(Collection<Resolver> resolvers) {
        if (resolvers == null || resolvers.stream().allMatch(Objects::isNull)) {
            return;
        }
        for (Resolver resolver : resolvers) {
            this.resolvers.remove(resolver);
        }
    }

    public void clearResolvers() {
        resolvers.clear();
    }

    public int getCacheMaximumSize() {
        return cache.getMaximumSize();
    }

    public void setCacheMaximumSize(int size) {
        cache.setMaximumSize(size);
    }

    public void setFilter(Filter filter) {
        this.filter = Objects.requireNonNull(filter);
    }

    public void setFilterMatcher(Matcher matcher) {
        filter.setMatcher(matcher);
    }

    public void addFilteredDomain(String domain) {
        filter.load(domain);
    }

    public void addFilteredDomains(Collection<String> domains) {
        filter.load(domains);
    }

    public void removeFilteredDomain(String domain) {
        filter.unload(domain);
    }

    public void removeFilteredDomains(Collection<String> domains) {
        filter.unload(domains);
    }

    public void clearFilteredDomains() {
        filter.clear();
    }

    public Message handle(Message request) {
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
            response = ResolverUtility.buildErrorResponse(request, 15, 15, "Blocked by proxy policy");
        }
        return response;
    }

    private Message resolve(Message request) {
        Message response = ResolverUtility.buildErrorResponse(
                request,
                15,
                15,
                "No available resolvers were found"
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