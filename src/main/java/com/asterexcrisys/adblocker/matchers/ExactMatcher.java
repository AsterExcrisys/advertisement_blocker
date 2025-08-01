package com.asterexcrisys.adblocker.matchers;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public final class ExactMatcher implements Matcher {

    private final Set<String> list;

    public ExactMatcher() {
        list = new HashSet<>();
    }

    public Set<String> list() {
        return list;
    }

    @Override
    public boolean matches(String domain) {
        return list.contains(domain);
    }

}