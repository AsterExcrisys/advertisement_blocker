package com.asterexcrisys.aab.filters;

import com.asterexcrisys.aab.matchers.Matcher;
import java.util.List;

public sealed interface Filter permits WhitelistFilter, BlacklistFilter {

    void setMatcher(Matcher matcher);

    void load(String domain);

    void load(List<String> domains);

    void unload(String domain);

    void unload();

    boolean isAllowed(String domain);

}