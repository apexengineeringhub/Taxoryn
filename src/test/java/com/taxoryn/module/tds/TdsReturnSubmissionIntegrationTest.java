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
import com.taxoryn.module.tds.dto.TdsReturnSubmissionResultDto;
import com.taxoryn.module.tds.dto.TdsReturnTotalsDto;
import com.taxoryn.module.tds.dto.TdsSubmitReturnRequest;
import com.taxoryn.module.tds.entity.TdsProfileEntity;
import com.taxoryn.module.tds.entity.TdsReturnEntity;
import com.taxoryn.module.tds.integration.TdsGovernmentIntegrationService;
import com.taxoryn.module.tds.repository.TdsProfileRepository;
import com.taxoryn.module.tds.repository.TdsReturnRepository;
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
public class TdsReturnSubmissionIntegrationTest {

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
                .name("TDS Submit Corp Org 1 " + UUID.randomUUID())
                .email("tds.submit.org1." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        tenant2 = organizationRepository.save(OrganizationEntity.builder()
                .name("TDS Submit Corp Org 2 " + UUID.randomUUID())
                .email("tds.submit.org2." + UUID.randomUUID() + "@taxoryn.com")
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
                .email("tds-submit-admin-" + UUID.randomUUID() + "@taxoryn.com")
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
                .clientCode("CL-TDS-SUB-" + UUID.randomUUID().toString().substring(0, 6))
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
    @DisplayName("Submitting prepared return in READY_TO_FILE transitions to SUBMITTED and captures ACK")
    void submitReturn_ReadyToFile_Success() {
        // 1. Prepare return
        TdsReturnTotalsDto totals = TdsReturnTotalsDto.builder()
                .totalAmountPaid(new BigDecimal("500000.00"))
                .totalTaxDeducted(new BigDecimal("50000.00"))
                .totalTaxDeposited(new BigDecimal("50000.00"))
                .totalChallanAmount(new BigDecimal("50000.00"))
                .build();

        TdsPreparedReturnDto prepared = tdsGovIntegrationService.prepareReturn(TdsPrepareReturnRequest.builder()
                .tan("MUMB12345A")
                .formType("FORM_26Q")
                .financialYear("2025-26")
                .quarter("Q1")
                .totals(totals)
                .profileId(profile1.getId())
                .clientId(client1.getId())
                .build());

        TdsReturnEntity returnEntity = tdsReturnRepository.save(TdsReturnEntity.builder()
                .clientId(client1.getId())
                .tdsProfileId(profile1.getId())
                .formType(TdsReturnEntity.TdsFormType.FORM_26Q)
                .quarter(TdsReturnEntity.TdsQuarter.Q1)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .filingStatus(TdsReturnEntity.TdsFilingStatus.READY_TO_FILE)
                .build());

        // 2. Submit return
        TdsSubmitReturnRequest submitRequest = TdsSubmitReturnRequest.builder()
                .returnId(returnEntity.getId())
                .tan("MUMB12345A")
                .formType("FORM_26Q")
                .financialYear("2025-26")
                .quarter("Q1")
                .payloadFingerprint(prepared.getPayloadFingerprint())
                .build();

        TdsReturnSubmissionResultDto result = tdsGovIntegrationService.submitReturn(submitRequest);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSubmissionStatus()).isEqualTo("SUBMITTED");
        assertThat(result.getAcknowledgementNumber()).startsWith("TRACES-ACK-MUMB12345A-Q1-202526-");
        assertThat(result.getProviderReference()).startsWith("TRACES-SUB-");
        assertThat(result.getSubmittedAt()).isNotNull();

        // 3. Verify entity state in DB: SUBMITTED, filingDate remains NULL
        TdsReturnEntity updated = tdsReturnRepository.findById(returnEntity.getId()).orElseThrow();
        assertThat(updated.getFilingStatus()).isEqualTo(TdsReturnEntity.TdsFilingStatus.SUBMITTED);
        assertThat(updated.getReceiptNumber()).isEqualTo(result.getAcknowledgementNumber());
        assertThat(updated.getFilingDate()).isNull(); // CRITICAL INVARIANT: SUBMITTED != FILED
    }

    @Test
    @DisplayName("Form 24Q (Salaries) return submission succeeds")
    void submitReturn_Form24Q_Success() {
        TdsReturnEntity returnEntity = tdsReturnRepository.save(TdsReturnEntity.builder()
                .clientId(client1.getId())
                .tdsProfileId(profile1.getId())
                .formType(TdsReturnEntity.TdsFormType.FORM_24Q)
                .quarter(TdsReturnEntity.TdsQuarter.Q2)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .filingStatus(TdsReturnEntity.TdsFilingStatus.READY_TO_FILE)
                .build());

        TdsSubmitReturnRequest request = TdsSubmitReturnRequest.builder()
                .returnId(returnEntity.getId())
                .tan("MUMB12345A")
                .formType("FORM_24Q")
                .financialYear("2025-26")
                .quarter("Q2")
                .build();

        TdsReturnSubmissionResultDto result = tdsGovIntegrationService.submitReturn(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSubmissionStatus()).isEqualTo("SUBMITTED");
        assertThat(result.getFormType()).isEqualTo("FORM_24Q");
    }

    @Test
    @DisplayName("Form 27Q (Non-Resident) and Form 27EQ (TCS) return submissions succeed")
    void submitReturn_Form27Q_And_Form27EQ_Success() {
        // 27Q
        TdsReturnEntity ret27Q = tdsReturnRepository.save(TdsReturnEntity.builder()
                .clientId(client1.getId())
                .tdsProfileId(profile1.getId())
                .formType(TdsReturnEntity.TdsFormType.FORM_27Q)
                .quarter(TdsReturnEntity.TdsQuarter.Q3)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .filingStatus(TdsReturnEntity.TdsFilingStatus.READY_TO_FILE)
                .build());

        TdsReturnSubmissionResultDto res27Q = tdsGovIntegrationService.submitReturn(TdsSubmitReturnRequest.builder()
                .returnId(ret27Q.getId())
                .tan("MUMB12345A")
                .formType("FORM_27Q")
                .financialYear("2025-26")
                .quarter("Q3")
                .build());
        assertThat(res27Q.isSuccess()).isTrue();
        assertThat(res27Q.getFormType()).isEqualTo("FORM_27Q");

        // 27EQ
        TdsReturnEntity ret27EQ = tdsReturnRepository.save(TdsReturnEntity.builder()
                .clientId(client1.getId())
                .tdsProfileId(profile1.getId())
                .formType(TdsReturnEntity.TdsFormType.FORM_27EQ)
                .quarter(TdsReturnEntity.TdsQuarter.Q4)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .filingStatus(TdsReturnEntity.TdsFilingStatus.READY_TO_FILE)
                .build());

        TdsReturnSubmissionResultDto res27EQ = tdsGovIntegrationService.submitReturn(TdsSubmitReturnRequest.builder()
                .returnId(ret27EQ.getId())
                .tan("MUMB12345A")
                .formType("FORM_27EQ")
                .financialYear("2025-26")
                .quarter("Q4")
                .build());
        assertThat(res27EQ.isSuccess()).isTrue();
        assertThat(res27EQ.getFormType()).isEqualTo("FORM_27EQ");
    }

    @Test
    @DisplayName("Fingerprint verification rejects tampered expectedFingerprint")
    void submitReturn_MismatchedFingerprint_Rejected() {
        TdsReturnEntity returnEntity = tdsReturnRepository.save(TdsReturnEntity.builder()
                .clientId(client1.getId())
                .tdsProfileId(profile1.getId())
                .formType(TdsReturnEntity.TdsFormType.FORM_26Q)
                .quarter(TdsReturnEntity.TdsQuarter.Q1)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .filingStatus(TdsReturnEntity.TdsFilingStatus.READY_TO_FILE)
                .build());

        TdsSubmitReturnRequest request = TdsSubmitReturnRequest.builder()
                .returnId(returnEntity.getId())
                .tan("MUMB12345A")
                .formType("FORM_26Q")
                .financialYear("2025-26")
                .quarter("Q1")
                .expectedFingerprint("TAMPERED_HASH_1234567890ABCDEF")
                .build();

        assertThatThrownBy(() -> tdsGovIntegrationService.submitReturn(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Prepared return payload fingerprint mismatch");
    }

    @Test
    @DisplayName("Submission of return in DRAFT status is rejected")
    void submitReturn_DraftStatus_Rejected() {
        TdsReturnEntity returnEntity = tdsReturnRepository.save(TdsReturnEntity.builder()
                .clientId(client1.getId())
                .tdsProfileId(profile1.getId())
                .formType(TdsReturnEntity.TdsFormType.FORM_26Q)
                .quarter(TdsReturnEntity.TdsQuarter.Q1)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .filingStatus(TdsReturnEntity.TdsFilingStatus.DRAFT)
                .build());

        TdsSubmitReturnRequest request = TdsSubmitReturnRequest.builder()
                .returnId(returnEntity.getId())
                .tan("MUMB12345A")
                .formType("FORM_26Q")
                .financialYear("2025-26")
                .quarter("Q1")
                .build();

        assertThatThrownBy(() -> tdsGovIntegrationService.submitReturn(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Return must be prepared and in READY_TO_FILE status");
    }

    @Test
    @DisplayName("Submission of already FILED return is rejected (terminal and immutable)")
    void submitReturn_FiledStatus_Rejected() {
        TdsReturnEntity returnEntity = tdsReturnRepository.save(TdsReturnEntity.builder()
                .clientId(client1.getId())
                .tdsProfileId(profile1.getId())
                .formType(TdsReturnEntity.TdsFormType.FORM_26Q)
                .quarter(TdsReturnEntity.TdsQuarter.Q1)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .filingStatus(TdsReturnEntity.TdsFilingStatus.FILED)
                .receiptNumber("TRACES-ACK-EXISTING-999")
                .build());

        TdsSubmitReturnRequest request = TdsSubmitReturnRequest.builder()
                .returnId(returnEntity.getId())
                .tan("MUMB12345A")
                .formType("FORM_26Q")
                .financialYear("2025-26")
                .quarter("Q1")
                .build();

        assertThatThrownBy(() -> tdsGovIntegrationService.submitReturn(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Cannot submit return in status 'FILED'");
    }

    @Test
    @DisplayName("Repeated submission of already SUBMITTED return returns existing ACK idempotently")
    void submitReturn_RepeatedSubmission_ReturnsExistingResult() {
        TdsReturnEntity returnEntity = tdsReturnRepository.save(TdsReturnEntity.builder()
                .clientId(client1.getId())
                .tdsProfileId(profile1.getId())
                .formType(TdsReturnEntity.TdsFormType.FORM_26Q)
                .quarter(TdsReturnEntity.TdsQuarter.Q1)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .filingStatus(TdsReturnEntity.TdsFilingStatus.SUBMITTED)
                .receiptNumber("TRACES-ACK-MUMB12345A-Q1-202526-ALREADY")
                .build());

        TdsSubmitReturnRequest request = TdsSubmitReturnRequest.builder()
                .returnId(returnEntity.getId())
                .tan("MUMB12345A")
                .formType("FORM_26Q")
                .financialYear("2025-26")
                .quarter("Q1")
                .build();

        TdsReturnSubmissionResultDto result = tdsGovIntegrationService.submitReturn(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSubmissionStatus()).isEqualTo("SUBMITTED");
        assertThat(result.getAcknowledgementNumber()).isEqualTo("TRACES-ACK-MUMB12345A-Q1-202526-ALREADY");
    }

    @Test
    @DisplayName("Provider DUPLICATE_SUBMISSION response is normalized correctly")
    void submitReturn_ProviderDuplicateSubmission_Normalized() {
        TdsReturnEntity returnEntity = tdsReturnRepository.save(TdsReturnEntity.builder()
                .clientId(client1.getId())
                .tdsProfileId(profile1.getId())
                .formType(TdsReturnEntity.TdsFormType.FORM_26Q)
                .quarter(TdsReturnEntity.TdsQuarter.Q1)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .filingStatus(TdsReturnEntity.TdsFilingStatus.READY_TO_FILE)
                .build());

        TdsSubmitReturnRequest request = TdsSubmitReturnRequest.builder()
                .returnId(returnEntity.getId())
                .tan("MUMB12345A")
                .formType("FORM_26Q")
                .financialYear("2025-26")
                .quarter("Q1")
                .options(Map.of("mockOutcome", "DUPLICATE_SUBMISSION"))
                .build();

        TdsReturnSubmissionResultDto result = tdsGovIntegrationService.submitReturn(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getSubmissionStatus()).isEqualTo("DUPLICATE_SUBMISSION");
        assertThat(result.getErrorCode()).isEqualTo("DUPLICATE_SUBMISSION");
        assertThat(result.getAcknowledgementNumber()).startsWith("TRACES-ACK-DUP-");
    }

    @Test
    @DisplayName("Mock failure directives restore return status to READY_TO_FILE and do not mark SUBMITTED")
    void submitReturn_MockFailures_PreservesReadyToFile() {
        TdsReturnEntity returnEntity = tdsReturnRepository.save(TdsReturnEntity.builder()
                .clientId(client1.getId())
                .tdsProfileId(profile1.getId())
                .formType(TdsReturnEntity.TdsFormType.FORM_26Q)
                .quarter(TdsReturnEntity.TdsQuarter.Q1)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .filingStatus(TdsReturnEntity.TdsFilingStatus.READY_TO_FILE)
                .build());

        // 1. AUTH_REQUIRED
        TdsReturnSubmissionResultDto authRes = tdsGovIntegrationService.submitReturn(TdsSubmitReturnRequest.builder()
                .returnId(returnEntity.getId())
                .tan("MUMB12345A")
                .options(Map.of("mockOutcome", "AUTH_REQUIRED"))
                .build());
        assertThat(authRes.isSuccess()).isFalse();
        assertThat(authRes.getErrorCode()).isEqualTo("AUTH_REQUIRED");
        assertThat(tdsReturnRepository.findById(returnEntity.getId()).orElseThrow().getFilingStatus())
                .isEqualTo(TdsReturnEntity.TdsFilingStatus.READY_TO_FILE);

        // 2. PROVIDER_UNAVAILABLE
        TdsReturnSubmissionResultDto unavailRes = tdsGovIntegrationService.submitReturn(TdsSubmitReturnRequest.builder()
                .returnId(returnEntity.getId())
                .tan("MUMB12345A")
                .options(Map.of("mockOutcome", "PROVIDER_UNAVAILABLE"))
                .build());
        assertThat(unavailRes.isSuccess()).isFalse();
        assertThat(unavailRes.getErrorCode()).isEqualTo("PROVIDER_UNAVAILABLE");
        assertThat(tdsReturnRepository.findById(returnEntity.getId()).orElseThrow().getFilingStatus())
                .isEqualTo(TdsReturnEntity.TdsFilingStatus.READY_TO_FILE);

        // 3. TIMEOUT
        TdsReturnSubmissionResultDto timeoutRes = tdsGovIntegrationService.submitReturn(TdsSubmitReturnRequest.builder()
                .returnId(returnEntity.getId())
                .tan("MUMB12345A")
                .options(Map.of("mockOutcome", "TIMEOUT"))
                .build());
        assertThat(timeoutRes.isSuccess()).isFalse();
        assertThat(timeoutRes.getErrorCode()).isEqualTo("TIMEOUT");

        // 4. RATE_LIMITED
        TdsReturnSubmissionResultDto rateLimitRes = tdsGovIntegrationService.submitReturn(TdsSubmitReturnRequest.builder()
                .returnId(returnEntity.getId())
                .tan("MUMB12345A")
                .options(Map.of("mockOutcome", "RATE_LIMITED"))
                .build());
        assertThat(rateLimitRes.isSuccess()).isFalse();
        assertThat(rateLimitRes.getErrorCode()).isEqualTo("RATE_LIMITED");
    }

    @Test
    @DisplayName("Tenant isolation prevents submitting another tenant's return")
    void submitReturn_CrossTenantReturn_Rejected() {
        // Return belongs to Tenant 2
        TenantContext.setTenantId(tenant2.getId());
        TdsReturnEntity tenant2Return = tdsReturnRepository.save(TdsReturnEntity.builder()
                .clientId(UUID.randomUUID())
                .tdsProfileId(UUID.randomUUID())
                .formType(TdsReturnEntity.TdsFormType.FORM_26Q)
                .quarter(TdsReturnEntity.TdsQuarter.Q1)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .filingStatus(TdsReturnEntity.TdsFilingStatus.READY_TO_FILE)
                .build());

        // Switch to Tenant 1
        TenantContext.setTenantId(tenant1.getId());

        TdsSubmitReturnRequest request = TdsSubmitReturnRequest.builder()
                .returnId(tenant2Return.getId())
                .tan("MUMB12345A")
                .build();

        assertThatThrownBy(() -> tdsGovIntegrationService.submitReturn(request))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("Controller endpoint POST /api/v1/tds/returns/{id}/submit works with RBAC and returns submission result")
    void controller_SubmitReturnById_Success() throws Exception {
        TdsReturnEntity returnEntity = tdsReturnRepository.save(TdsReturnEntity.builder()
                .clientId(client1.getId())
                .tdsProfileId(profile1.getId())
                .formType(TdsReturnEntity.TdsFormType.FORM_26Q)
                .quarter(TdsReturnEntity.TdsQuarter.Q1)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .filingStatus(TdsReturnEntity.TdsFilingStatus.READY_TO_FILE)
                .build());

        TdsSubmitReturnRequest request = TdsSubmitReturnRequest.builder()
                .tan("MUMB12345A")
                .formType("FORM_26Q")
                .financialYear("2025-26")
                .quarter("Q1")
                .build();

        mockMvc.perform(post("/api/v1/tds/returns/" + returnEntity.getId() + "/submit")
                        .header("Authorization", jwtTokenTenant1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.submissionStatus").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.acknowledgementNumber").isNotEmpty());
    }

    @Test
    @DisplayName("Controller endpoint POST /api/v1/tds/returns/submit works directly")
    void controller_SubmitReturnDirect_Success() throws Exception {
        TdsReturnEntity returnEntity = tdsReturnRepository.save(TdsReturnEntity.builder()
                .clientId(client1.getId())
                .tdsProfileId(profile1.getId())
                .formType(TdsReturnEntity.TdsFormType.FORM_26Q)
                .quarter(TdsReturnEntity.TdsQuarter.Q1)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .filingStatus(TdsReturnEntity.TdsFilingStatus.READY_TO_FILE)
                .build());

        TdsSubmitReturnRequest request = TdsSubmitReturnRequest.builder()
                .returnId(returnEntity.getId())
                .tan("MUMB12345A")
                .formType("FORM_26Q")
                .financialYear("2025-26")
                .quarter("Q1")
                .build();

        mockMvc.perform(post("/api/v1/tds/returns/submit")
                        .header("Authorization", jwtTokenTenant1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.submissionStatus").value("SUBMITTED"));
    }

    @Test
    @DisplayName("Controller submission endpoints are unauthorized without token")
    void controller_SubmitReturn_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/tds/returns/" + UUID.randomUUID() + "/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
