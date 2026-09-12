package com.taxoryn.core.security.swagger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security Integration Test verifying that Swagger UI and OpenAPI documentation endpoints
 * are strictly disabled and blocked when springdoc is disabled (Production Mode).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
class ProductionSwaggerDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {
            "/swagger-ui/",
            "/swagger-ui/index.html",
            "/swagger-ui.html",
            "/swagger-ui/swagger-ui.css",
            "/swagger-ui/swagger-ui-bundle.js",
            "/v3/api-docs",
            "/v3/api-docs/",
            "/v3/api-docs/swagger-config",
            "/api-docs",
            "/api-docs/",
            "/api-docs/swagger-config",
            "/swagger-resources",
            "/swagger-resources/configuration/ui",
            "/swagger-resources/configuration/security",
            "/webjars/swagger-ui/index.html"
    })
    @DisplayName("Security: Documentation endpoints are blocked and do not expose API specs in production")
    void testDocumentationEndpointsAreInaccessibleInProduction(String endpoint) throws Exception {
        MvcResult result = mockMvc.perform(get(endpoint))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        // Assert that OpenAPI schema details, endpoints, and internal models are never leaked
        assertThat(responseBody).doesNotContain("\"openapi\":");
        assertThat(responseBody).doesNotContain("\"swagger\":");
        assertThat(responseBody).doesNotContain("Swagger UI");
        assertThat(responseBody).doesNotContain("\"paths\":");
        assertThat(responseBody).doesNotContain("\"components\":");
        assertThat(responseBody).doesNotContain("<!DOCTYPE html>");
        assertThat(responseBody).doesNotContain("SwaggerUIBundle");
    }

    @Test
    @DisplayName("Security: Business and health APIs remain fully operational when documentation is disabled")
    void testBusinessAndHealthApisRemainOperational() throws Exception {
        // Public Health Check
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        // Actuator Health Check
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
