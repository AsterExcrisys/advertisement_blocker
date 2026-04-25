package com.asterexcrisys.adblocker.services.protocols;

import com.asterexcrisys.adblocker.filters.BlacklistFilter;
import com.asterexcrisys.adblocker.filters.Filter;
import com.asterexcrisys.adblocker.filters.WhitelistFilter;
import com.asterexcrisys.adblocker.models.types.FilterMode;
import com.asterexcrisys.adblocker.models.types.MatcherMode;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import java.util.Objects;

public class DNSPolicy {

    private final Filter filter;

    public DNSPolicy(Filter filter) {
        this.filter = Objects.requireNonNull(filter);
    }

    public FilterMode filterMode() {
        return switch (filter) {
            case WhitelistFilter ignored -> FilterMode.WHITELIST;
            case BlacklistFilter ignored -> FilterMode.BLACKLIST;
        };
    }

    public MatcherMode matcherMode() {
        return filter.matcherMode();
    }

    public boolean isValid(Message request) {
        if (request == null || request.getQuestion() == null) {
            return false;
        }
        Name name = request.getQuestion().getName();
        return filter.isAllowed(name.toString(true));
    }

}