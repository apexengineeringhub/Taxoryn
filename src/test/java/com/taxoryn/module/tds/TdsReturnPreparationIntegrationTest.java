package com.taxoryn.module.tds;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
import com.taxoryn.module.gov.model.GovCredentialType;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.tds.dto.TdsPrepareReturnRequest;
import com.taxoryn.module.tds.dto.TdsPreparedReturnDto;
import com.taxoryn.module.tds.dto.TdsReturnTotalsDto;
import com.taxoryn.module.tds.entity.TdsProfileEntity;
import com.taxoryn.module.tds.entity.TdsReturnEntity;
import com.taxoryn.module.tds.integration.TdsGovernmentIntegrationService;
import com.taxoryn.module.tds.repository.TdsProfileRepository;
import com.taxoryn.module.tds.repository.TdsReturnRepository;
import com.taxoryn.module.tds.service.TdsPayloadFingerprintGenerator;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TdsReturnPreparationIntegrationTest {

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
    private TdsProfileRepository tdsProfileRepository;

    @Autowired
    private TdsReturnRepository tdsReturnRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private TdsPayloadFingerprintGenerator fingerprintGenerator;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private AuditService auditService;

    private OrganizationEntity tenant1;
    private OrganizationEntity tenant2;
    private UserEntity userTenant1;
    private RoleEntity adminRole;
    private String jwtTokenTenant1;
    private GovConnectionDto connectionTenant1;
    private GovConnectionDto connectionTenant2;
    private ClientEntity client1;
    private TdsProfileEntity profile1;

    @BeforeEach
    void setUp() {
        TenantContext.clear();

        tenant1 = organizationRepository.save(OrganizationEntity.builder()
                .name("TDS Prep Corp Org 1 " + UUID.randomUUID())
                .email("tds.prep.org1." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        tenant2 = organizationRepository.save(OrganizationEntity.builder()
                .name("TDS Prep Corp Org 2 " + UUID.randomUUID())
                .email("tds.prep.org2." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(tenant1.getId());

        adminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN_" + UUID.randomUUID().toString().substring(0, 8))
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        userTenant1 = userRepository.save(UserEntity.builder()
                .organizationId(tenant1.getId())
                .email("tds-prep-admin-" + UUID.randomUUID() + "@taxoryn.com")
                .passwordHash(passwordEncoder.encode("SecurePass123!"))
                .firstName("TDS")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());

        jwtTokenTenant1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                userTenant1.getId(),
                tenant1.getId(),
                userTenant1.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("TDS_VIEW", "TDS_CREATE", "TDS_UPDATE", "TDS_WRITE", "TDS_READ")
        );

        client1 = clientRepository.save(ClientEntity.builder()
                .clientCode("CL-TDS-PREP-" + UUID.randomUUID().toString().substring(0, 6))
                .displayName("TDS Alpha Corp")
                .legalName("TDS Alpha Corp Private Limited")
                .clientType(ClientEntity.ClientType.COMPANY)
                .pan("AAACA1234C")
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build());

        profile1 = tdsProfileRepository.save(TdsProfileEntity.builder()
                .clientId(client1.getId())
                .tan("MUMB12345A")
                .deductorType(TdsProfileEntity.DeductorType.COMPANY)
                .responsiblePersonName("Rajesh Sharma")
                .responsiblePersonPan("AAACA1234C")
                .status(TdsProfileEntity.TdsProfileStatus.ACTIVE)
                .build());

        connectionTenant1 = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.TDS)
                .displayName("Tenant 1 TRACES Connection")
                .build());

        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(connectionTenant1.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("traces_key_1***")
                .rawSecret("TracesSecret123!")
                .build());

        govConnectionService.activateConnection(connectionTenant1.getId());

        TenantContext.setTenantId(tenant2.getId());

        connectionTenant2 = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.TDS)
                .displayName("Tenant 2 TRACES Connection")
                .build());

        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(connectionTenant2.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("traces_key_2***")
                .rawSecret("TracesSecret456!")
                .build());

        govConnectionService.activateConnection(connectionTenant2.getId());

        TenantContext.setTenantId(tenant1.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Form 24Q (Salaries) return preparation generates valid payload and READY_TO_FILE status")
    void prepareReturn_Form24Q_Success() {
        TdsReturnTotalsDto totals = TdsReturnTotalsDto.builder()
                .totalAmountPaid(new BigDecimal("1200000.00"))
                .totalTaxDeducted(new BigDecimal("120000.00"))
                .totalTaxDeposited(new BigDecimal("120000.00"))
                .totalChallanAmount(new BigDecimal("120000.00"))
                .totalDeducteeCount(10L)
                .totalChallanCount(1L)
                .build();

        TdsPrepareReturnRequest request = TdsPrepareReturnRequest.builder()
                .tan("MUMB12345A")
                .formType("FORM_24Q")
                .financialYear("2025-26")
                .quarter("Q1")
                .totals(totals)
                .profileId(profile1.getId())
                .clientId(client1.getId())
                .build();

        TdsPreparedReturnDto response = tdsGovIntegrationService.prepareReturn(request);

        assertThat(response).isNotNull();
        assertThat(response.isReadyForSubmission()).isTrue();
        assertThat(response.getStatus()).isEqualTo("READY_TO_FILE");
        assertThat(response.getFormType()).isEqualTo("FORM_24Q");
        assertThat(response.getFinancialYear()).isEqualTo("2025-26");
        assertThat(response.getQuarter()).isEqualTo("Q1");
        assertThat(response.getAssessmentYear()).isEqualTo("2026-27");
        assertThat(response.getPayloadFingerprint()).isNotBlank();
        assertThat(response.getValidationResult().isValid()).isTrue();
        assertThat(response.getProviderReferenceId()).startsWith("TRACES-TDS_RETURN_PREPARATION-ACK-");
    }

    @Test
    @DisplayName("Form 26Q (Resident Non-Salary) return preparation succeeds with deductee records")
    void prepareReturn_Form26Q_Success() {
        TdsReturnTotalsDto totals = TdsReturnTotalsDto.builder()
                .totalAmountPaid(new BigDecimal("500000.00"))
                .totalTaxDeducted(new BigDecimal("50000.00"))
                .totalTaxDeposited(new BigDecimal("50000.00"))
                .totalChallanAmount(new BigDecimal("50000.00"))
                .totalDeducteeCount(2L)
                .totalChallanCount(1L)
                .build();

        List<Map<String, Object>> challans = List.of(
                Map.of("bsrCode", "0210001", "challanNo", "10023", "depositDate", "2025-07-05", "taxAmount", 50000.00)
        );

        List<Map<String, Object>> deductees = List.of(
                Map.of("pan", "ABCDE1234F", "name", "John Contractor", "section", "194C", "amountPaid", 200000.00, "tdsDeducted", 20000.00),
                Map.of("pan", "FGHIJ5678K", "name", "Professional Services", "section", "194J", "amountPaid", 300000.00, "tdsDeducted", 30000.00)
        );

        TdsPrepareReturnRequest request = TdsPrepareReturnRequest.builder()
                .tan("MUMB12345A")
                .formType("FORM_26Q")
                .financialYear("2025-26")
                .quarter("Q2")
                .totals(totals)
                .challans(challans)
                .deductees(deductees)
                .build();

        TdsPreparedReturnDto response = tdsGovIntegrationService.prepareReturn(request);

        assertThat(response).isNotNull();
        assertThat(response.isReadyForSubmission()).isTrue();
        assertThat(response.getStatus()).isEqualTo("READY_TO_FILE");
        assertThat(response.getPayload().getChallans()).hasSize(1);
        assertThat(response.getPayload().getDeductees()).hasSize(2);
        assertThat(response.getPayloadFingerprint()).hasSize(64);
    }

    @Test
    @DisplayName("Form 27Q (Non-Resident) and Form 27EQ (TCS) return preparations succeed")
    void prepareReturn_Form27Q_And_Form27EQ_Success() {
        TdsReturnTotalsDto totals = TdsReturnTotalsDto.builder()
                .totalAmountPaid(new BigDecimal("800000.00"))
                .totalTaxDeducted(new BigDecimal("80000.00"))
                .totalTaxDeposited(new BigDecimal("80000.00"))
                .totalChallanAmount(new BigDecimal("80000.00"))
                .build();

        // 27Q
        TdsPreparedReturnDto res27Q = tdsGovIntegrationService.prepareReturn(TdsPrepareReturnRequest.builder()
                .tan("MUMB12345A")
                .formType("FORM_27Q")
                .financialYear("2025-26")
                .quarter("Q3")
                .totals(totals)
                .build());
        assertThat(res27Q.isReadyForSubmission()).isTrue();
        assertThat(res27Q.getFormType()).isEqualTo("FORM_27Q");

        // 27EQ
        TdsPreparedReturnDto res27EQ = tdsGovIntegrationService.prepareReturn(TdsPrepareReturnRequest.builder()
                .tan("MUMB12345A")
                .formType("FORM_27EQ")
                .financialYear("2025-26")
                .quarter("Q4")
                .totals(totals)
                .build());
        assertThat(res27EQ.isReadyForSubmission()).isTrue();
        assertThat(res27EQ.getFormType()).isEqualTo("FORM_27EQ");
    }

    @Test
    @DisplayName("Validation fails when TAN format is malformed")
    void prepareReturn_ValidationFailure_InvalidTan() {
        TdsPrepareReturnRequest request = TdsPrepareReturnRequest.builder()
                .tan("MALFORMED_TAN")
                .formType("FORM_26Q")
                .financialYear("2025-26")
                .quarter("Q1")
                .build();

        TdsPreparedReturnDto response = tdsGovIntegrationService.prepareReturn(request);

        assertThat(response.isReadyForSubmission()).isFalse();
        assertThat(response.getStatus()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getValidationResult().isValid()).isFalse();
        assertThat(response.getValidationResult().getErrors()).anyMatch(e -> e.contains("Invalid TAN format"));
    }

    @Test
    @DisplayName("Validation fails when Financial Year span is non-consecutive")
    void prepareReturn_ValidationFailure_InvalidFY() {
        TdsPrepareReturnRequest request = TdsPrepareReturnRequest.builder()
                .tan("MUMB12345A")
                .formType("FORM_26Q")
                .financialYear("2025-28")
                .quarter("Q1")
                .build();

        TdsPreparedReturnDto response = tdsGovIntegrationService.prepareReturn(request);

        assertThat(response.isReadyForSubmission()).isFalse();
        assertThat(response.getStatus()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getValidationResult().getErrors()).anyMatch(e -> e.contains("Invalid Financial Year span"));
    }

    @Test
    @DisplayName("Validation fails when Quarter is invalid")
    void prepareReturn_ValidationFailure_InvalidQuarter() {
        TdsPrepareReturnRequest request = TdsPrepareReturnRequest.builder()
                .tan("MUMB12345A")
                .formType("FORM_26Q")
                .financialYear("2025-26")
                .quarter("Q5")
                .build();

        TdsPreparedReturnDto response = tdsGovIntegrationService.prepareReturn(request);

        assertThat(response.isReadyForSubmission()).isFalse();
        assertThat(response.getStatus()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getValidationResult().getErrors()).anyMatch(e -> e.contains("Invalid quarter"));
    }

    @Test
    @DisplayName("Validation fails when form type is unsupported")
    void prepareReturn_ValidationFailure_UnsupportedFormType() {
        TdsPrepareReturnRequest request = TdsPrepareReturnRequest.builder()
                .tan("MUMB12345A")
                .formType("FORM_UNKNOWN_99")
                .financialYear("2025-26")
                .quarter("Q1")
                .build();

        TdsPreparedReturnDto response = tdsGovIntegrationService.prepareReturn(request);

        assertThat(response.isReadyForSubmission()).isFalse();
        assertThat(response.getStatus()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getValidationResult().getErrors()).anyMatch(e -> e.contains("Unsupported TDS return form type"));
    }

    @Test
    @DisplayName("Validation fails when financial totals contain negative amounts")
    void prepareReturn_ValidationFailure_NegativeAmounts() {
        TdsReturnTotalsDto totals = TdsReturnTotalsDto.builder()
                .totalAmountPaid(new BigDecimal("-500.00"))
                .totalTaxDeducted(new BigDecimal("500.00"))
                .build();

        TdsPrepareReturnRequest request = TdsPrepareReturnRequest.builder()
                .tan("MUMB12345A")
                .formType("FORM_26Q")
                .financialYear("2025-26")
                .quarter("Q1")
                .totals(totals)
                .build();

        TdsPreparedReturnDto response = tdsGovIntegrationService.prepareReturn(request);

        assertThat(response.isReadyForSubmission()).isFalse();
        assertThat(response.getStatus()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getValidationResult().getErrors()).anyMatch(e -> e.contains("cannot be negative"));
    }

    @Test
    @DisplayName("Validation fails when tax deducted exceeds tax deposited (Short payment detected)")
    void prepareReturn_ValidationFailure_ShortPayment() {
        TdsReturnTotalsDto totals = TdsReturnTotalsDto.builder()
                .totalTaxDeducted(new BigDecimal("100000.00"))
                .totalTaxDeposited(new BigDecimal("70000.00"))
                .build();

        TdsPrepareReturnRequest request = TdsPrepareReturnRequest.builder()
                .tan("MUMB12345A")
                .formType("FORM_26Q")
                .financialYear("2025-26")
                .quarter("Q1")
                .totals(totals)
                .build();

        TdsPreparedReturnDto response = tdsGovIntegrationService.prepareReturn(request);

        assertThat(response.isReadyForSubmission()).isFalse();
        assertThat(response.getStatus()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getValidationResult().getErrors()).anyMatch(e -> e.contains("Short payment detected"));
    }

    @Test
    @DisplayName("Deterministic SHA-256 fingerprinting produces identical hash for identical payloads")
    void fingerprintDeterminism_IdenticalPayloads() {
        TdsReturnTotalsDto totals = TdsReturnTotalsDto.builder()
                .totalAmountPaid(new BigDecimal("100000.00"))
                .totalTaxDeducted(new BigDecimal("10000.00"))
                .totalTaxDeposited(new BigDecimal("10000.00"))
                .build();

        String hash1 = fingerprintGenerator.generateFingerprint(
                "MUMB12345A", "FORM_26Q", "2025-26", "Q1", "2026-27", "COMPANY",
                totals, Collections.emptyList(), Collections.emptyList(), Map.of("key1", "val1")
        );

        String hash2 = fingerprintGenerator.generateFingerprint(
                "MUMB12345A", "FORM_26Q", "2025-26", "Q1", "2026-27", "COMPANY",
                totals, Collections.emptyList(), Collections.emptyList(), Map.of("key1", "val1")
        );

        assertThat(hash1).isNotNull();
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("Deterministic SHA-256 fingerprinting produces different hash for mutated inputs")
    void fingerprintDeterminism_MutatedPayloads() {
        TdsReturnTotalsDto totals1 = TdsReturnTotalsDto.builder()
                .totalAmountPaid(new BigDecimal("100000.00"))
                .totalTaxDeducted(new BigDecimal("10000.00"))
                .build();

        TdsReturnTotalsDto totals2 = TdsReturnTotalsDto.builder()
                .totalAmountPaid(new BigDecimal("200000.00"))
                .totalTaxDeducted(new BigDecimal("20000.00"))
                .build();

        String hash1 = fingerprintGenerator.generateFingerprint(
                "MUMB12345A", "FORM_26Q", "2025-26", "Q1", "2026-27", "COMPANY",
                totals1, Collections.emptyList(), Collections.emptyList(), Collections.emptyMap()
        );

        String hash2 = fingerprintGenerator.generateFingerprint(
                "MUMB12345A", "FORM_26Q", "2025-26", "Q1", "2026-27", "COMPANY",
                totals2, Collections.emptyList(), Collections.emptyList(), Collections.emptyMap()
        );

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("Re-preparation updates existing draft return entity to READY_TO_FILE and refreshes fingerprint")
    void rePreparation_UpdatesReturnEntityLifecycle() {
        TdsReturnEntity draftReturn = tdsReturnRepository.save(TdsReturnEntity.builder()
                .clientId(client1.getId())
                .tdsProfileId(profile1.getId())
                .formType(TdsReturnEntity.TdsFormType.FORM_26Q)
                .quarter(TdsReturnEntity.TdsQuarter.Q1)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .filingStatus(TdsReturnEntity.TdsFilingStatus.DRAFT)
                .build());

        TdsReturnTotalsDto totals = TdsReturnTotalsDto.builder()
                .totalAmountPaid(new BigDecimal("300000.00"))
                .totalTaxDeducted(new BigDecimal("30000.00"))
                .totalTaxDeposited(new BigDecimal("30000.00"))
                .build();

        TdsPrepareReturnRequest request = TdsPrepareReturnRequest.builder()
                .returnId(draftReturn.getId())
                .tan("MUMB12345A")
                .formType("FORM_26Q")
                .financialYear("2025-26")
                .quarter("Q1")
                .totals(totals)
                .build();

        TdsPreparedReturnDto prepared = tdsGovIntegrationService.prepareReturn(request);

        assertThat(prepared.isReadyForSubmission()).isTrue();
        assertThat(prepared.getStatus()).isEqualTo("READY_TO_FILE");

        TdsReturnEntity updated = tdsReturnRepository.findById(draftReturn.getId()).orElseThrow();
        assertThat(updated.getFilingStatus()).isEqualTo(TdsReturnEntity.TdsFilingStatus.READY_TO_FILE);
        assertThat(updated.getFvuValidationStatus()).isEqualTo(TdsReturnEntity.FvuValidationStatus.VALIDATED);
        assertThat(updated.getTotalAmountPaid()).isEqualByComparingTo(new BigDecimal("300000.00"));
    }

    @Test
    @DisplayName("Mock directives simulate AUTH_REQUIRED, PROVIDER_UNAVAILABLE, TIMEOUT, and RATE_LIMITED")
    void prepareReturn_MockDirectives() {
        TdsReturnTotalsDto totals = TdsReturnTotalsDto.builder()
                .totalTaxDeducted(new BigDecimal("10000.00"))
                .totalTaxDeposited(new BigDecimal("10000.00"))
                .build();

        // AUTH_REQUIRED
        TdsPreparedReturnDto authRes = tdsGovIntegrationService.prepareReturn(TdsPrepareReturnRequest.builder()
                .tan("MUMB12345A")
                .formType("FORM_26Q")
                .financialYear("2025-26")
                .quarter("Q1")
                .totals(totals)
                .options(Map.of("mockOutcome", "AUTH_REQUIRED"))
                .build());
        assertThat(authRes.isReadyForSubmission()).isFalse();
        assertThat(authRes.getErrorCode()).isEqualTo("AUTH_REQUIRED");

        // PROVIDER_UNAVAILABLE
        TdsPreparedReturnDto unavailRes = tdsGovIntegrationService.prepareReturn(TdsPrepareReturnRequest.builder()
                .tan("MUMB12345A")
                .formType("FORM_26Q")
                .financialYear("2025-26")
                .quarter("Q1")
                .totals(totals)
                .options(Map.of("mockOutcome", "PROVIDER_UNAVAILABLE"))
                .build());
        assertThat(unavailRes.isReadyForSubmission()).isFalse();
        assertThat(unavailRes.getErrorCode()).isEqualTo("PROVIDER_UNAVAILABLE");

        // TIMEOUT
        TdsPreparedReturnDto timeoutRes = tdsGovIntegrationService.prepareReturn(TdsPrepareReturnRequest.builder()
                .tan("MUMB12345A")
                .formType("FORM_26Q")
                .financialYear("2025-26")
                .quarter("Q1")
                .totals(totals)
                .options(Map.of("mockOutcome", "TIMEOUT"))
                .build());
        assertThat(timeoutRes.isReadyForSubmission()).isFalse();
        assertThat(timeoutRes.getErrorCode()).isEqualTo("TIMEOUT");

        // RATE_LIMITED
        TdsPreparedReturnDto rateLimitRes = tdsGovIntegrationService.prepareReturn(TdsPrepareReturnRequest.builder()
                .tan("MUMB12345A")
                .formType("FORM_26Q")
                .financialYear("2025-26")
                .quarter("Q1")
                .totals(totals)
                .options(Map.of("mockOutcome", "RATE_LIMITED"))
                .build());
        assertThat(rateLimitRes.isReadyForSubmission()).isFalse();
        assertThat(rateLimitRes.getErrorCode()).isEqualTo("RATE_LIMITED");
    }

    @Test
    @DisplayName("Tenant isolation prevents cross-tenant connection usage during preparation")
    void prepareReturn_TenantIsolation() {
        TdsPrepareReturnRequest request = TdsPrepareReturnRequest.builder()
                .tan("MUMB12345A")
                .formType("FORM_26Q")
                .financialYear("2025-26")
                .quarter("Q1")
                .connectionId(connectionTenant2.getId())
                .build();

        // Tenant 1 attempting to use Tenant 2's connection ID must fail
        assertThatThrownBy(() -> tdsGovIntegrationService.prepareReturn(request))
                .isInstanceOf(com.taxoryn.module.gov.exception.GovConnectionNotFoundException.class);
    }

    @Test
    @DisplayName("Controller endpoint POST /api/v1/tds/returns/prepare works with RBAC and valid payload")
    void controller_PrepareReturn_Success() throws Exception {
        TdsReturnTotalsDto totals = TdsReturnTotalsDto.builder()
                .totalAmountPaid(new BigDecimal("450000.00"))
                .totalTaxDeducted(new BigDecimal("45000.00"))
                .totalTaxDeposited(new BigDecimal("45000.00"))
                .totalChallanAmount(new BigDecimal("45000.00"))
                .build();

        TdsPrepareReturnRequest request = TdsPrepareReturnRequest.builder()
                .tan("MUMB12345A")
                .formType("FORM_26Q")
                .financialYear("2025-26")
                .quarter("Q1")
                .totals(totals)
                .build();

        mockMvc.perform(post("/api/v1/tds/returns/prepare")
                        .header("Authorization", jwtTokenTenant1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("READY_TO_FILE"))
                .andExpect(jsonPath("$.data.formType").value("FORM_26Q"))
                .andExpect(jsonPath("$.data.readyForSubmission").value(true))
                .andExpect(jsonPath("$.data.payloadFingerprint").isNotEmpty());
    }

    @Test
    @DisplayName("Controller endpoint POST /api/v1/tds/returns/prepare is unauthorized without token")
    void controller_PrepareReturn_Unauthorized() throws Exception {
        TdsPrepareReturnRequest request = TdsPrepareReturnRequest.builder()
                .tan("MUMB12345A")
                .formType("FORM_26Q")
                .financialYear("2025-26")
                .quarter("Q1")
                .build();

        mockMvc.perform(post("/api/v1/tds/returns/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
