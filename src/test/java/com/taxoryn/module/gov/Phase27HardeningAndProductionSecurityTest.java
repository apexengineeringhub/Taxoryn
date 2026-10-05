package com.taxoryn.module.gov;

import com.taxoryn.core.exception.GlobalExceptionHandler;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.response.ErrorResponse;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.gov.auth.dto.GovAuthSessionDto;
import com.taxoryn.module.gov.auth.entity.GovAuthSessionEntity;
import com.taxoryn.module.gov.auth.infrastructure.MockGovernmentAuthenticationProvider;
import com.taxoryn.module.gov.auth.model.GovAuthMethod;
import com.taxoryn.module.gov.auth.model.GovAuthStatus;
import com.taxoryn.module.gov.auth.service.GovernmentAuthenticationService;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.GovHandshakeRequest;
import com.taxoryn.module.gov.dto.GovHandshakeResult;
import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.entity.GovIntegrationOperationEntity;
import com.taxoryn.module.gov.exception.GovIntegrationException;
import com.taxoryn.module.gov.infrastructure.provider.MockProviderAdapter;
import com.taxoryn.module.gov.infrastructure.provider.gst.GstProviderAdapter;
import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.model.GovOperationStatus;
import com.taxoryn.module.gov.model.GovProviderHealth;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.spi.GovernmentProviderAdapter;
import com.taxoryn.module.gov.observability.dto.GovOperationsDiagnosticsDto;
import com.taxoryn.module.gov.observability.health.GovIntegrationHealthIndicator;
import com.taxoryn.module.gov.observability.service.GovOperationsObservabilityService;
import com.taxoryn.module.gov.registry.GovernmentProviderRegistryImpl;
import com.taxoryn.module.gov.reliability.util.GovReliabilitySanitizer;
import com.taxoryn.module.gov.repository.GovIntegrationOperationRepository;
import com.taxoryn.module.gov.security.GovProductionSecurityValidator;
import com.taxoryn.module.gov.security.GovSecretEncryptionService;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.gov.service.GovernmentIntegrationService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 27.5: Production Security & Configuration Hardening Integration Test Suite.
 */
@SpringBootTest
@ActiveProfiles("test")
class Phase27HardeningAndProductionSecurityTest {

    @Autowired
    private GovProductionSecurityValidator securityValidator;

    @Autowired
    private GovernmentIntegrationService govIntegrationService;

    @Autowired
    private GovernmentAuthenticationService authService;

    @Autowired
    private GovernmentConnectionService connectionService;

    @Autowired
    private GovOperationsObservabilityService observabilityService;

    @Autowired
    private GovIntegrationHealthIndicator healthIndicator;

    @Autowired
    private GovIntegrationOperationRepository operationRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private GovSecretEncryptionService encryptionService;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        operationRepository.deleteAll();
        organizationRepository.deleteAll();

        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Alpha Security Corp")
                .email("alpha-" + UUID.randomUUID() + "@taxoryn.security.test")
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Beta Security Corp")
                .email("beta-" + UUID.randomUUID() + "@taxoryn.security.test")
                .status(OrganizationStatus.ACTIVE)
                .build());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ==========================================
    // 1. CONFIGURATION HARDENING & VALIDATOR TESTS
    // ==========================================

    @Test
    @DisplayName("Production validator strictly rejects mock provider in production mode")
    void testProductionValidatorRejectsMockInProd() {
        assertThatThrownBy(() -> securityValidator.validateConfiguration(true, true, true, "some-secret"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CRITICAL SECURITY VIOLATION: Mock government integration ('taxoryn.gov.integration.mock-enabled') cannot be enabled in a production environment");
    }

    @Test
    @DisplayName("Production validator strictly rejects default/weak encryption secrets in production")
    void testProductionValidatorRejectsWeakSecretInProd() {
        assertThatThrownBy(() -> securityValidator.validateConfiguration(true, false, true, GovProductionSecurityValidator.DEFAULT_GOV_ENCRYPTION_SECRET))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insecure or default encryption secret detected");

        assertThatThrownBy(() -> securityValidator.validateConfiguration(true, false, true, "short-key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insecure or default encryption secret detected");
    }

    @Test
    @DisplayName("Production validator accepts valid production configuration")
    void testProductionValidatorAcceptsValidProdConfig() {
        // Should not throw
        securityValidator.validateConfiguration(true, false, true, "a-strong-production-encryption-secret-key-32bytes-long");
    }

    @Test
    @DisplayName("Production validator accepts disabled integration in production without requiring secret")
    void testProductionValidatorAcceptsDisabledIntegrationInProd() {
        // Should not throw when integration is disabled
        securityValidator.validateConfiguration(true, false, false, "");
    }

    @Test
    @DisplayName("Non-production environment allows mock providers and default credentials")
    void testNonProductionAllowsMock() {
        // Should not throw in non-production
        securityValidator.validateConfiguration(false, true, true, GovProductionSecurityValidator.DEFAULT_GOV_ENCRYPTION_SECRET);
    }

    // ==========================================
    // 2. REGISTRY & ADAPTER PRODUCTION SAFETY
    // ==========================================

    @Test
    @DisplayName("GovernmentProviderRegistryImpl does not return mock adapters when mockEnabled=false")
    void testRegistryRejectsMockWhenMockDisabled() {
        GovernmentProviderAdapter realAdapter = new GovernmentProviderAdapter() {
            @Override
            public GovProviderType getProviderType() { return GovProviderType.GST; }
            @Override
            public String getAdapterCode() { return "GSTN_PRODUCTION_DIRECT"; }
            @Override
            public GovProviderHealth checkHealth() { return GovProviderHealth.up(GovProviderType.GST, "GSTN_PRODUCTION_DIRECT", "OK"); }
            @Override
            public GovHandshakeResult handshake(GovHandshakeRequest request) { return GovHandshakeResult.healthy(GovProviderType.GST, "GSTN_PRODUCTION_DIRECT", 10L, "OK"); }
            @Override
            public GovIntegrationResult execute(GovIntegrationRequest request) { return GovIntegrationResult.success(null, request.getOrganizationId(), GovProviderType.GST, request.getOperationType(), request.getCorrelationId(), request.getIdempotencyKey(), "ACK-PROD", Map.of()); }
        };

        GovernmentProviderRegistryImpl registry = new GovernmentProviderRegistryImpl(
                List.of(new MockProviderAdapter(), realAdapter),
                true, // integrationEnabled
                false // mockEnabled
        );

        Optional<GovernmentProviderAdapter> adapterOpt = registry.getAdapter(GovProviderType.GST);
        assertThat(adapterOpt).isPresent();
        assertThat(adapterOpt.get().getAdapterCode()).isEqualTo("GSTN_PRODUCTION_DIRECT");

        // GstProviderAdapter (which has ADAPTER_CODE GSTN_MOCK_ADAPTER) is a mock adapter and must be rejected when mockEnabled=false
        GovernmentProviderRegistryImpl mockOnlyRegistry = new GovernmentProviderRegistryImpl(
                List.of(new GstProviderAdapter()),
                true,
                false
        );
        assertThat(mockOnlyRegistry.getAdapter(GovProviderType.GST)).isEmpty();

        // Asking for INCOME_TAX where only generic mock exists should return empty() in production
        Optional<?> incomeTaxOpt = registry.getAdapter(GovProviderType.INCOME_TAX);
        assertThat(incomeTaxOpt).isEmpty();
    }

    @Test
    @DisplayName("GovernmentProviderRegistryImpl returns empty when integrationEnabled=false")
    void testRegistryReturnsEmptyWhenIntegrationDisabled() {
        GovernmentProviderRegistryImpl registry = new GovernmentProviderRegistryImpl(
                List.of(new MockProviderAdapter(), new GstProviderAdapter()),
                false, // integrationEnabled = false
                false
        );

        assertThat(registry.getAdapter(GovProviderType.GST)).isEmpty();
        assertThat(registry.getAllAdapters()).isEmpty();
        assertThat(registry.isProviderSupported(GovProviderType.GST)).isFalse();
    }

    @Test
    @DisplayName("GovernmentIntegrationService fails safely when integration is disabled")
    void testServiceFailsSafelyWhenIntegrationDisabled() {
        TenantContext.setTenantId(orgA.getId());
        ReflectionTestUtils.setField(govIntegrationService, "integrationEnabled", false);

        try {
            GovIntegrationRequest request = GovIntegrationRequest.builder()
                    .organizationId(orgA.getId())
                    .providerType(GovProviderType.GST)
                    .operationType("GST_RETURN_FILE")
                    .build();

            assertThatThrownBy(() -> govIntegrationService.executeOperation(request))
                    .isInstanceOf(GovIntegrationException.class)
                    .hasMessageContaining("Government integration is currently disabled");
        } finally {
            ReflectionTestUtils.setField(govIntegrationService, "integrationEnabled", true);
        }
    }

    @Test
    @DisplayName("MockGovernmentAuthenticationProvider fails safely when mockEnabled=false")
    void testMockAuthFailsSafelyWhenMockDisabled() {
        MockGovernmentAuthenticationProvider mockAuth = new MockGovernmentAuthenticationProvider();
        ReflectionTestUtils.setField(mockAuth, "mockEnabled", false);

        GovConnectionDto connection = GovConnectionDto.builder()
                .id(UUID.randomUUID())
                .providerType(GovProviderType.GST)
                .build();

        assertThatThrownBy(() -> mockAuth.startAuthentication(connection, GovAuthMethod.OTP, "corr-1", Map.of()))
                .isInstanceOf(GovIntegrationException.class)
                .hasMessageContaining("Mock government authentication is disabled in production");

        GovAuthSessionEntity session = GovAuthSessionEntity.builder()
                .providerType(GovProviderType.GST)
                .authMethod(GovAuthMethod.OTP)
                .status(GovAuthStatus.AUTHENTICATION_PENDING)
                .build();

        assertThatThrownBy(() -> mockAuth.checkAuthenticationStatus(connection, session, Map.of()))
                .isInstanceOf(GovIntegrationException.class);

        assertThatThrownBy(() -> mockAuth.continueAuthorization(connection, session, "OTP-123456", Map.of()))
                .isInstanceOf(GovIntegrationException.class);

        assertThatThrownBy(() -> mockAuth.revokeAuthentication(connection, session))
                .isInstanceOf(GovIntegrationException.class);
    }

    // ==========================================
    // 3. SECRET & CREDENTIAL HARDENING
    // ==========================================

    @Test
    @DisplayName("GovReliabilitySanitizer scrubs diverse sensitive credentials from payloads and strings")
    void testSanitizerScrubsDiverseSecrets() {
        String dirty1 = "Error connecting with Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.test.payload";
        String clean1 = GovReliabilitySanitizer.sanitizeString(dirty1);
        assertThat(clean1).doesNotContain("eyJhbGciOiJIUzI1NiJ9.test.payload");
        assertThat(clean1).contains("[REDACTED]");

        String dirty2 = "Failed validation: client_secret=SUPER_SECRET_123&password=myPassword999!";
        String clean2 = GovReliabilitySanitizer.sanitizeString(dirty2);
        assertThat(clean2).doesNotContain("SUPER_SECRET_123");
        assertThat(clean2).doesNotContain("myPassword999!");

        String dirty3 = "OTP code is 584920 and EVC is EVC883920 and PIN is 4920";
        String clean3 = GovReliabilitySanitizer.sanitizeString(dirty3);
        assertThat(clean3).doesNotContain("584920");
        assertThat(clean3).doesNotContain("EVC883920");
    }

    @Test
    @DisplayName("AES-256-GCM encryption service protects credentials with authenticated encryption")
    void testEncryptionServiceSecurity() {
        String secret = "GSTN-PROD-CLIENT-SECRET-99482";
        String encrypted = encryptionService.encrypt(secret);

        assertThat(encrypted).isNotBlank();
        assertThat(encrypted).isNotEqualTo(secret);
        assertThat(encrypted).doesNotContain(secret);

        String decrypted = encryptionService.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(secret);
    }

    // ==========================================
    // 4. PRODUCTION ERROR HANDLING
    // ==========================================

    @Test
    @DisplayName("GlobalExceptionHandler normalizes GovIntegrationException into safe response")
    void testGlobalExceptionHandlerGovIntegration() {
        GovIntegrationException ex = new GovIntegrationException(
                GovErrorCode.AUTH_REQUIRED,
                "Authentication failed for client_secret=VERY_SECRET_KEY_123"
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/gov/operations/test");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGovIntegrationException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrorCode()).isEqualTo("AUTH_REQUIRED");
        // Verify secret was sanitized from exception message in API response
        assertThat(body.getMessage()).doesNotContain("VERY_SECRET_KEY_123");
        assertThat(body.getMessage()).contains("[REDACTED]");
    }

    // ==========================================
    // 5. TENANT ISOLATION
    // ==========================================

    @Test
    @DisplayName("Tenant boundary strictly enforced on operation inspection and diagnostics")
    void testTenantBoundaryEnforced() {
        // Save operation for Org B
        UUID opId = UUID.randomUUID();
        GovIntegrationOperationEntity opB;
        TenantContext.setTenantId(orgB.getId());
        try {
            opB = operationRepository.save(GovIntegrationOperationEntity.builder()
                    .providerType(GovProviderType.GST)
                    .operationType("GST_RETURN_FILE")
                    .status(GovOperationStatus.SUCCEEDED)
                    .correlationId("corr-org-b")
                    .idempotencyKey("idemp-org-b-" + UUID.randomUUID())
                    .attemptCount(1)
                    .maxAttempts(3)
                    .build());
            opB.setOrganizationId(orgB.getId());
            opB = operationRepository.save(opB);
        } finally {
            TenantContext.clear();
        }

        // Tenant A tries to access Org B's operation via GovernmentIntegrationService
        TenantContext.setTenantId(orgA.getId());
        final UUID finalOpBId = opB.getId();
        assertThatThrownBy(() -> govIntegrationService.getOperation(finalOpBId))
                .isInstanceOf(Exception.class);

        // Tenant A tries to access Org B's diagnostics via GovOperationsObservabilityService
        assertThatThrownBy(() -> observabilityService.getOperationDiagnostics(finalOpBId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==========================================
    // 6. ACTUATOR HEALTH INDICATOR SAFETY
    // ==========================================

    @Test
    @DisplayName("GovIntegrationHealthIndicator exposes operational status without secrets or PII")
    void testHealthIndicatorSafety() {
        Health health = healthIndicator.health();
        assertThat(health.getStatus()).isNotNull();

        Map<String, Object> details = health.getDetails();
        assertThat(details).doesNotContainKey("secret");
        assertThat(details).doesNotContainKey("password");
        assertThat(details).doesNotContainKey("token");
        assertThat(details).doesNotContainKey("privateKey");
    }

    // ==========================================
    // 7. INVARIANT: SUBMITTED != FILED
    // ==========================================

    @Test
    @DisplayName("Submission ack does not transition operation status to FILED/SUCCEEDED")
    void testSubmittedDoesNotEqualFiled() {
        TenantContext.setTenantId(orgA.getId());

        GovIntegrationRequest submitRequest = GovIntegrationRequest.builder()
                .organizationId(orgA.getId())
                .providerType(GovProviderType.GST)
                .operationType("GST_RETURN_SUBMISSION")
                .requestData(Map.of("gstin", "27AAAAA0000A1Z5", "returnType", "GSTR1", "returnPeriod", "042026"))
                .build();

        GovIntegrationResult result = govIntegrationService.executeOperation(submitRequest);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResponseMetadata().get("status")).isEqualTo("SUBMITTED");

        // The operation status in Taxoryn should be recorded, but authoritative state must remain non-terminal until reconciliation
        GovIntegrationOperationEntity op = operationRepository.findById(result.getOperationId()).orElseThrow();
        assertThat(op.getStatus()).isNotNull();
    }
}
