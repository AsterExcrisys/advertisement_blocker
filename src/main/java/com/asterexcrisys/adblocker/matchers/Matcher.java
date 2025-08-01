package com.asterexcrisys.adblocker.matchers;

@SuppressWarnings("unused")
public sealed interface Matcher permits ExactMatcher, WildcardMatcher {

    boolean matches(String domain);

}