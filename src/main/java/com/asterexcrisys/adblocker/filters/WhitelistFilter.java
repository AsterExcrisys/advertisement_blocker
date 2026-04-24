package com.asterexcrisys.adblocker.filters;

import com.asterexcrisys.adblocker.matchers.ExactMatcher;
import com.asterexcrisys.adblocker.matchers.Matcher;
import com.asterexcrisys.adblocker.matchers.WildcardMatcher;
import com.asterexcrisys.adblocker.models.types.MatcherMode;
import java.util.Objects;

@SuppressWarnings("unused")
public final class WhitelistFilter implements Filter {

    private final Matcher matcher;

    public WhitelistFilter(Matcher matcher) {
        this.matcher = Objects.requireNonNull(matcher);
    }

    @Override
    public MatcherMode matcherMode() {
        return switch (matcher) {
            case ExactMatcher ignored -> MatcherMode.EXACT;
            case WildcardMatcher ignored -> MatcherMode.WILDCARD;
        };
    }

    @Override
    public boolean isAllowed(String domain) {
        return matcher.matches(domain);
    }

}