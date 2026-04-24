package com.asterexcrisys.adblocker.filters;

import com.asterexcrisys.adblocker.models.types.MatcherMode;

@SuppressWarnings("unused")
public sealed interface Filter permits WhitelistFilter, BlacklistFilter {

    MatcherMode matcherMode();

    boolean isAllowed(String domain);

}