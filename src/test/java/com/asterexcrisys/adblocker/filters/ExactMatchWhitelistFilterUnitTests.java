package com.asterexcrisys.adblocker.filters;

import com.asterexcrisys.adblocker.matchers.ExactMatcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ExactMatchWhitelistFilterUnitTests {

    private Filter filter;

    @BeforeAll
    public void setUp() {
        filter = new WhitelistFilter();
        filter.setMatcher(new ExactMatcher());
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
    public void shouldReturnTrueWhenInWhitelist() {
        Assertions.assertTrue(filter.isAllowed("google.com"));
    }

    @Test
    public void shouldThrowExceptionWhenIsNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> filter.isAllowed(null));
    }

}