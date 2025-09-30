package com.asterexcrisys.adblocker.matchers;

import com.asterexcrisys.adblocker.services.DomainTrie;
import java.util.Collection;
import java.util.Objects;

@SuppressWarnings("unused")
public final class WildcardMatcher implements Matcher {

    private final DomainTrie list;

    public WildcardMatcher() {
        list = new DomainTrie("www", "*");
    }

    @Override
    public void add(String domain) {
        if (domain == null) {
            throw new IllegalArgumentException("domain must not be null");
        }
        list.add(domain.toLowerCase(), "\\.");
    }

    @Override
    public void addAll(Collection<String> domains) {
        if (domains == null || domains.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("domains list must not be null or contain null elements");
        }
        for (String domain : domains) {
            list.add(domain.toLowerCase(), "\\.");
        }
    }

    @Override
    public void remove(String domain) {
        if (domain == null) {
            return;
        }
        list.remove(domain.toLowerCase(), "\\.");
    }

    @Override
    public void removeAll(Collection<String> domains) {
        if (domains == null || domains.stream().allMatch(Objects::isNull)) {
            return;
        }
        for (String domain : domains) {
            if (domain == null) {
                continue;
            }
            list.remove(domain.toLowerCase(), "\\.");
        }
    }

    @Override
    public void clear() {
        list.clear();
    }

    @Override
    public boolean matches(String domain) {
        if (domain == null) {
            throw new IllegalArgumentException("domain must not be null");
        }
        return list.has(domain.toLowerCase(), "\\.");
    }

}