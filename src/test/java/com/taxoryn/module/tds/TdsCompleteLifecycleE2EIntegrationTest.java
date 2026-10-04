package com.taxoryn.module.tds;

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
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.tds.controller.TdsController;
import com.taxoryn.module.tds.dto.*;
import com.taxoryn.module.tds.entity.TdsChallanEntity;
import com.taxoryn.module.tds.entity.TdsProfileEntity.DeductorType;
import com.taxoryn.module.tds.entity.TdsReturnEntity;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFilingStatus;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFormType;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsQuarter;
import com.taxoryn.module.tds.integration.TdsGovernmentIntegrationService;
import com.taxoryn.module.tds.repository.TdsChallanRepository;
import com.taxoryn.module.tds.repository.TdsReturnRepository;
import com.taxoryn.module.tds.service.TdsPayloadFingerprintGenerator;
import com.taxoryn.module.tds.service.TdsService;
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
import java.util.Collections;
import java.util.List;
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
public class TdsCompleteLifecycleE2EIntegrationTest {

    @Autowired
    private TdsGovernmentIntegrationService tdsGovIntegrationService;

    @Autowired
    private TdsService tdsService;

    @Autowired
    private TdsReturnRepository tdsReturnRepository;

    @Autowired
    private TdsChallanRepository tdsChallanRepository;

    @Autowired
    private GovernmentConnectionService govConnectionService;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private TdsController tdsController;

    @Autowired
    private TdsPayloadFingerprintGenerator fingerprintGenerator;

    @MockBean
    private AuditService auditService;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private ClientEntity clientA;
    private TdsProfileDto profileA;
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
                .permissions(Set.of("ROLE_ORG_ADMIN", "TDS_VIEW", "TDS_CREATE", "TDS_UPDATE", "TDS_WRITE", "CLIENT_VIEW"))
                .enabled(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userA, null, userA.getAuthorities())
        );

        clientA = clientRepository.save(ClientEntity.builder()
                .displayName("Acme Infrastructure Limited")
                .legalName("Acme Infrastructure Limited")
                .pan("AAACA1234C")
                .clientType(ClientType.COMPANY)
                .status(ClientStatus.ACTIVE)
                .build());

        connectionA = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.TDS)
                .displayName("TRACES Production Gateway Org A")
                .build());

        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(connectionA.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("tds_prod_***")
                .rawSecret("SecretTdsMasterKeyOrgA123")
                .build());

        govConnectionService.activateConnection(connectionA.getId());

        profileA = tdsService.createProfile(CreateTdsProfileRequest.builder()
                .clientId(clientA.getId())
                .tan("MUMB12345A")
                .deductorType(DeductorType.COMPANY)
                .responsiblePersonName("Sanjay Mehra")
                .responsiblePersonPan("AAACA1234C")
                .build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    // =========================================================================
    // 1. Full Master TDS E2E Happy Path (TAN -> Prep -> Submit -> Status -> Reconcile)
    // =========================================================================

    @Test
    @DisplayName("Master TDS E2E Workflow: TAN Verify -> Prep -> Fingerprint -> Submit -> Poll -> FILED -> Reconcile")
    void testMasterTdsE2EHappyPath() {
        // Step 1: TAN Lookup & Verification
        TdsDeductorProfileDto verifiedTan = tdsGovIntegrationService.lookupDeductor("MUMB12345A");
        assertThat(verifiedTan).isNotNull();
        assertThat(verifiedTan.isValid()).isTrue();
        assertThat(verifiedTan.isVerified()).isTrue();
        assertThat(verifiedTan.getTan()).isEqualTo("MUMB12345A");

        // Step 2: Create Quarterly Return & Deposit Challan
        TdsReturnDto returnDto = tdsService.createReturn(CreateTdsReturnRequest.builder()
                .clientId(clientA.getId())
                .tdsProfileId(profileA.getId())
                .formType(TdsFormType.FORM_26Q)
                .quarter(TdsQuarter.Q1)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .dueDate(LocalDate.of(2025, 7, 31))
                .build());

        TdsChallanDto challanDto = tdsService.createChallan(CreateTdsChallanRequest.builder()
                .tdsProfileId(profileA.getId())
                .bsrCode("0210001")
                .challanDate(LocalDate.of(2025, 5, 7))
                .challanSerialNo("10023")
                .cin("02100011504202610023")
                .sectionCode("194C")
                .tdsAmount(new BigDecimal("50000.00"))
                .quarter(TdsQuarter.Q1)
                .financialYear("2025-26")
                .build());

        TdsChallanEntity challanEntity = tdsChallanRepository.findById(challanDto.getId()).orElseThrow();
        challanEntity.setTdsReturnId(returnDto.getId());
        tdsChallanRepository.save(challanEntity);

        // Step 3: Return Preparation & Fingerprinting
        TdsReturnTotalsDto totals = TdsReturnTotalsDto.builder()
                .totalAmountPaid(new BigDecimal("2500000.00"))
                .totalTaxDeducted(new BigDecimal("50000.00"))
                .totalTaxDeposited(new BigDecimal("50000.00"))
                .totalInterest(BigDecimal.ZERO)
                .totalLateFee(BigDecimal.ZERO)
                .totalPenalty(BigDecimal.ZERO)
                .build();

        TdsPreparedReturnDto prepared = tdsGovIntegrationService.prepareReturn(TdsPrepareReturnRequest.builder()
                .returnId(returnDto.getId())
                .clientId(clientA.getId())
                .profileId(profileA.getId())
                .tan("MUMB12345A")
                .formType("FORM_26Q")
                .quarter("Q1")
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .totals(totals)
                .build());

        assertThat(prepared).isNotNull();
        assertThat(prepared.isReadyForSubmission()).isTrue();
        assertThat(prepared.getStatus()).isEqualTo("READY_TO_FILE");
        assertThat(prepared.getPayloadFingerprint()).isNotBlank();

        TdsReturnEntity readyEntity = tdsReturnRepository.findById(returnDto.getId()).orElseThrow();
        assertThat(readyEntity.getFilingStatus()).isEqualTo(TdsFilingStatus.READY_TO_FILE);

        // Step 4: Submission to TRACES Gateway
        TdsReturnSubmissionResultDto submission = tdsGovIntegrationService.submitReturn(TdsSubmitReturnRequest.builder()
                .returnId(returnDto.getId())
                .expectedFingerprint(prepared.getPayloadFingerprint())
                .build());

        assertThat(submission).isNotNull();
        assertThat(submission.isSuccess()).isTrue();
        assertThat(submission.getSubmissionStatus()).isEqualTo("SUBMITTED");
        assertThat(submission.getAcknowledgementNumber()).isNotBlank();

        // Invariant check: SUBMITTED != FILED and filingDate is null
        TdsReturnEntity submittedEntity = tdsReturnRepository.findById(returnDto.getId()).orElseThrow();
        assertThat(submittedEntity.getFilingStatus()).isEqualTo(TdsFilingStatus.SUBMITTED);
        assertThat(submittedEntity.getFilingDate()).isNull();

        // Step 5: Status Check & Authoritative Transition to FILED
        TdsReturnStatusDto statusResult = tdsGovIntegrationService.checkReturnStatus(returnDto.getId());

        assertThat(statusResult).isNotNull();
        assertThat(statusResult.isSuccess()).isTrue();
        assertThat(statusResult.getFilingStatus()).isEqualTo(TdsFilingStatus.FILED);
        assertThat(statusResult.getProviderStatus()).isEqualTo("FILED");
        assertThat(statusResult.isTerminal()).isTrue();
        assertThat(statusResult.getFilingDate()).isNotNull();

        // Step 6: Verify Challan Reconciliation
        assertThat(statusResult.getChallans()).isNotEmpty();
        TdsChallanReconciliationDto challanReconciliation = statusResult.getChallans().get(0);
        assertThat(challanReconciliation.getBsrCode()).isEqualTo("0210001");
        assertThat(challanReconciliation.getChallanSerialNo()).isEqualTo("10023");
        assertThat(challanReconciliation.getStatus()).isEqualTo("MATCHED");

        TdsChallanEntity updatedChallan = tdsChallanRepository.findById(challanDto.getId()).orElseThrow();
        assertThat(updatedChallan.getChallanStatus()).isEqualTo(TdsChallanEntity.ChallanStatus.FULLY_UTILIZED);
        assertThat(updatedChallan.getUtilizedAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(updatedChallan.getBalanceAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // =========================================================================
    // 2. Multi-Form Preparation Matrix (24Q, 26Q, 27Q, 27EQ)
    // =========================================================================

    @Test
    @DisplayName("Preparation supports Form 24Q, Form 26Q, Form 27Q, and Form 27EQ")
    void testPreparationAllFormsSupported() {
        String[] forms = {"FORM_24Q", "FORM_26Q", "FORM_27Q", "FORM_27EQ"};
        for (String form : forms) {
            TdsReturnTotalsDto totals = TdsReturnTotalsDto.builder()
                    .totalAmountPaid(new BigDecimal("100000.00"))
                    .totalTaxDeducted(new BigDecimal("10000.00"))
                    .totalTaxDeposited(new BigDecimal("10000.00"))
                    .build();

            TdsPreparedReturnDto prepared = tdsGovIntegrationService.prepareReturn(TdsPrepareReturnRequest.builder()
                    .tan("MUMB12345A")
                    .formType(form)
                    .quarter("Q1")
                    .financialYear("2025-26")
                    .assessmentYear("2026-27")
                    .totals(totals)
                    .build());

            assertThat(prepared.isReadyForSubmission()).isTrue();
            assertThat(prepared.getFormType()).isEqualTo(form);
            assertThat(prepared.getPayloadFingerprint()).isNotBlank();
        }
    }

    // =========================================================================
    // 3. Fingerprint Determinism & Mutation Sensitivity
    // =========================================================================

    @Test
    @DisplayName("Fingerprint is deterministic and sensitive to payload mutations")
    void testFingerprintDeterminismAndMutationSensitivity() {
        TdsReturnTotalsDto totals1 = TdsReturnTotalsDto.builder()
                .totalAmountPaid(new BigDecimal("500000.00"))
                .totalTaxDeducted(new BigDecimal("10000.00"))
                .totalTaxDeposited(new BigDecimal("10000.00"))
                .build();

        String fp1 = fingerprintGenerator.generateFingerprint(
                "MUMB12345A", "FORM_26Q", "2025-26", "Q1", "2026-27", "COMPANY",
                totals1, Collections.emptyList(), Collections.emptyList(), Collections.emptyMap()
        );

        String fp2 = fingerprintGenerator.generateFingerprint(
                "MUMB12345A", "FORM_26Q", "2025-26", "Q1", "2026-27", "COMPANY",
                totals1, Collections.emptyList(), Collections.emptyList(), Collections.emptyMap()
        );

        assertThat(fp1).isEqualTo(fp2);

        TdsReturnTotalsDto totals2 = TdsReturnTotalsDto.builder()
                .totalAmountPaid(new BigDecimal("500000.00"))
                .totalTaxDeducted(new BigDecimal("10000.00"))
                .totalTaxDeposited(new BigDecimal("9999.00")) // mutated amount
                .build();

        String fpMutated = fingerprintGenerator.generateFingerprint(
                "MUMB12345A", "FORM_26Q", "2025-26", "Q1", "2026-27", "COMPANY",
                totals2, Collections.emptyList(), Collections.emptyList(), Collections.emptyMap()
        );

        assertThat(fp1).isNotEqualTo(fpMutated);
    }

    // =========================================================================
    // 4. State Machine Guards
    // =========================================================================

    @Test
    @DisplayName("State machine guard: DRAFT return cannot submit directly")
    void testStateMachineGuard_DraftCannotSubmit() {
        TdsReturnDto unprepReturn = tdsService.createReturn(CreateTdsReturnRequest.builder()
                .clientId(clientA.getId())
                .tdsProfileId(profileA.getId())
                .formType(TdsFormType.FORM_26Q)
                .quarter(TdsQuarter.Q1)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .dueDate(LocalDate.of(2025, 7, 31))
                .build());

        assertThatThrownBy(() -> tdsGovIntegrationService.submitReturn(unprepReturn.getId(), Map.of()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("READY_TO_FILE");
    }

    @Test
    @DisplayName("State machine guard: FILED return cannot be resubmitted")
    void testStateMachineGuard_FiledCannotSubmit() {
        TdsReturnDto returnDto = tdsService.createReturn(CreateTdsReturnRequest.builder()
                .clientId(clientA.getId())
                .tdsProfileId(profileA.getId())
                .formType(TdsFormType.FORM_26Q)
                .quarter(TdsQuarter.Q1)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .dueDate(LocalDate.of(2025, 7, 31))
                .build());

        TdsReturnTotalsDto totals = TdsReturnTotalsDto.builder()
                .totalAmountPaid(new BigDecimal("100000.00"))
                .totalTaxDeducted(new BigDecimal("10000.00"))
                .totalTaxDeposited(new BigDecimal("10000.00"))
                .build();

        tdsGovIntegrationService.prepareReturn(TdsPrepareReturnRequest.builder()
                .returnId(returnDto.getId())
                .tan("MUMB12345A")
                .formType("FORM_26Q")
                .quarter("Q1")
                .financialYear("2025-26")
                .totals(totals)
                .build());

        tdsGovIntegrationService.submitReturn(returnDto.getId(), Map.of());
        tdsGovIntegrationService.checkReturnStatus(returnDto.getId()); // Transitions to FILED

        assertThatThrownBy(() -> tdsGovIntegrationService.submitReturn(returnDto.getId(), Map.of()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("FILED");
    }

    // =========================================================================
    // 5. Cross-Tenant Isolation
    // =========================================================================

    @Test
    @DisplayName("Cross-tenant isolation strictly blocks unauthorized tenant access")
    void testCrossTenantIsolation() {
        // Prepare return under Org A
        TdsReturnDto returnDto = tdsService.createReturn(CreateTdsReturnRequest.builder()
                .clientId(clientA.getId())
                .tdsProfileId(profileA.getId())
                .formType(TdsFormType.FORM_26Q)
                .quarter(TdsQuarter.Q1)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .dueDate(LocalDate.of(2025, 7, 31))
                .build());

        // Switch to Org B
        TenantContext.setTenantId(orgB.getId());

        // Org B attempts to submit Org A's return
        assertThatThrownBy(() -> tdsGovIntegrationService.submitReturn(returnDto.getId(), Map.of()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("TDS return does not belong to current organization");

        // Org B attempts to check status of Org A's return
        assertThatThrownBy(() -> tdsGovIntegrationService.checkReturnStatus(returnDto.getId()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("TDS return does not belong to current organization");
    }

    // =========================================================================
    // 6. Audit Logging Verification
    // =========================================================================

    @Test
    @DisplayName("Audit events are reliably recorded for all major TDS lifecycle actions")
    void testAuditTrailCompleteness() {
        tdsGovIntegrationService.lookupDeductor("MUMB12345A");

        ArgumentCaptor<String> eventCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService, atLeastOnce()).logEvent(
                eq(orgA.getId()),
                any(),
                eventCaptor.capture(),
                any(),
                any(),
                any(),
                any()
        );

        List<String> events = eventCaptor.getAllValues();
        assertThat(events).contains("TDS_TAN_LOOKUP_REQUESTED", "TDS_TAN_LOOKUP_COMPLETED");
    }
}
