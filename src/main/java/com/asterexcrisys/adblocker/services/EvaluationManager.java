package com.asterexcrisys.adblocker.services;

import com.asterexcrisys.adblocker.filters.Filter;
import com.asterexcrisys.adblocker.models.types.FilterMode;
import com.asterexcrisys.adblocker.models.types.MatcherMode;
import com.asterexcrisys.adblocker.services.protocols.DNSCache;
import com.asterexcrisys.adblocker.services.protocols.DNSPolicy;
import com.asterexcrisys.adblocker.utilities.ResolverUtilities;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import java.util.Optional;

public class EvaluationManager {

    private final DNSPolicy policy;
    private final DNSCache cache;

    public EvaluationManager(Filter filter) {
        this.policy = new DNSPolicy(filter);
        this.cache = new DNSCache();
    }

    public EvaluationManager(Filter filter, int maximumSize) {
        this.policy = new DNSPolicy(filter);
        this.cache = new DNSCache(maximumSize);
    }

    public FilterMode filterMode() {
        return policy.filterMode();
    }

    public MatcherMode matcherMode() {
        return policy.matcherMode();
    }

    public int getCacheMaximumSize() {
        return cache.getMaximumSize();
    }

    public void setCacheMaximumSize(int maximumSize) {
        cache.setMaximumSize(maximumSize);
    }

    public Optional<Message> evaluate(Message request) {
        if (!policy.isValid(request)) {
            return Optional.of(ResolverUtilities.buildErrorResponse(
                    request,
                    Rcode.REFUSED,
                    300,
                    "Failed to resolve the DNS query: blocked by the proxy's policy"
            ));
        }
        return cache.get(request);
    }

    public void update(Message response) {
        cache.put(response);
    }

}