package com.asterexcrisys.gab.matchers;

@SuppressWarnings("unused")
public sealed interface Matcher permits ExactMatcher, WildcardMatcher {

    boolean matches(String domain);

}