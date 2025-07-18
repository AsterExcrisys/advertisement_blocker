package com.asterexcrisys.gab.matchers;

import com.asterexcrisys.gab.utility.Trie;

@SuppressWarnings("unused")
public final class WildcardMatcher implements Matcher {

    private final Trie list;

    public WildcardMatcher() {
        list = new Trie("www");
    }

    public Trie list() {
        return list;
    }

    @Override
    public boolean matches(String domain) {
        return list.has(domain, "\\.");
    }

}