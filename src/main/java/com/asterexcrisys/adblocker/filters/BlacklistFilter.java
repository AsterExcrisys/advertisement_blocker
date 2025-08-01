package com.asterexcrisys.adblocker.filters;

import com.asterexcrisys.adblocker.matchers.ExactMatcher;
import com.asterexcrisys.adblocker.matchers.Matcher;
import com.asterexcrisys.adblocker.matchers.WildcardMatcher;
import java.util.Collection;
import java.util.Objects;

@SuppressWarnings("unused")
public final class BlacklistFilter implements Filter {

    private Matcher matcher;

    public BlacklistFilter() {
        matcher = new ExactMatcher();
    }

    public BlacklistFilter(Matcher matcher) {
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
    public void load(Collection<String> domains) {
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
    public void unload(Collection<String> domains) {
        if (domains == null || domains.stream().allMatch(Objects::isNull)) {
            return;
        }
        switch (matcher) {
            case ExactMatcher exact -> exact.list().removeAll(domains);
            case WildcardMatcher wildcard -> {
                for (String domain : domains) {
                    wildcard.list().remove(domain, "\\.");
                }
            }
        }
    }

    @Override
    public void clear() {
        switch (matcher) {
            case ExactMatcher exact -> exact.list().clear();
            case WildcardMatcher wildcard -> wildcard.list().clear();
        }
    }

    @Override
    public boolean isAllowed(String domain) {
        return !matcher.matches(domain);
    }

}