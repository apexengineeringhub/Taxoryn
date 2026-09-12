package com.taxoryn.core.security.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.RateLimitingService;
import com.taxoryn.module.authentication.dto.ForgotPasswordRequest;
import com.taxoryn.module.authentication.dto.LoginRequest;
import com.taxoryn.module.authentication.dto.RegisterOrganizationRequest;
import com.taxoryn.module.marketplace.dto.RegisterCustomerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TrustedProxyAndRateLimitingSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimitingService rateLimitingService;

    @Autowired
    private ClientIpResolver clientIpResolver;

    @Autowired
    private TrustedProxyProperties trustedProxyProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        rateLimitingService.reset();
        ReflectionTestUtils.setField(rateLimitingService, "enabled", true);
        ReflectionTestUtils.setField(rateLimitingService, "authLimitPerMinute", 3);
        ReflectionTestUtils.setField(rateLimitingService, "apiLimitPerMinute", 5);
        trustedProxyProperties.setEnabled(true);
        trustedProxyProperties.setTrustAllProxies(false);
        clientIpResolver.reloadMatchers();
    }

    @AfterEach
    void tearDown() {
        rateLimitingService.reset();
        ReflectionTestUtils.setField(rateLimitingService, "enabled", false);
        ReflectionTestUtils.setField(rateLimitingService, "authLimitPerMinute", 15);
        ReflectionTestUtils.setField(rateLimitingService, "apiLimitPerMinute", 300);
    }

    @Test
    @DisplayName("1. Repeated login attempts exceeding auth limit must return HTTP 429 Too Many Requests")
    void testRepeatedLoginAttemptsRateLimited() throws Exception {
        LoginRequest loginRequest = new LoginRequest("admin@taxoryn.com", "WrongPassword123!");
        String json = objectMapper.writeValueAsString(loginRequest);

        // Attempts 1, 2, 3: Within quota (may fail auth with 401/404, but not 429)
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(request -> {
                                request.setRemoteAddr("10.0.0.50"); // Trusted proxy
                                return request;
                            })
                            .header("X-Forwarded-For", "198.51.100.10"))
                    .andExpect(header().string("X-RateLimit-Limit", "3"))
                    .andExpect(header().string("X-RateLimit-Remaining", String.valueOf(2 - i)));
        }

        // Attempt 4: Exceeds auth quota -> HTTP 429 Too Many Requests
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.50");
                            return request;
                        })
                        .header("X-Forwarded-For", "198.51.100.10"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    @DisplayName("2. Repeated forgot password attempts exceeding quota must return HTTP 429")
    void testForgotPasswordRateLimited() throws Exception {
        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest("user@taxoryn.com");
        String json = objectMapper.writeValueAsString(forgotRequest);

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(request -> {
                                request.setRemoteAddr("10.0.0.51");
                                return request;
                            })
                            .header("X-Forwarded-For", "198.51.100.20"))
                    .andExpect(header().string("X-RateLimit-Remaining", String.valueOf(2 - i)));
        }

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.51");
                            return request;
                        })
                        .header("X-Forwarded-For", "198.51.100.20"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("3. Repeated registration attempts on auth & marketplace must be throttled by auth rate limit")
    void testRegistrationRateLimited() throws Exception {
        RegisterCustomerRequest customerRequest = RegisterCustomerRequest.builder()
                .firstName("Rate Test User")
                .email("ratetest@example.com")
                .phone("9876543210")
                .password("Password123!@")
                .build();
        String json = objectMapper.writeValueAsString(customerRequest);

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/marketplace/customer/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(request -> {
                                request.setRemoteAddr("10.0.0.52");
                                return request;
                            })
                            .header("X-Forwarded-For", "198.51.100.30"))
                    .andExpect(header().string("X-RateLimit-Remaining", String.valueOf(2 - i)));
        }

        // 4th request -> blocked
        mockMvc.perform(post("/api/v1/marketplace/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.52");
                            return request;
                        })
                        .header("X-Forwarded-For", "198.51.100.30"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("4. General API abuse exceeding general API limit must return HTTP 429")
    void testGeneralApiRateLimiting() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/v1/subscriptions/plans")
                            .with(request -> {
                                request.setRemoteAddr("10.0.0.53");
                                return request;
                            })
                            .header("X-Forwarded-For", "198.51.100.40"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-RateLimit-Limit", "5"))
                    .andExpect(header().string("X-RateLimit-Remaining", String.valueOf(4 - i)));
        }

        // 6th request -> blocked
        mockMvc.perform(get("/api/v1/subscriptions/plans")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.53");
                            return request;
                        })
                        .header("X-Forwarded-For", "198.51.100.40"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("5. Untrusted direct client connection sending spoofed X-Forwarded-For headers cannot bypass rate limiting")
    void testDirectUntrustedConnectionIgnoresSpoofedHeaders() throws Exception {
        String attackerSocketIp = "203.0.113.199"; // Public untrusted IP (not in trusted-proxies)

        // Attacker attempts 3 requests with changing spoofed X-Forwarded-For headers
        for (int i = 0; i < 3; i++) {
            String fakeIp = "1.2.3." + (i + 1);
            mockMvc.perform(get("/api/v1/subscriptions/plans")
                            .with(request -> {
                                request.setRemoteAddr(attackerSocketIp);
                                return request;
                            })
                            .header("X-Forwarded-For", fakeIp))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-RateLimit-Remaining", String.valueOf(4 - i)));
        }

        // 4th and 5th requests from same socket IP with different spoofed headers
        mockMvc.perform(get("/api/v1/subscriptions/plans")
                        .with(request -> {
                            request.setRemoteAddr(attackerSocketIp);
                            return request;
                        })
                        .header("X-Forwarded-For", "9.9.9.9"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Remaining", "1"));

        mockMvc.perform(get("/api/v1/subscriptions/plans")
                        .with(request -> {
                            request.setRemoteAddr(attackerSocketIp);
                            return request;
                        })
                        .header("X-Forwarded-For", "8.8.8.8"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Remaining", "0"));

        // 6th request from attacker socket IP is BLOCKED despite spoofing a new fake IP!
        mockMvc.perform(get("/api/v1/subscriptions/plans")
                        .with(request -> {
                            request.setRemoteAddr(attackerSocketIp);
                            return request;
                        })
                        .header("X-Forwarded-For", "7.7.7.7"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("6. Trusted proxy (Render / Private Network) correctly extracts genuine client IP from X-Forwarded-For chain")
    void testTrustedProxyXForwardedForExtraction() throws Exception {
        // Render / Private subnet proxy IP: 172.18.0.5
        mockMvc.perform(get("/api/v1/subscriptions/plans")
                        .with(request -> {
                            request.setRemoteAddr("172.18.0.5");
                            return request;
                        })
                        .header("X-Forwarded-For", "203.0.113.88, 172.18.0.5"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Remaining", "4"));

        // A second request with a DIFFERENT client IP through the same Render proxy should have its own fresh bucket
        mockMvc.perform(get("/api/v1/subscriptions/plans")
                        .with(request -> {
                            request.setRemoteAddr("172.18.0.5");
                            return request;
                        })
                        .header("X-Forwarded-For", "203.0.113.99, 172.18.0.5"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Remaining", "4"));
    }

    @Test
    @DisplayName("7. Trusted proxy prioritizes Cloudflare CF-Connecting-IP when configured")
    void testCloudflareHeaderPriority() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions/plans")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.1"); // Trusted private proxy
                            return request;
                        })
                        .header("CF-Connecting-IP", "198.51.100.77")
                        .header("X-Forwarded-For", "1.1.1.1, 10.0.0.1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Remaining", "4"));
    }

    @Test
    @DisplayName("8. ClientIpResolver unit validation and IP sanitization checks")
    void testClientIpResolverUnitValidation() {
        assertTrue(clientIpResolver.isTrustedProxy("127.0.0.1"));
        assertTrue(clientIpResolver.isTrustedProxy("10.0.5.2"));
        assertTrue(clientIpResolver.isTrustedProxy("172.20.0.1"));
        assertTrue(clientIpResolver.isTrustedProxy("192.168.1.100"));
        assertTrue(clientIpResolver.isTrustedProxy("::1"));

        assertFalse(clientIpResolver.isTrustedProxy("203.0.113.1"));
        assertFalse(clientIpResolver.isTrustedProxy("8.8.8.8"));
        assertFalse(clientIpResolver.isTrustedProxy("invalid-ip"));

        assertEquals("192.168.1.1", clientIpResolver.sanitizeIp("192.168.1.1:8080"));
        assertEquals("2001:db8::1", clientIpResolver.sanitizeIp("[2001:db8::1]:443"));
        assertEquals("127.0.0.1", clientIpResolver.sanitizeIp("localhost"));
        assertEquals("127.0.0.1", clientIpResolver.sanitizeIp("0:0:0:0:0:0:0:1"));
    }
}
