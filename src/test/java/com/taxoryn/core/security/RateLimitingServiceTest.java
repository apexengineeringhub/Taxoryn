package com.taxoryn.core.security;

import com.taxoryn.core.security.RateLimitingService.RateLimitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitingServiceTest {

    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        rateLimitingService = new RateLimitingService();
        ReflectionTestUtils.setField(rateLimitingService, "enabled", true);
        ReflectionTestUtils.setField(rateLimitingService, "authLimitPerMinute", 5);
        ReflectionTestUtils.setField(rateLimitingService, "apiLimitPerMinute", 10);
    }

    @Test
    @DisplayName("Should allow requests within configured auth limit")
    void testAuthRateLimitAllowed() {
        String clientIp = "192.168.1.100";

        for (int i = 0; i < 5; i++) {
            RateLimitResult result = rateLimitingService.checkRateLimit(clientIp, true);
            assertTrue(result.isAllowed(), "Request " + (i + 1) + " should be allowed");
            assertEquals(5, result.getLimit());
            assertEquals(4 - i, result.getRemaining());
        }
    }

    @Test
    @DisplayName("Should reject requests exceeding auth limit with HTTP 429 semantics")
    void testAuthRateLimitExceeded() {
        String clientIp = "192.168.1.101";

        for (int i = 0; i < 5; i++) {
            rateLimitingService.checkRateLimit(clientIp, true);
        }

        RateLimitResult exceededResult = rateLimitingService.checkRateLimit(clientIp, true);
        assertFalse(exceededResult.isAllowed());
        assertEquals(0, exceededResult.getRemaining());
        assertTrue(exceededResult.getRetryAfterSeconds() > 0);
    }

    @Test
    @DisplayName("Should separate auth and general API rate limits per client IP")
    void testDistinctLimitsPerEndpointType() {
        String clientIp = "192.168.1.102";

        // Consume 5 auth tokens (exhausting auth limit)
        for (int i = 0; i < 5; i++) {
            rateLimitingService.checkRateLimit(clientIp, true);
        }
        assertFalse(rateLimitingService.checkRateLimit(clientIp, true).isAllowed());

        // General API should still have its separate quota (10 limit)
        RateLimitResult apiResult = rateLimitingService.checkRateLimit(clientIp, false);
        assertTrue(apiResult.isAllowed());
        assertEquals(10, apiResult.getLimit());
        assertEquals(9, apiResult.getRemaining());
    }

    @Test
    @DisplayName("Should allow all requests when rate limiting is disabled")
    void testRateLimitingDisabled() {
        ReflectionTestUtils.setField(rateLimitingService, "enabled", false);
        String clientIp = "192.168.1.103";

        for (int i = 0; i < 20; i++) {
            RateLimitResult result = rateLimitingService.checkRateLimit(clientIp, true);
            assertTrue(result.isAllowed());
        }
    }
}
