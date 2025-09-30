package com.asterexcrisys.adblocker.matchers;

import java.util.Collection;

@SuppressWarnings("unused")
public sealed interface Matcher permits ExactMatcher, WildcardMatcher {

    void add(String domain);

    void addAll(Collection<String> domains);

    void remove(String domain);

    void removeAll(Collection<String> domains);

    void clear();

    boolean matches(String domain);

}