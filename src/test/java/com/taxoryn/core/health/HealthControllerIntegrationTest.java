package com.taxoryn.core.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the lightweight {@code /api/health} liveness endpoint behaves as required for
 * Render's health checker and external uptime monitors, and that adding it did not weaken
 * security on the rest of the API.
 * <p>
 * Uses the {@code test} Spring profile, which is backed by an in-memory H2 database
 * (see {@code application-test.yml}) - no real PostgreSQL instance is required to run this test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/health returns 200 with {status: UP} and requires no authentication")
    void healthEndpoint_returnsUp_withoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", equalTo("UP")));
    }

    @Test
    @DisplayName("GET /api/health does not count against the API rate-limit bucket")
    void healthEndpoint_isExcludedFromRateLimitHeaders() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-RateLimit-Limit"));
    }

    @Test
    @DisplayName("Existing authenticated APIs still require a token after adding /api/health")
    void protectedEndpoint_stillRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Existing /api/v1 authenticated APIs still require a token after adding /api/health")
    void protectedV1Endpoint_stillRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().isUnauthorized());
    }
}
