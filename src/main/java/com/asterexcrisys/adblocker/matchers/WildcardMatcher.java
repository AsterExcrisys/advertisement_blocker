package com.asterexcrisys.adblocker.matchers;

import com.asterexcrisys.adblocker.services.domains.DomainTrie;
import java.util.Collection;

@SuppressWarnings("unused")
public final class WildcardMatcher implements Matcher {

    private final DomainTrie list;

    public WildcardMatcher(Collection<String> domains) {
        list = new DomainTrie("www", "*", "\\.", domains);
    }

    @Override
    public boolean matches(String domain) {
        if (domain == null) {
            throw new IllegalArgumentException("domain must not be null");
        }
        return list.contains(domain.toLowerCase());
    }

}