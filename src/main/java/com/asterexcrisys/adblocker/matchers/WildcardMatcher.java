package com.asterexcrisys.adblocker.matchers;

import com.asterexcrisys.adblocker.services.DomainTrie;

@SuppressWarnings("unused")
public final class WildcardMatcher implements Matcher {

    private final DomainTrie list;

    public WildcardMatcher() {
        list = new DomainTrie("www", "*");
    }

    public DomainTrie list() {
        return list;
    }

    @Override
    public boolean matches(String domain) {
        return list.has(domain, "\\.");
    }

}