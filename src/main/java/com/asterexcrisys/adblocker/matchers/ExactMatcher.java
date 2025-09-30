package com.asterexcrisys.adblocker.matchers;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public final class ExactMatcher implements Matcher {

    private final Set<String> list;

    public ExactMatcher() {
        list = ConcurrentHashMap.newKeySet();
    }

    @Override
    public void add(String domain) {
        if (domain == null) {
            throw new IllegalArgumentException("domain must not be null");
        }
        list.add(domain.toLowerCase());
    }

    @Override
    public void addAll(Collection<String> domains) {
        if (domains == null || domains.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("domains list must not be null or contain null elements");
        }
        domains.stream().map(String::toLowerCase).forEach(list::add);
    }

    @Override
    public void remove(String domain) {
        if (domain == null) {
            return;
        }
        list.remove(domain.toLowerCase());
    }

    @Override
    public void removeAll(Collection<String> domains) {
        if (domains == null || domains.stream().allMatch(Objects::isNull)) {
            return;
        }
        domains.stream().filter(Objects::nonNull).map(String::toLowerCase).forEach(list::remove);
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
        return list.contains(domain.toLowerCase());
    }

}