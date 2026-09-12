package com.taxoryn.core.security.cors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "taxoryn.cors.allowed-origins=https://app.taxoryn.com,https://taxoryn.com",
        "taxoryn.cors.allow-credentials=true"
})
class ProductionCorsHardeningSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Production CORS: Allows primary frontend origin https://app.taxoryn.com")
    void testAppTaxorynOriginAllowed() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://app.taxoryn.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.taxoryn.com"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("Production CORS: Allows configured apex marketing origin https://taxoryn.com")
    void testApexTaxorynOriginAllowed() throws Exception {
        mockMvc.perform(options("/api/v1/marketplace/search")
                        .header(HttpHeaders.ORIGIN, "https://taxoryn.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://taxoryn.com"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("Production CORS: Rejects arbitrary attacker origin https://evil.example")
    void testEvilExampleOriginRejected() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type,Authorization"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Production CORS: Rejects arbitrary Vercel domain https://random.vercel.app")
    void testRandomVercelOriginRejected() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://random.vercel.app")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type,Authorization"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Production CORS: Rejects demo Vercel domain https://taxoryn-7x7f.vercel.app in production")
    void testDemoVercelOriginRejectedInProd() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://taxoryn-7x7f.vercel.app")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type,Authorization"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Production CORS: Rejects localhost http://localhost:5173 in production")
    void testLocalhostOriginRejectedInProd() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type,Authorization"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Production CORS: Rejects loopback IP http://127.0.0.1:8080 in production")
    void testLoopbackIpOriginRejectedInProd() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:8080")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type,Authorization"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Production CORS: Simple request with unapproved origin is rejected with 403 Forbidden and omits CORS header")
    void testSimpleRequestWithUntrustedOriginRejected() throws Exception {
        mockMvc.perform(get("/api/health")
                        .header(HttpHeaders.ORIGIN, "https://evil.example"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Production CORS: Simple request with approved origin includes Access-Control-Allow-Origin header")
    void testSimpleRequestWithTrustedOriginIncludesCorsHeader() throws Exception {
        mockMvc.perform(get("/api/health")
                        .header(HttpHeaders.ORIGIN, "https://app.taxoryn.com"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.taxoryn.com"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("Production CORS: Rejects arbitrary subdomain https://evil.taxoryn.com")
    void testEvilSubdomainRejectedInProd() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://evil.taxoryn.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type,Authorization"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Production CORS: Rejects tenant subdomain https://tenant.taxoryn.com")
    void testTenantSubdomainRejectedInProd() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://tenant.taxoryn.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type,Authorization"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Production CORS: Rejects insecure HTTP scheme http://app.taxoryn.com")
    void testHttpSchemeOriginRejectedInProd() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://app.taxoryn.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type,Authorization"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Config Safety: application-local.yml contains no hardcoded mail credentials")
    void testApplicationLocalContainsNoHardcodedMailPassword() throws Exception {
        org.springframework.core.io.Resource resource = new org.springframework.core.io.ClassPathResource("application-local.yml");
        if (resource.exists()) {
            String content = new String(resource.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            org.assertj.core.api.Assertions.assertThat(content).doesNotContain("taxoryn@gmail.com");
            org.assertj.core.api.Assertions.assertThat(content).doesNotContain("hudd pluc");
            org.assertj.core.api.Assertions.assertThat(content).contains("username: ${SPRING_MAIL_USERNAME:");
            org.assertj.core.api.Assertions.assertThat(content).contains("password: ${SPRING_MAIL_PASSWORD:");
        }
    }
}
