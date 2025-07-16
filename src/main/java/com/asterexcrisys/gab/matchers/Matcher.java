package com.asterexcrisys.gab.matchers;

public sealed interface Matcher permits ExactMatcher, WildcardMatcher {

    boolean matches(String domain);

}