package com.asterexcrisys.aab;

import com.asterexcrisys.aab.filters.BlacklistFilter;
import com.asterexcrisys.aab.filters.Filter;
import com.asterexcrisys.aab.matchers.Matcher;
import com.asterexcrisys.aab.resolvers.Resolver;
import com.asterexcrisys.aab.utility.DNSCache;
import com.asterexcrisys.aab.utility.Utility;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Rcode;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class ProxyManager {

    private final Set<Resolver> resolvers;
    private final DNSCache cache;
    private Filter filter;

    public ProxyManager() {
        resolvers = new HashSet<>();
        cache = new DNSCache();
        filter = new BlacklistFilter();
    }

    public void addResolver(Resolver resolver) {
        if (resolver == null) {
            throw new IllegalArgumentException();
        }
        resolvers.add(resolver);
    }

    public void removeResolver(Resolver resolver) {
        if (resolver == null) {
            return;
        }
        resolvers.remove(resolver);
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

    public void addFilterDomain(String domain) {
        filter.load(domain);
    }

    public void removeFilterDomain(String domain) {
        filter.unload(domain);
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
            response = Utility.buildErrorResponse(request, 15, 15, "Blocked by proxy policy");
        }
        return response;
    }

    private Message resolve(Message request) {
        Message response = Utility.buildErrorResponse(
                request,
                15,
                15,
                "No resolvers were found"
        );
        for (Resolver resolver : resolvers) {
            response = resolver.resolve(request);
            if (response.getHeader().getRcode() == Rcode.NOERROR) {
                break;
            }
        }
        return response;
    }

}