package com.asterexcrisys.adblocker.filters;

import com.asterexcrisys.adblocker.matchers.WildcardMatcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WildcardMatchBlacklistFilterUnitTests {

    private Filter filter;

    @BeforeAll
    public void setUp() {
        filter = new BlacklistFilter();
        filter.setMatcher(new WildcardMatcher());
        filter.load(List.of(
                "google.com", "youtube.com", "facebook.com",
                "apple.com", "amazon.com", "microsoft.com",
                "*.altervista.org", "*.net", "*.io"
        ));
    }

    @Test
    public void shouldReturnTrueWhenNotInBlacklist() {
        Assertions.assertTrue(filter.isAllowed("cloudflare.com"));
    }

    @Test
    public void shouldReturnFalseWhenInBlacklistExactly() {
        Assertions.assertFalse(filter.isAllowed("google.com"));
    }

    @Test
    public void shouldReturnFalseWhenInBlacklistPartially() {
        Assertions.assertFalse(filter.isAllowed("github.io"));
    }

    @Test
    public void shouldThrowExceptionWhenIsNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> filter.isAllowed(null));
    }

    @Test
    public void shouldThrowExceptionWhenContainsWildcard() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> filter.isAllowed("*.example.com"));
    }

}