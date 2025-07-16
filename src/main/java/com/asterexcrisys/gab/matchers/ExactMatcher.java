package com.asterexcrisys.aab.matchers;

import java.util.HashSet;
import java.util.Set;

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