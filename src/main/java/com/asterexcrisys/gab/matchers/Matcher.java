package com.asterexcrisys.aab.matchers;

public sealed interface Matcher permits ExactMatcher, WildcardMatcher {

    boolean matches(String domain);

}