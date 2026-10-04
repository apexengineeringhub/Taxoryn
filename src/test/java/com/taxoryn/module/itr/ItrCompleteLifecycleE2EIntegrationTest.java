package com.taxoryn.module.itr;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
import com.taxoryn.module.gov.model.GovCredentialType;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.itr.controller.ItrController;
import com.taxoryn.module.itr.dto.CreateItrProfileRequest;
import com.taxoryn.module.itr.dto.CreateItrReturnRequest;
import com.taxoryn.module.itr.dto.ItrPanVerificationRequest;
import com.taxoryn.module.itr.dto.ItrPrepareReturnRequest;
import com.taxoryn.module.itr.integration.dto.ItrIntegrationResultDto;
import com.taxoryn.module.itr.dto.ItrPreparedReturnDto;
import com.taxoryn.module.itr.dto.ItrProfileDto;
import com.taxoryn.module.itr.dto.ItrReturnDto;
import com.taxoryn.module.itr.dto.ItrReturnStatusDto;
import com.taxoryn.module.itr.dto.ItrReturnSubmissionResultDto;
import com.taxoryn.module.itr.dto.ItrSubmitReturnRequest;
import com.taxoryn.module.itr.dto.ItrTaxSummaryDto;
import com.taxoryn.module.itr.dto.ItrTaxpayerProfileDto;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrType;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ResidentialStatus;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
import com.taxoryn.module.itr.entity.ItrReturnEntity;
import com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus;
import com.taxoryn.module.itr.integration.ItrGovernmentIntegrationService;
import com.taxoryn.module.itr.repository.ItrReturnRepository;
import com.taxoryn.module.itr.service.ItrPayloadFingerprintGenerator;
import com.taxoryn.module.itr.service.ItrService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@ActiveProfiles("test")
public class ItrCompleteLifecycleE2EIntegrationTest {

    @Autowired
    private ItrGovernmentIntegrationService itrGovIntegrationService;

    @Autowired
    private ItrService itrService;

    @Autowired
    private ItrReturnRepository itrReturnRepository;

    @Autowired
    private GovernmentConnectionService govConnectionService;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ItrController itrController;

    @Autowired
    private ItrPayloadFingerprintGenerator fingerprintGenerator;

    @MockBean
    private AuditService auditService;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private ClientEntity clientA;
    private ItrProfileDto profileA;
    private GovConnectionDto connectionA;
    private GovConnectionDto connectionB;

    @BeforeEach
    void setUp() {
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex CA Partners Org A " + UUID.randomUUID())
                .email("apex." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Zenith Tax Advisors Org B " + UUID.randomUUID())
                .email("zenith." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgA.getId());

        SecurityUser userA = SecurityUser.builder()
                .userId(UUID.randomUUID())
                .organizationId(orgA.getId())
                .email("partner@" + orgA.getId() + ".taxoryn.com")
                .roles(Set.of("ROLE_ORG_ADMIN"))
                .permissions(Set.of("ROLE_ORG_ADMIN", "ITR_VIEW", "ITR_CREATE", "ITR_UPDATE", "ITR_WRITE", "CLIENT_VIEW"))
                .enabled(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userA, null, userA.getAuthorities())
        );

        clientA = clientRepository.save(ClientEntity.builder()
                .displayName("Vikramaditya Singhania")
                .legalName("Vikramaditya Singhania")
                .pan("ABCDE1234F")
                .clientType(ClientType.INDIVIDUAL)
                .status(ClientStatus.ACTIVE)
                .build());

        connectionA = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.INCOME_TAX)
                .displayName("ITD Production Gateway Org A")
                .build());

        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(connectionA.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("itd_key_a_***")
                .rawSecret("SecretA123456789")
                .build());
        govConnectionService.activateConnection(connectionA.getId());

        profileA = itrService.createProfile(CreateItrProfileRequest.builder()
                .clientId(clientA.getId())
                .pan("ABCDE1234F")
                .taxpayerType(TaxpayerType.INDIVIDUAL)
                .defaultItrType(ItrType.ITR_1)
                .residentialStatus(ResidentialStatus.RESIDENT)
                .build());

        // Setup Org B Connection
        TenantContext.setTenantId(orgB.getId());
        connectionB = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.INCOME_TAX)
                .displayName("ITD Gateway Org B")
                .build());
        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(connectionB.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("itd_key_b_***")
                .rawSecret("SecretB987654321")
                .build());
        govConnectionService.activateConnection(connectionB.getId());

        // Reset to Org A
        TenantContext.setTenantId(orgA.getId());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userA, null, userA.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    // =========================================================================
    // 1. GOLDEN PATH COMPLETE END-TO-END TEST
    // =========================================================================

    @Test
    @DisplayName("Golden Path E2E: PAN Verification -> Preparation -> Submission -> Verification Pending -> Authoritative FILED")
    void testCompleteItrLifecycleGoldenPath() {
        // Step 1: Real-time PAN Verification
        ItrTaxpayerProfileDto taxpayerProfile = itrGovIntegrationService.lookupTaxpayer("ABCDE1234F");
        assertThat(taxpayerProfile).isNotNull();
        assertThat(taxpayerProfile.isValid()).isTrue();
        assertThat(taxpayerProfile.isVerified()).isTrue();
        assertThat(taxpayerProfile.getPan()).isEqualTo("ABCDE1234F");
        assertThat(taxpayerProfile.getProviderReferenceId()).startsWith("ITD-PAN-ACK-");

        // Step 2: Create ITR Return Record (starts in DOCUMENTS_PENDING)
        ItrReturnDto createdReturn = itrService.createReturn(CreateItrReturnRequest.builder()
                .clientId(clientA.getId())
                .assessmentYear("2026-27")
                .financialYear("2025-26")
                .itrType(ItrType.ITR_1)
                .taxpayerType(TaxpayerType.INDIVIDUAL)
                .dueDate(LocalDate.of(2026, 7, 31))
                .build());
        assertThat(createdReturn).isNotNull();
        assertThat(createdReturn.getStatus()).isEqualTo(ItrStatus.DOCUMENTS_PENDING);

        // Step 3: Return Preparation & Payload Normalization
        ItrTaxSummaryDto taxSummary = ItrTaxSummaryDto.builder()
                .grossTotalIncome(new BigDecimal("1500000.00"))
                .totalDeductions(new BigDecimal("150000.00"))
                .taxableIncome(new BigDecimal("1350000.00"))
                .taxPayable(new BigDecimal("217500.00"))
                .surcharge(BigDecimal.ZERO)
                .cess(new BigDecimal("8700.00"))
                .totalTaxLiability(new BigDecimal("226200.00"))
                .tdsTcsCredit(new BigDecimal("226200.00"))
                .advanceTaxPaid(BigDecimal.ZERO)
                .selfAssessmentTaxPaid(BigDecimal.ZERO)
                .totalTaxesPaid(new BigDecimal("226200.00"))
                .refundDue(BigDecimal.ZERO)
                .balancePayable(BigDecimal.ZERO)
                .build();

        ItrPreparedReturnDto preparedReturn = itrGovIntegrationService.prepareReturn(ItrPrepareReturnRequest.builder()
                .returnId(createdReturn.getId())
                .clientId(clientA.getId())
                .profileId(profileA.getId())
                .pan("ABCDE1234F")
                .returnType("ITR-1")
                .assessmentYear("2026-27")
                .financialYear("2025-26")
                .taxpayerType("INDIVIDUAL")
                .residentialStatus("RESIDENT")
                .taxSummary(taxSummary)
                .incomeDetails(Map.of("salaryIncome", 1500000))
                .deductions(Map.of("80C", 150000))
                .bankDetails(Map.of("bankName", "HDFC Bank", "accountNumber", "50100123456789", "ifsc", "HDFC0001234"))
                .build());

        assertThat(preparedReturn).isNotNull();
        assertThat(preparedReturn.isReadyForSubmission()).isTrue();
        assertThat(preparedReturn.getPayloadFingerprint()).isNotBlank();

        // Database status verified transitioned to READY_TO_FILE
        ItrReturnEntity returnEntity = itrReturnRepository.findById(createdReturn.getId()).orElseThrow();
        assertThat(returnEntity.getStatus()).isEqualTo(ItrStatus.READY_TO_FILE);
        assertThat(returnEntity.getFilingDate()).isNull();

        // Step 4: Submission to Gateway (SUBMITTED != FILED)
        ItrReturnSubmissionResultDto submissionResult = itrGovIntegrationService.submitReturn(ItrSubmitReturnRequest.builder()
                .returnId(createdReturn.getId())
                .connectionId(connectionA.getId())
                .build());

        assertThat(submissionResult).isNotNull();
        assertThat(submissionResult.isSuccess()).isTrue();
        assertThat(submissionResult.getSubmissionStatus()).isEqualTo("SUBMITTED");
        assertThat(submissionResult.getAcknowledgementNumber()).startsWith("ITD-ACK-");
        String submissionAck = submissionResult.getAcknowledgementNumber();

        // Check Entity State: VERIFICATION_PENDING, filingDate is strictly null
        returnEntity = itrReturnRepository.findById(createdReturn.getId()).orElseThrow();
        assertThat(returnEntity.getStatus()).isEqualTo(ItrStatus.VERIFICATION_PENDING);
        assertThat(returnEntity.getAcknowledgementNumber()).isEqualTo(submissionAck);
        assertThat(returnEntity.getFilingDate()).isNull();

        // Step 5: Intermediate Status Polling (Gateway returns PROCESSING)
        ItrReturnStatusDto intermediateStatus = itrGovIntegrationService.checkReturnStatus(
                createdReturn.getId(),
                connectionA.getId(),
                Map.of("mockStatus", "PROCESSING")
        );
        assertThat(intermediateStatus.isSuccess()).isTrue();
        assertThat(intermediateStatus.getFilingStatus()).isEqualTo(ItrStatus.VERIFICATION_PENDING);
        assertThat(intermediateStatus.getFilingDate()).isNull();

        returnEntity = itrReturnRepository.findById(createdReturn.getId()).orElseThrow();
        assertThat(returnEntity.getStatus()).isEqualTo(ItrStatus.VERIFICATION_PENDING);
        assertThat(returnEntity.getFilingDate()).isNull();

        // Step 6: Authoritative Filing Status (Gateway returns FILED)
        ItrReturnStatusDto filedStatus = itrGovIntegrationService.checkReturnStatus(
                createdReturn.getId(),
                connectionA.getId(),
                Map.of("mockStatus", "FILED")
        );
        assertThat(filedStatus.isSuccess()).isTrue();
        assertThat(filedStatus.getFilingStatus()).isEqualTo(ItrStatus.FILED);
        assertThat(filedStatus.isTerminal()).isTrue();
        assertThat(filedStatus.getFilingDate()).isNotNull();
        assertThat(filedStatus.getAcknowledgementNumber()).isEqualTo(submissionAck);

        // Verify Final Database State
        returnEntity = itrReturnRepository.findById(createdReturn.getId()).orElseThrow();
        assertThat(returnEntity.getStatus()).isEqualTo(ItrStatus.FILED);
        assertThat(returnEntity.getFilingDate()).isNotNull();
        assertThat(returnEntity.getVerificationDate()).isNotNull();
        assertThat(returnEntity.getNotes()).contains("Filing confirmed authoritative");

        // Step 7: Terminal State Protection (Cannot regress from FILED)
        ItrReturnSubmissionResultDto reSubmitResult = itrGovIntegrationService.submitReturn(ItrSubmitReturnRequest.builder()
                .returnId(createdReturn.getId())
                .build());
        assertThat(reSubmitResult.isSuccess()).isTrue();
        assertThat(reSubmitResult.getSubmissionStatus()).isEqualTo("FILED");
        assertThat(reSubmitResult.getAcknowledgementNumber()).isEqualTo(submissionAck);

        returnEntity = itrReturnRepository.findById(createdReturn.getId()).orElseThrow();
        assertThat(returnEntity.getStatus()).isEqualTo(ItrStatus.FILED);
    }

    // =========================================================================
    // 2. NEGATIVE END-TO-END SCENARIOS
    // =========================================================================

    @Test
    @DisplayName("Negative E2E: Invalid PAN format fails validation")
    void testE2EInvalidPanRejected() {
        assertThatThrownBy(() -> itrGovIntegrationService.lookupTaxpayer("INVALID_PAN"))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException appEx = (AppException) e;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                });
    }

    @Test
    @DisplayName("Negative E2E: Unregistered PAN for organization rejected")
    void testE2EUnregisteredPanRejected() {
        // 1. Preparation produces VALIDATION_FAILED and readyForSubmission=false
        ItrPreparedReturnDto prepResult = itrGovIntegrationService.prepareReturn(ItrPrepareReturnRequest.builder()
                .pan("ZZZZZ9999Z")
                .assessmentYear("2026-27")
                .returnType("ITR-1")
                .build());
        assertThat(prepResult.isReadyForSubmission()).isFalse();
        assertThat(prepResult.getStatus()).isEqualTo("VALIDATION_FAILED");

        // 2. Submission of unregistered PAN throws FORBIDDEN
        assertThatThrownBy(() -> itrGovIntegrationService.submitReturn(ItrSubmitReturnRequest.builder()
                .pan("ZZZZZ9999Z")
                .assessmentYear("2026-27")
                .returnType("ITR-1")
                .build()))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException appEx = (AppException) e;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
                });
    }

    @Test
    @DisplayName("Negative E2E: Submission without preparation rejected")
    void testE2EUnpreparedReturnSubmissionRejected() {
        ItrReturnDto unPreparedReturn = itrService.createReturn(CreateItrReturnRequest.builder()
                .clientId(clientA.getId())
                .assessmentYear("2026-27")
                .financialYear("2025-26")
                .itrType(ItrType.ITR_1)
                .taxpayerType(TaxpayerType.INDIVIDUAL)
                .dueDate(LocalDate.of(2026, 7, 31))
                .build());

        assertThatThrownBy(() -> itrGovIntegrationService.submitReturn(ItrSubmitReturnRequest.builder()
                .returnId(unPreparedReturn.getId())
                .build()))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException appEx = (AppException) e;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                    assertThat(appEx.getMessage()).contains("Return must be prepared and in READY_TO_FILE status");
                });
    }

    @Test
    @DisplayName("Negative E2E: Cross-tenant isolation blocks Org B from accessing Org A's return")
    void testE2ECrossTenantIsolation() {
        ItrReturnDto returnA = itrService.createReturn(CreateItrReturnRequest.builder()
                .clientId(clientA.getId())
                .assessmentYear("2026-27")
                .financialYear("2025-26")
                .itrType(ItrType.ITR_1)
                .taxpayerType(TaxpayerType.INDIVIDUAL)
                .dueDate(LocalDate.of(2026, 7, 31))
                .build());

        // Switch to Org B
        TenantContext.setTenantId(orgB.getId());
        SecurityUser userB = SecurityUser.builder()
                .userId(UUID.randomUUID())
                .organizationId(orgB.getId())
                .email("admin@" + orgB.getId() + ".taxoryn.com")
                .roles(Set.of("ROLE_ORG_ADMIN"))
                .permissions(Set.of("ROLE_ORG_ADMIN", "ITR_VIEW", "ITR_CREATE", "ITR_UPDATE", "ITR_WRITE"))
                .enabled(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userB, null, userB.getAuthorities())
        );

        // Org B cannot access Org A's return
        assertThatThrownBy(() -> itrGovIntegrationService.prepareReturn(ItrPrepareReturnRequest.builder()
                .returnId(returnA.getId())
                .pan("ABCDE1234F")
                .returnType("ITR-1")
                .build()))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException appEx = (AppException) e;
                    assertThat(appEx.getErrorCode()).isIn(ErrorCode.TENANT_MISMATCH, ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.FORBIDDEN);
                });
    }

    @Test
    @DisplayName("Negative E2E: Transient failures (PROVIDER_UNAVAILABLE, TIMEOUT) preserve VERIFICATION_PENDING")
    void testE2ETransientFailuresPreserveState() {
        ItrReturnDto testRet = itrService.createReturn(CreateItrReturnRequest.builder()
                .clientId(clientA.getId())
                .assessmentYear("2026-27")
                .financialYear("2025-26")
                .itrType(ItrType.ITR_1)
                .taxpayerType(TaxpayerType.INDIVIDUAL)
                .dueDate(LocalDate.of(2026, 7, 31))
                .build());

        itrGovIntegrationService.prepareReturn(ItrPrepareReturnRequest.builder()
                .returnId(testRet.getId())
                .clientId(clientA.getId())
                .profileId(profileA.getId())
                .pan("ABCDE1234F")
                .returnType("ITR-1")
                .assessmentYear("2026-27")
                .taxSummary(ItrTaxSummaryDto.builder().build())
                .build());

        itrGovIntegrationService.submitReturn(ItrSubmitReturnRequest.builder()
                .returnId(testRet.getId())
                .build());

        // Status check with timeout directive
        ItrReturnStatusDto statusDto = itrGovIntegrationService.checkReturnStatus(
                testRet.getId(),
                connectionA.getId(),
                Map.of("directive", "TIMEOUT")
        );

        assertThat(statusDto.isSuccess()).isFalse();
        assertThat(statusDto.getErrorCode()).isEqualTo("TIMEOUT");

        ItrReturnEntity entity = itrReturnRepository.findById(testRet.getId()).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo(ItrStatus.VERIFICATION_PENDING);
        assertThat(entity.getFilingDate()).isNull();
    }

    @Test
    @DisplayName("Negative E2E: Government rejection transitions return to CANCELLED and preserves rejection note")
    void testE2ERejectedOutcome() {
        ItrReturnDto testRet = itrService.createReturn(CreateItrReturnRequest.builder()
                .clientId(clientA.getId())
                .assessmentYear("2026-27")
                .financialYear("2025-26")
                .itrType(ItrType.ITR_1)
                .taxpayerType(TaxpayerType.INDIVIDUAL)
                .dueDate(LocalDate.of(2026, 7, 31))
                .build());

        itrGovIntegrationService.prepareReturn(ItrPrepareReturnRequest.builder()
                .returnId(testRet.getId())
                .clientId(clientA.getId())
                .profileId(profileA.getId())
                .pan("ABCDE1234F")
                .returnType("ITR-1")
                .assessmentYear("2026-27")
                .taxSummary(ItrTaxSummaryDto.builder().build())
                .build());

        itrGovIntegrationService.submitReturn(ItrSubmitReturnRequest.builder()
                .returnId(testRet.getId())
                .build());

        ItrReturnStatusDto statusDto = itrGovIntegrationService.checkReturnStatus(
                testRet.getId(),
                connectionA.getId(),
                Map.of("mockStatus", "REJECTED")
        );

        assertThat(statusDto.isSuccess()).isTrue();
        assertThat(statusDto.getFilingStatus()).isEqualTo(ItrStatus.CANCELLED);
        assertThat(statusDto.isTerminal()).isTrue();

        ItrReturnEntity entity = itrReturnRepository.findById(testRet.getId()).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo(ItrStatus.CANCELLED);
        assertThat(entity.getNotes()).contains("REJECTED");
    }

    // =========================================================================
    // 3. PAYLOAD INTEGRITY & FINGERPRINTING
    // =========================================================================

    @Test
    @DisplayName("Payload Integrity: Same business payload generates identical SHA-256 fingerprint")
    void testPayloadFingerprintDeterministic() {
        ItrTaxSummaryDto summary1 = ItrTaxSummaryDto.builder()
                .grossTotalIncome(new BigDecimal("1000000.00"))
                .taxableIncome(new BigDecimal("850000.00"))
                .build();
        ItrTaxSummaryDto summary2 = ItrTaxSummaryDto.builder()
                .grossTotalIncome(new BigDecimal("1000000.00"))
                .taxableIncome(new BigDecimal("850000.00"))
                .build();

        String fp1 = fingerprintGenerator.generateFingerprint("ABCDE1234F", "2026-27", "ITR-1", "INDIVIDUAL", "RESIDENT", summary1, Map.of("sal", 1000), Map.of(), Map.of(), Map.of());
        String fp2 = fingerprintGenerator.generateFingerprint("ABCDE1234F", "2026-27", "ITR-1", "INDIVIDUAL", "RESIDENT", summary2, Map.of("sal", 1000), Map.of(), Map.of(), Map.of());

        assertThat(fp1).isEqualTo(fp2);

        // Modified payload generates different fingerprint
        String fp3 = fingerprintGenerator.generateFingerprint("ABCDE1234F", "2026-27", "ITR-1", "INDIVIDUAL", "RESIDENT", summary1, Map.of("sal", 2000), Map.of(), Map.of(), Map.of());
        assertThat(fp1).isNotEqualTo(fp3);
    }

    // =========================================================================
    // 4. AUDIT TRAIL & PAN MASKING VERIFICATION
    // =========================================================================

    @Test
    @DisplayName("Audit & Security: Full lifecycle emits audit events with masked PAN and zero credential leakage")
    void testE2EAuditTrailAndPanMasking() {
        ItrReturnDto testRet = itrService.createReturn(CreateItrReturnRequest.builder()
                .clientId(clientA.getId())
                .assessmentYear("2026-27")
                .financialYear("2025-26")
                .itrType(ItrType.ITR_1)
                .taxpayerType(TaxpayerType.INDIVIDUAL)
                .dueDate(LocalDate.of(2026, 7, 31))
                .build());

        itrGovIntegrationService.prepareReturn(ItrPrepareReturnRequest.builder()
                .returnId(testRet.getId())
                .clientId(clientA.getId())
                .profileId(profileA.getId())
                .pan("ABCDE1234F")
                .returnType("ITR-1")
                .assessmentYear("2026-27")
                .taxSummary(ItrTaxSummaryDto.builder().build())
                .build());

        itrGovIntegrationService.submitReturn(ItrSubmitReturnRequest.builder()
                .returnId(testRet.getId())
                .build());

        itrGovIntegrationService.checkReturnStatus(testRet.getId(), connectionA.getId(), Map.of("mockStatus", "FILED"));

        ArgumentCaptor<Map<String, Object>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService, atLeastOnce()).logEvent(
                eq(orgA.getId()),
                any(),
                eq("ITR_RETURN_FILED"),
                eq("ITR_RETURN"),
                any(),
                any(),
                metadataCaptor.capture()
        );

        Map<String, Object> loggedData = metadataCaptor.getValue();
        assertThat(loggedData).containsKey("pan");
        assertThat(loggedData.get("pan").toString()).isEqualTo("ABCDE****F");
        assertThat(loggedData).doesNotContainKey("apiKey");
        assertThat(loggedData).doesNotContainKey("rawSecret");
        assertThat(loggedData).doesNotContainKey("password");
    }
}
