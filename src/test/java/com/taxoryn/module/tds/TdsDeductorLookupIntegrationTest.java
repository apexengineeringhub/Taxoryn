package com.taxoryn.module.tds;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
import com.taxoryn.module.gov.exception.GovConnectionNotFoundException;
import com.taxoryn.module.gov.model.GovCredentialType;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.tds.dto.TdsDeductorProfileDto;
import com.taxoryn.module.tds.dto.TdsTanVerificationRequest;
import com.taxoryn.module.tds.integration.TdsGovernmentIntegrationService;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TdsDeductorLookupIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TdsGovernmentIntegrationService tdsGovIntegrationService;

    @Autowired
    private GovernmentConnectionService govConnectionService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AuditService auditService;

    private OrganizationEntity testOrg;
    private GovConnectionDto tdsConn;
    private UserEntity adminUser;
    private String adminToken;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("TDS Deductor Test Org " + UUID.randomUUID())
                .email("tds.deductor." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
        TenantContext.setTenantId(testOrg.getId());

        tdsConn = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.TDS)
                .displayName("TRACES Gateway")
                .build());

        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(tdsConn.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("traces_key_***")
                .rawSecret("SecretTracesApiKey987")
                .build());

        govConnectionService.activateConnection(tdsConn.getId());

        RoleEntity orgAdminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN_" + UUID.randomUUID().toString().substring(0, 8))
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        adminUser = userRepository.save(UserEntity.builder()
                .email("admin." + UUID.randomUUID() + "@taxoryn.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Tds")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build());

        adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser.getId(),
                testOrg.getId(),
                adminUser.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("TDS_VIEW", "TDS_READ", "TDS_WRITE", "TDS_CREATE")
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("lookupDeductor with valid TAN and explicit connection returns full normalized profile")
    void testLookupDeductorSuccess() {
        String testTan = "MUMB12345A";

        TdsDeductorProfileDto profile = tdsGovIntegrationService.lookupDeductor(tdsConn.getId(), testTan, null);

        assertThat(profile).isNotNull();
        assertThat(profile.isValid()).isTrue();
        assertThat(profile.isVerified()).isTrue();
        assertThat(profile.getTan()).isEqualTo(testTan);
        assertThat(profile.getDeductorName()).isEqualTo("Acme Enterprises Private Limited");
        assertThat(profile.getStatus()).isEqualTo("ACTIVE");
        assertThat(profile.getTanStatus()).isEqualTo("VALID");
        assertThat(profile.getTracesStatus()).isEqualTo("REGISTERED_ACTIVE");
        assertThat(profile.getCategory()).isEqualTo("COMPANY");
        assertThat(profile.getPan()).isEqualTo("AAACA1234C");
        assertThat(profile.getState()).isEqualTo("MAHARASHTRA");
        assertThat(profile.getPinCode()).isEqualTo("400001");
        assertThat(profile.getProviderReferenceId()).contains("TRACES-VERIFY_TAN-ACK-");
        assertThat(profile.getOperationId()).isNotNull();
        assertThat(profile.getVerifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("lookupDeductor with valid TAN and auto-resolved connection returns normalized profile")
    void testLookupDeductorWithAutoResolvedConnection() {
        String testTan = "BLRN12345A";

        TdsDeductorProfileDto profile = tdsGovIntegrationService.lookupDeductor(testTan);

        assertThat(profile).isNotNull();
        assertThat(profile.isValid()).isTrue();
        assertThat(profile.isVerified()).isTrue();
        assertThat(profile.getTan()).isEqualTo(testTan);
        assertThat(profile.getDeductorName()).isEqualTo("Bangalore Tech Corp Limited");
        assertThat(profile.getState()).isEqualTo("KARNATAKA");
        assertThat(profile.getPinCode()).isEqualTo("560001");
    }

    @Test
    @DisplayName("lookupDeductor normalizes lowercase TAN to uppercase")
    void testLookupDeductorNormalizesLowercaseTan() {
        String lowercaseTan = "dela98765b";

        TdsDeductorProfileDto profile = tdsGovIntegrationService.lookupDeductor(lowercaseTan);

        assertThat(profile).isNotNull();
        assertThat(profile.isValid()).isTrue();
        assertThat(profile.getTan()).isEqualTo("DELA98765B");
        assertThat(profile.getDeductorName()).isEqualTo("Delhi Consulting Services Private Limited");
        assertThat(profile.getState()).isEqualTo("DELHI");
    }

    @Test
    @DisplayName("lookupDeductor with unknown TAN returns valid=false and NOT_FOUND error")
    void testLookupDeductorUnknownTanReturnsNotFound() {
        String unknownTan = "XXXX99999Z";

        TdsDeductorProfileDto profile = tdsGovIntegrationService.lookupDeductor(unknownTan);

        assertThat(profile).isNotNull();
        assertThat(profile.isValid()).isFalse();
        assertThat(profile.isVerified()).isFalse();
        assertThat(profile.getTan()).isEqualTo(unknownTan);
        assertThat(profile.getErrorCode()).isEqualTo("NOT_FOUND");
        assertThat(profile.getErrorMessage()).contains("not found");
    }

    @Test
    @DisplayName("lookupDeductor with malformed TAN throws AppException VALIDATION_FAILED")
    void testLookupDeductorMalformedTanThrowsValidationException() {
        assertThatThrownBy(() -> tdsGovIntegrationService.lookupDeductor("INVALID_TAN_123"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Invalid TAN format");

        assertThatThrownBy(() -> tdsGovIntegrationService.lookupDeductor("12345ABCDE"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Invalid TAN format");
    }

    @Test
    @DisplayName("lookupDeductor with null or blank TAN throws AppException VALIDATION_FAILED")
    void testLookupDeductorNullOrBlankTanThrowsValidationException() {
        assertThatThrownBy(() -> tdsGovIntegrationService.lookupDeductor(null))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("TAN is required");

        assertThatThrownBy(() -> tdsGovIntegrationService.lookupDeductor("   "))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("TAN is required");
    }

    @Test
    @DisplayName("lookupDeductor with simulated AUTH_REQUIRED directive returns AUTH_REQUIRED error")
    void testLookupDeductorAuthRequiredDirective() {
        TdsDeductorProfileDto profile = tdsGovIntegrationService.lookupDeductor(
                tdsConn.getId(), "MUMB12345A", Map.of("mockOutcome", "AUTH_REQUIRED"));

        assertThat(profile).isNotNull();
        assertThat(profile.isValid()).isFalse();
        assertThat(profile.isVerified()).isFalse();
        assertThat(profile.getErrorCode()).isEqualTo("AUTH_REQUIRED");
        assertThat(profile.getErrorMessage()).contains("session token expired");
    }

    @Test
    @DisplayName("lookupDeductor with simulated PROVIDER_UNAVAILABLE directive returns PROVIDER_UNAVAILABLE error")
    void testLookupDeductorProviderUnavailableDirective() {
        TdsDeductorProfileDto profile = tdsGovIntegrationService.lookupDeductor(
                tdsConn.getId(), "MUMB12345A", Map.of("mockOutcome", "PROVIDER_UNAVAILABLE"));

        assertThat(profile).isNotNull();
        assertThat(profile.isValid()).isFalse();
        assertThat(profile.isVerified()).isFalse();
        assertThat(profile.getErrorCode()).isEqualTo("PROVIDER_UNAVAILABLE");
        assertThat(profile.getErrorMessage()).contains("unavailable");
    }

    @Test
    @DisplayName("lookupDeductor with simulated TIMEOUT directive returns TIMEOUT error")
    void testLookupDeductorTimeoutDirective() {
        TdsDeductorProfileDto profile = tdsGovIntegrationService.lookupDeductor(
                tdsConn.getId(), "MUMB12345A", Map.of("mockOutcome", "TIMEOUT"));

        assertThat(profile).isNotNull();
        assertThat(profile.isValid()).isFalse();
        assertThat(profile.isVerified()).isFalse();
        assertThat(profile.getErrorCode()).isEqualTo("TIMEOUT");
        assertThat(profile.getErrorMessage()).contains("timed out");
    }

    @Test
    @DisplayName("lookupDeductor with simulated RATE_LIMITED directive returns RATE_LIMITED error")
    void testLookupDeductorRateLimitedDirective() {
        TdsDeductorProfileDto profile = tdsGovIntegrationService.lookupDeductor(
                tdsConn.getId(), "MUMB12345A", Map.of("mockOutcome", "RATE_LIMITED"));

        assertThat(profile).isNotNull();
        assertThat(profile.isValid()).isFalse();
        assertThat(profile.isVerified()).isFalse();
        assertThat(profile.getErrorCode()).isEqualTo("RATE_LIMITED");
        assertThat(profile.getErrorMessage()).contains("rate limit exceeded");
    }

    @Test
    @DisplayName("lookupDeductor with non-TDS connection throws VALIDATION_FAILED exception")
    void testLookupDeductorWithNonTdsConnectionThrowsValidationException() {
        GovConnectionDto gstConn = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("GST Gateway")
                .build());

        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(gstConn.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("gst_key_***")
                .rawSecret("SecretGstKey")
                .build());

        govConnectionService.activateConnection(gstConn.getId());

        assertThatThrownBy(() -> tdsGovIntegrationService.lookupDeductor(gstConn.getId(), "MUMB12345A", null))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("is not a TDS provider connection");
    }

    @Test
    @DisplayName("Cross-tenant lookup is blocked when Tenant B attempts to use Tenant A's connection")
    void testCrossTenantLookupIsBlocked() {
        OrganizationEntity orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Tenant Org B " + UUID.randomUUID())
                .email("orgB." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgB.getId());

        assertThatThrownBy(() -> tdsGovIntegrationService.lookupDeductor(tdsConn.getId(), "MUMB12345A", null))
                .isInstanceOf(GovConnectionNotFoundException.class);
    }

    @Test
    @DisplayName("lookupDeductor records audit events with masked TAN and operation metadata")
    void testLookupDeductorAuditEvents() {
        String testTan = "MUMB12345A";

        tdsGovIntegrationService.lookupDeductor(tdsConn.getId(), testTan, null);

        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> metadataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(auditService, atLeastOnce()).logEvent(
                eq(testOrg.getId()),
                any(),
                actionCaptor.capture(),
                any(),
                any(),
                any(),
                metadataCaptor.capture()
        );

        assertThat(actionCaptor.getAllValues()).contains("TDS_TAN_LOOKUP_REQUESTED", "TDS_TAN_LOOKUP_COMPLETED");
        boolean foundMaskedTan = metadataCaptor.getAllValues().stream()
                .anyMatch(m -> "MUMB*****A".equals(m.get("tan")));
        assertThat(foundMaskedTan).isTrue();
    }

    @Test
    @DisplayName("POST /api/v1/tds/deductors/verify-tan returns 200 OK with full deductor profile")
    void testVerifyTanControllerEndpointSuccess() throws Exception {
        TdsTanVerificationRequest request = TdsTanVerificationRequest.builder()
                .tan("MUMB12345A")
                .connectionId(tdsConn.getId())
                .build();

        mockMvc.perform(post("/api/v1/tds/deductors/verify-tan")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tan").value("MUMB12345A"))
                .andExpect(jsonPath("$.data.deductorName").value("Acme Enterprises Private Limited"))
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.verified").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/tds/deductors/verify-tan with malformed TAN returns 400 Bad Request")
    void testVerifyTanControllerEndpointMalformedTan() throws Exception {
        TdsTanVerificationRequest request = TdsTanVerificationRequest.builder()
                .tan("INVALID_TAN")
                .build();

        mockMvc.perform(post("/api/v1/tds/deductors/verify-tan")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/tds/deductors/verify-tan unauthenticated returns 401 Unauthorized")
    void testVerifyTanControllerEndpointUnauthenticated() throws Exception {
        TdsTanVerificationRequest request = TdsTanVerificationRequest.builder()
                .tan("MUMB12345A")
                .build();

        mockMvc.perform(post("/api/v1/tds/deductors/verify-tan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
