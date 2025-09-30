package com.asterexcrisys.adblocker.filters;

import com.asterexcrisys.adblocker.matchers.ExactMatcher;
import com.asterexcrisys.adblocker.matchers.Matcher;
import com.asterexcrisys.adblocker.matchers.WildcardMatcher;
import java.util.Collection;
import java.util.Objects;

@SuppressWarnings("unused")
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
        switch (matcher) {
            case ExactMatcher exact -> exact.add(domain);
            case WildcardMatcher wildcard -> wildcard.add(domain);
        }
    }

    @Override
    public void load(Collection<String> domains) {
        switch (matcher) {
            case ExactMatcher exact -> exact.addAll(domains);
            case WildcardMatcher wildcard -> wildcard.addAll(domains);
        }
    }

    @Override
    public void unload(String domain) {
        switch (matcher) {
            case ExactMatcher exact -> exact.remove(domain);
            case WildcardMatcher wildcard -> wildcard.remove(domain);
        }
    }

    @Override
    public void unload(Collection<String> domains) {
        switch (matcher) {
            case ExactMatcher exact -> exact.removeAll(domains);
            case WildcardMatcher wildcard -> wildcard.removeAll(domains);
        }
    }

    @Override
    public void clear() {
        switch (matcher) {
            case ExactMatcher exact -> exact.clear();
            case WildcardMatcher wildcard -> wildcard.clear();
        }
    }

    @Override
    public boolean isAllowed(String domain) {
        return matcher.matches(domain);
    }

}