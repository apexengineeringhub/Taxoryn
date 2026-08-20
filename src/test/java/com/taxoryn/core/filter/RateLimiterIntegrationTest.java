package com.taxoryn.core.filter;

import com.taxoryn.core.security.RateLimitingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimiterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        rateLimitingService.reset();
        ReflectionTestUtils.setField(rateLimitingService, "enabled", true);
        ReflectionTestUtils.setField(rateLimitingService, "apiLimitPerMinute", 3);
    }

    @AfterEach
    void tearDown() {
        rateLimitingService.reset();
        ReflectionTestUtils.setField(rateLimitingService, "apiLimitPerMinute", 300);
    }

    @Test
    @DisplayName("RateLimitingFilter should return headers and enforce HTTP 429 when quota exceeded")
    void testRateLimitEnforcement() throws Exception {
        String testIp = "10.0.0.99";

        // Request 1: Allowed (Remaining = 2)
        mockMvc.perform(get("/api/v1/subscriptions/plans")
                        .header("X-Forwarded-For", testIp))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "3"))
                .andExpect(header().string("X-RateLimit-Remaining", "2"));

        // Request 2: Allowed (Remaining = 1)
        mockMvc.perform(get("/api/v1/subscriptions/plans")
                        .header("X-Forwarded-For", testIp))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Remaining", "1"));

        // Request 3: Allowed (Remaining = 0)
        mockMvc.perform(get("/api/v1/subscriptions/plans")
                        .header("X-Forwarded-For", testIp))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Remaining", "0"));

        // Request 4: Exceeded -> HTTP 429
        mockMvc.perform(get("/api/v1/subscriptions/plans")
                        .header("X-Forwarded-For", testIp))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.status").value(429));
    }
}
