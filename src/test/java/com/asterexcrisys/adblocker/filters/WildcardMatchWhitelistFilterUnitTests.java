package com.asterexcrisys.adblocker.filters;

import com.asterexcrisys.adblocker.matchers.WildcardMatcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WildcardMatchWhitelistFilterUnitTests {

    private Filter filter;

    @BeforeAll
    public void setUp() {
        filter = new WhitelistFilter();
        filter.setMatcher(new WildcardMatcher());
        filter.load(List.of(
                "google.com", "youtube.com", "facebook.com",
                "apple.com", "amazon.com", "microsoft.com",
                "*.altervista.org", "*.net", "*.io"
        ));
    }

    @Test
    public void shouldReturnFalseWhenNotInWhitelist() {
        Assertions.assertFalse(filter.isAllowed("cloudflare.com"));
    }

    @Test
    public void shouldReturnTrueWhenInWhitelistExactly() {
        Assertions.assertTrue(filter.isAllowed("google.com"));
    }

    @Test
    public void shouldReturnTrueWhenInWhitelistPartially() {
        Assertions.assertTrue(filter.isAllowed("github.io"));
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