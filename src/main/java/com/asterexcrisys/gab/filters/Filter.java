package com.asterexcrisys.gab.filters;

import com.asterexcrisys.gab.matchers.Matcher;
import java.util.Collection;

@SuppressWarnings("unused")
public sealed interface Filter permits WhitelistFilter, BlacklistFilter {

    void setMatcher(Matcher matcher);

    void load(String domain);

    void load(Collection<String> domains);

    void unload(String domain);

    void unload(Collection<String> domains);

    void clear();

    boolean isAllowed(String domain);

}