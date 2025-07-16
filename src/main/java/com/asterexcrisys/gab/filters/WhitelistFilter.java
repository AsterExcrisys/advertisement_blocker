package com.asterexcrisys.aab.filters;

import com.asterexcrisys.aab.matchers.ExactMatcher;
import com.asterexcrisys.aab.matchers.Matcher;
import com.asterexcrisys.aab.matchers.WildcardMatcher;
import java.util.List;
import java.util.Objects;

public final class WhitelistFilter implements Filter {

    private Matcher matcher;

    public WhitelistFilter() {
        matcher = new ExactMatcher();
    }

    public WhitelistFilter(Matcher matcher) {
        this.matcher = Objects.requireNonNull(matcher);
    }

    @Override
    public void setMatcher(Matcher matcher) {
        this.matcher = Objects.requireNonNull(matcher);
    }

    @Override
    public void load(String domain) {
        if (domain == null) {
            throw new IllegalArgumentException();
        }
        switch (matcher) {
            case ExactMatcher exact -> exact.list().add(domain);
            case WildcardMatcher wildcard -> wildcard.list().add(domain, "\\.");
        }
    }

    @Override
    public void load(List<String> domains) {
        if (domains == null || domains.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException();
        }
        switch (matcher) {
            case ExactMatcher exact -> exact.list().addAll(domains);
            case WildcardMatcher wildcard -> {
                for (String domain : domains) {
                    wildcard.list().add(domain, "\\.");
                }
            }
        }
    }

    @Override
    public void unload(String domain) {
        if (domain == null) {
            return;
        }
        switch (matcher) {
            case ExactMatcher exact -> exact.list().remove(domain);
            case WildcardMatcher wildcard -> wildcard.list().remove(domain, "\\.");
        }
    }

    @Override
    public void unload() {
        switch (matcher) {
            case ExactMatcher exact -> exact.list().clear();
            case WildcardMatcher wildcard -> wildcard.list().clear();
        }
    }

    @Override
    public boolean isAllowed(String domain) {
        return matcher.matches(domain);
    }

}