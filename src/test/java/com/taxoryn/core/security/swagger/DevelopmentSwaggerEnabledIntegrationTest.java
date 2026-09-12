package com.taxoryn.core.security.swagger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration Test verifying that Swagger UI and OpenAPI documentation endpoints
 * remain fully functional in development / non-production environments when enabled.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "springdoc.api-docs.enabled=true",
        "springdoc.swagger-ui.enabled=true"
})
class DevelopmentSwaggerEnabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Development: OpenAPI JSON documentation endpoint is available when enabled")
    void testOpenApiDocsAvailableInDevelopment() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("Taxoryn API — Practice Management Platform"));
    }

    @Test
    @DisplayName("Development: Swagger UI html endpoint is accessible when enabled")
    void testSwaggerUiHtmlAccessibleInDevelopment() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }
}
