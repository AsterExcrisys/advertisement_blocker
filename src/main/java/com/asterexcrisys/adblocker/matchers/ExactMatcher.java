package com.asterexcrisys.adblocker.matchers;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public final class ExactMatcher implements Matcher {

    private final Set<String> list;

    public ExactMatcher() {
        list = ConcurrentHashMap.newKeySet();
    }

    public Set<String> list() {
        return list;
    }

    @Override
    public boolean matches(String domain) {
        return list.contains(domain);
    }

}