package com.asterexcrisys.adblocker.matchers;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("unused")
public final class ExactMatcher implements Matcher {

    private final Set<String> list;

    public ExactMatcher(Collection<String> domains) {
        list = Set.copyOf(Objects.requireNonNull(domains));
    }

    @Override
    public boolean matches(String domain) {
        if (domain == null) {
            throw new IllegalArgumentException("domain must not be null");
        }
        return list.contains(domain.toLowerCase());
    }

}