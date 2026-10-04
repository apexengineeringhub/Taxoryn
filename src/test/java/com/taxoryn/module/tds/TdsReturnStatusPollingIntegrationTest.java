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
import com.taxoryn.module.tds.service.TdsFilingStatusPollingScheduler;
import com.taxoryn.module.tds.service.TdsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
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
public class TdsReturnStatusPollingIntegrationTest {

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
    private TdsFilingStatusPollingScheduler statusPollingScheduler;

    @MockBean
    private AuditService auditService;

    private OrganizationEntity testOrg;
    private OrganizationEntity otherOrg;
    private ClientEntity testClient;
    private TdsProfileDto testProfile;
    private TdsReturnDto testReturn;
    private TdsChallanDto testChallan;
    private GovConnectionDto tdsConn;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("TDS Status Polling Practice " + UUID.randomUUID())
                .email("status.tds." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        otherOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Other Org " + UUID.randomUUID())
                .email("other." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(testOrg.getId());

        SecurityUser testUser = SecurityUser.builder()
                .userId(UUID.randomUUID())
                .organizationId(testOrg.getId())
                .email("admin@" + testOrg.getId() + ".taxoryn.com")
                .roles(Set.of("ROLE_ORG_ADMIN"))
                .permissions(Set.of("ROLE_ORG_ADMIN", "TDS_VIEW", "TDS_CREATE", "TDS_UPDATE", "TDS_WRITE", "CLIENT_VIEW"))
                .enabled(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities())
        );

        testClient = clientRepository.save(ClientEntity.builder()
                .displayName("Alpha Corporate Services Limited")
                .legalName("Alpha Corporate Services Limited")
                .pan("AAACA1234C")
                .clientType(ClientType.COMPANY)
                .status(ClientStatus.ACTIVE)
                .build());

        tdsConn = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.TDS)
                .displayName("TRACES Gateway Status Connection")
                .build());

        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(tdsConn.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("tds_stat_***")
                .rawSecret("SecretTdsStatusKey123")
                .build());

        govConnectionService.activateConnection(tdsConn.getId());

        testProfile = tdsService.createProfile(CreateTdsProfileRequest.builder()
                .clientId(testClient.getId())
                .tan("MUMB12345A")
                .deductorType(DeductorType.COMPANY)
                .responsiblePersonName("Rajesh Sharma")
                .responsiblePersonPan("AAACA1234C")
                .build());

        testReturn = tdsService.createReturn(CreateTdsReturnRequest.builder()
                .clientId(testClient.getId())
                .tdsProfileId(testProfile.getId())
                .formType(TdsFormType.FORM_26Q)
                .quarter(TdsQuarter.Q1)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .dueDate(LocalDate.of(2025, 7, 31))
                .build());

        testChallan = tdsService.createChallan(CreateTdsChallanRequest.builder()
                .tdsProfileId(testProfile.getId())
                .bsrCode("0210001")
                .challanDate(LocalDate.of(2025, 5, 7))
                .challanSerialNo("10023")
                .cin("02100011504202610023")
                .sectionCode("194C")
                .tdsAmount(new BigDecimal("50000.00"))
                .quarter(TdsQuarter.Q1)
                .financialYear("2025-26")
                .build());

        // Link challan to return
        TdsChallanEntity challanEntity = tdsChallanRepository.findById(testChallan.getId()).orElseThrow();
        challanEntity.setTdsReturnId(testReturn.getId());
        tdsChallanRepository.save(challanEntity);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private void prepareAndSubmitTestReturn() {
        TdsReturnTotalsDto totals = TdsReturnTotalsDto.builder()
                .totalAmountPaid(new BigDecimal("2500000.00"))
                .totalTaxDeducted(new BigDecimal("50000.00"))
                .totalTaxDeposited(new BigDecimal("50000.00"))
                .totalInterest(BigDecimal.ZERO)
                .totalLateFee(BigDecimal.ZERO)
                .totalPenalty(BigDecimal.ZERO)
                .build();

        tdsGovIntegrationService.prepareReturn(TdsPrepareReturnRequest.builder()
                .returnId(testReturn.getId())
                .clientId(testClient.getId())
                .profileId(testProfile.getId())
                .tan("MUMB12345A")
                .formType("FORM_26Q")
                .quarter("Q1")
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .totals(totals)
                .build());

        tdsGovIntegrationService.submitReturn(testReturn.getId(), Map.of());
    }

    // =========================================================================
    // 1. Return Status Check & Lifecycle Transition Scenarios
    // =========================================================================

    @Test
    @DisplayName("Status Check Success - transitions from SUBMITTED to FILED and sets filingDate")
    void testCheckReturnStatus_Success_TransitionsToFiled() {
        prepareAndSubmitTestReturn();

        TdsReturnEntity submittedReturn = tdsReturnRepository.findById(testReturn.getId()).orElseThrow();
        assertThat(submittedReturn.getFilingStatus()).isEqualTo(TdsFilingStatus.SUBMITTED);
        assertThat(submittedReturn.getFilingDate()).isNull(); // Invariant: SUBMITTED != FILED

        TdsReturnStatusDto statusResult = tdsGovIntegrationService.checkReturnStatus(testReturn.getId());

        assertThat(statusResult).isNotNull();
        assertThat(statusResult.isSuccess()).isTrue();
        assertThat(statusResult.getFilingStatus()).isEqualTo(TdsFilingStatus.FILED);
        assertThat(statusResult.getProviderStatus()).isEqualTo("FILED");
        assertThat(statusResult.isTerminal()).isTrue();
        assertThat(statusResult.getFilingDate()).isNotNull();
        assertThat(statusResult.getAcknowledgementNumber()).isNotBlank();
        assertThat(statusResult.getMaskedTan()).isEqualTo("MUMB*****A");

        // Verify entity state in database
        TdsReturnEntity updatedEntity = tdsReturnRepository.findById(testReturn.getId()).orElseThrow();
        assertThat(updatedEntity.getFilingStatus()).isEqualTo(TdsFilingStatus.FILED);
        assertThat(updatedEntity.getFilingDate()).isEqualTo(statusResult.getFilingDate());
    }

    @Test
    @DisplayName("Status Check Processing - preserves SUBMITTED status and null filingDate")
    void testCheckReturnStatus_Processing_RemainsSubmitted() {
        prepareAndSubmitTestReturn();

        TdsReturnStatusDto statusResult = tdsGovIntegrationService.checkReturnStatus(
                testReturn.getId(),
                null,
                Map.of("mockOutcome", "PROCESSING")
        );

        assertThat(statusResult).isNotNull();
        assertThat(statusResult.isSuccess()).isTrue();
        assertThat(statusResult.getFilingStatus()).isEqualTo(TdsFilingStatus.SUBMITTED);
        assertThat(statusResult.getProviderStatus()).isEqualTo("PROCESSING");
        assertThat(statusResult.isTerminal()).isFalse();
        assertThat(statusResult.getFilingDate()).isNull();

        TdsReturnEntity entity = tdsReturnRepository.findById(testReturn.getId()).orElseThrow();
        assertThat(entity.getFilingStatus()).isEqualTo(TdsFilingStatus.SUBMITTED);
        assertThat(entity.getFilingDate()).isNull();
    }

    @Test
    @DisplayName("Status Check Pending - preserves SUBMITTED status")
    void testCheckReturnStatus_Pending_RemainsSubmitted() {
        prepareAndSubmitTestReturn();

        TdsReturnStatusDto statusResult = tdsGovIntegrationService.checkReturnStatus(
                testReturn.getId(),
                null,
                Map.of("mockOutcome", "PENDING")
        );

        assertThat(statusResult).isNotNull();
        assertThat(statusResult.isSuccess()).isTrue();
        assertThat(statusResult.getFilingStatus()).isEqualTo(TdsFilingStatus.SUBMITTED);
        assertThat(statusResult.getProviderStatus()).isEqualTo("PENDING");
        assertThat(statusResult.isTerminal()).isFalse();
    }

    @Test
    @DisplayName("Status Check Rejected - transitions to CANCELLED and marks terminal")
    void testCheckReturnStatus_Rejected_TransitionsToCancelled() {
        prepareAndSubmitTestReturn();

        TdsReturnStatusDto statusResult = tdsGovIntegrationService.checkReturnStatus(
                testReturn.getId(),
                null,
                Map.of("mockOutcome", "REJECTED")
        );

        assertThat(statusResult).isNotNull();
        assertThat(statusResult.isSuccess()).isTrue();
        assertThat(statusResult.getFilingStatus()).isEqualTo(TdsFilingStatus.CANCELLED);
        assertThat(statusResult.getProviderStatus()).isEqualTo("REJECTED");
        assertThat(statusResult.isTerminal()).isTrue();

        TdsReturnEntity entity = tdsReturnRepository.findById(testReturn.getId()).orElseThrow();
        assertThat(entity.getFilingStatus()).isEqualTo(TdsFilingStatus.CANCELLED);
    }

    @Test
    @DisplayName("Status Check Failed - resets status to READY_TO_FILE for resubmission")
    void testCheckReturnStatus_Failed_TransitionsToReadyToFile() {
        prepareAndSubmitTestReturn();

        TdsReturnStatusDto statusResult = tdsGovIntegrationService.checkReturnStatus(
                testReturn.getId(),
                null,
                Map.of("mockOutcome", "FAILED")
        );

        assertThat(statusResult).isNotNull();
        assertThat(statusResult.isSuccess()).isTrue();
        assertThat(statusResult.getFilingStatus()).isEqualTo(TdsFilingStatus.READY_TO_FILE);
        assertThat(statusResult.getProviderStatus()).isEqualTo("FAILED");

        TdsReturnEntity entity = tdsReturnRepository.findById(testReturn.getId()).orElseThrow();
        assertThat(entity.getFilingStatus()).isEqualTo(TdsFilingStatus.READY_TO_FILE);
    }

    // =========================================================================
    // 2. Gateway Error Directives Simulation
    // =========================================================================

    @Test
    @DisplayName("Status Check Auth Required - returns failure without corrupting return state")
    void testCheckReturnStatus_AuthRequired_Failure() {
        prepareAndSubmitTestReturn();

        TdsReturnStatusDto statusResult = tdsGovIntegrationService.checkReturnStatus(
                testReturn.getId(),
                null,
                Map.of("mockOutcome", "AUTH_REQUIRED")
        );

        assertThat(statusResult).isNotNull();
        assertThat(statusResult.isSuccess()).isFalse();
        assertThat(statusResult.getErrorCode()).isEqualTo("AUTH_REQUIRED");

        // Entity status should remain untouched
        TdsReturnEntity entity = tdsReturnRepository.findById(testReturn.getId()).orElseThrow();
        assertThat(entity.getFilingStatus()).isEqualTo(TdsFilingStatus.SUBMITTED);
    }

    @Test
    @DisplayName("Status Check Provider Unavailable - returns retryable failure")
    void testCheckReturnStatus_ProviderUnavailable_Failure() {
        prepareAndSubmitTestReturn();

        TdsReturnStatusDto statusResult = tdsGovIntegrationService.checkReturnStatus(
                testReturn.getId(),
                null,
                Map.of("mockOutcome", "PROVIDER_UNAVAILABLE")
        );

        assertThat(statusResult).isNotNull();
        assertThat(statusResult.isSuccess()).isFalse();
        assertThat(statusResult.getErrorCode()).isEqualTo("PROVIDER_UNAVAILABLE");
    }

    @Test
    @DisplayName("Status Check Timeout - returns retryable timeout failure")
    void testCheckReturnStatus_Timeout_Failure() {
        prepareAndSubmitTestReturn();

        TdsReturnStatusDto statusResult = tdsGovIntegrationService.checkReturnStatus(
                testReturn.getId(),
                null,
                Map.of("mockOutcome", "TIMEOUT")
        );

        assertThat(statusResult).isNotNull();
        assertThat(statusResult.isSuccess()).isFalse();
        assertThat(statusResult.getErrorCode()).isEqualTo("TIMEOUT");
    }

    @Test
    @DisplayName("Status Check Rate Limited - returns rate limited error code")
    void testCheckReturnStatus_RateLimited_Failure() {
        prepareAndSubmitTestReturn();

        TdsReturnStatusDto statusResult = tdsGovIntegrationService.checkReturnStatus(
                testReturn.getId(),
                null,
                Map.of("mockOutcome", "RATE_LIMITED")
        );

        assertThat(statusResult).isNotNull();
        assertThat(statusResult.isSuccess()).isFalse();
        assertThat(statusResult.getErrorCode()).isEqualTo("RATE_LIMITED");
    }

    // =========================================================================
    // 3. Terminal State Protection & Guards
    // =========================================================================

    @Test
    @DisplayName("Terminal FILED return protection - checking status on FILED return does not mutate state")
    void testCheckReturnStatus_AlreadyFiled_TerminalStateProtection() {
        prepareAndSubmitTestReturn();

        // Transition to FILED
        tdsGovIntegrationService.checkReturnStatus(testReturn.getId());

        TdsReturnEntity filedEntity = tdsReturnRepository.findById(testReturn.getId()).orElseThrow();
        LocalDate originalFilingDate = filedEntity.getFilingDate();
        String originalReceipt = filedEntity.getReceiptNumber();

        // Call again with REJECTED directive - must protect terminal state
        TdsReturnStatusDto recheck = tdsGovIntegrationService.checkReturnStatus(
                testReturn.getId(),
                null,
                Map.of("mockOutcome", "REJECTED")
        );

        assertThat(recheck.isSuccess()).isTrue();
        assertThat(recheck.getFilingStatus()).isEqualTo(TdsFilingStatus.FILED);
        assertThat(recheck.isTerminal()).isTrue();
        assertThat(recheck.getFilingDate()).isEqualTo(originalFilingDate);
        assertThat(recheck.getReceiptNumber()).isEqualTo(originalReceipt);

        TdsReturnEntity postCheckEntity = tdsReturnRepository.findById(testReturn.getId()).orElseThrow();
        assertThat(postCheckEntity.getFilingStatus()).isEqualTo(TdsFilingStatus.FILED);
    }

    @Test
    @DisplayName("Unsubmitted return status check guard - throws validation exception")
    void testCheckReturnStatus_UnsubmittedReturn_ThrowsValidationException() {
        // Return is in PENDING state
        assertThatThrownBy(() -> tdsGovIntegrationService.checkReturnStatus(testReturn.getId()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Cannot check filing status");
    }

    @Test
    @DisplayName("Non-existent return status check - throws not found exception")
    void testCheckReturnStatus_NonExistentReturn_ThrowsNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        assertThatThrownBy(() -> tdsGovIntegrationService.checkReturnStatus(nonExistentId))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Multi-tenant isolation - cross tenant status check throws TENANT_MISMATCH")
    void testCheckReturnStatus_CrossTenantIsolation() {
        prepareAndSubmitTestReturn();

        TenantContext.setTenantId(otherOrg.getId());

        assertThatThrownBy(() -> tdsGovIntegrationService.checkReturnStatus(testReturn.getId()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("TDS return does not belong to current organization");
    }

    // =========================================================================
    // 4. Persisted Status Retrieval & Challan Reconciliation
    // =========================================================================

    @Test
    @DisplayName("getReturnStatus retrieves persisted state without calling gateway")
    void testGetReturnStatus_PersistedStatus_NoGatewayCall() {
        prepareAndSubmitTestReturn();

        TdsReturnStatusDto status = tdsGovIntegrationService.getReturnStatus(testReturn.getId());

        assertThat(status).isNotNull();
        assertThat(status.getReturnId()).isEqualTo(testReturn.getId());
        assertThat(status.getTan()).isEqualTo("MUMB12345A");
        assertThat(status.getFilingStatus()).isEqualTo(TdsFilingStatus.SUBMITTED);
        assertThat(status.isTerminal()).isFalse();
        assertThat(status.getChallans()).isNotEmpty();
    }

    @Test
    @DisplayName("Challan reconciliation matches attached challans and marks FULLY_UTILIZED on FILED")
    void testChallanReconciliation_MatchesAttachedChallans() {
        prepareAndSubmitTestReturn();

        TdsReturnStatusDto statusResult = tdsGovIntegrationService.checkReturnStatus(testReturn.getId());

        assertThat(statusResult.getChallans()).isNotEmpty();
        TdsChallanReconciliationDto challanReconciliation = statusResult.getChallans().get(0);
        assertThat(challanReconciliation.getBsrCode()).isEqualTo("0210001");
        assertThat(challanReconciliation.getChallanSerialNo()).isEqualTo("10023");
        assertThat(challanReconciliation.getStatus()).isEqualTo("MATCHED");

        // Verify challan entity is updated to FULLY_UTILIZED
        TdsChallanEntity challanEntity = tdsChallanRepository.findById(testChallan.getId()).orElseThrow();
        assertThat(challanEntity.getChallanStatus()).isEqualTo(TdsChallanEntity.ChallanStatus.FULLY_UTILIZED);
        assertThat(challanEntity.getUtilizedAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(challanEntity.getBalanceAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // =========================================================================
    // 5. Background Polling Scheduler
    // =========================================================================

    @Test
    @DisplayName("Scheduler polls only eligible returns in SUBMITTED state")
    void testScheduler_PollsOnlySubmittedReturns() {
        prepareAndSubmitTestReturn();

        // Create a second draft return that should not be polled
        TdsReturnDto secondReturn = tdsService.createReturn(CreateTdsReturnRequest.builder()
                .clientId(testClient.getId())
                .tdsProfileId(testProfile.getId())
                .formType(TdsFormType.FORM_24Q)
                .quarter(TdsQuarter.Q2)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .dueDate(LocalDate.of(2025, 10, 31))
                .build());

        // Run scheduler
        statusPollingScheduler.pollSubmittedReturnStatuses();

        // First return should have transitioned to FILED
        TdsReturnEntity polledEntity = tdsReturnRepository.findById(testReturn.getId()).orElseThrow();
        assertThat(polledEntity.getFilingStatus()).isEqualTo(TdsFilingStatus.FILED);

        // Second return should remain in PENDING
        TdsReturnEntity draftEntity = tdsReturnRepository.findById(secondReturn.getId()).orElseThrow();
        assertThat(draftEntity.getFilingStatus()).isEqualTo(TdsFilingStatus.PENDING);
    }

    // =========================================================================
    // 6. Controller Endpoints
    // =========================================================================

    @Test
    @DisplayName("Controller POST /returns/{id}/status-check succeeds with 200 OK")
    void testController_CheckStatusEndpoint_Success() {
        prepareAndSubmitTestReturn();

        ResponseEntity<com.taxoryn.core.response.ApiResponse<TdsReturnStatusDto>> response =
                tdsController.checkReturnStatus(testReturn.getId(), Map.of());

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getFilingStatus()).isEqualTo(TdsFilingStatus.FILED);
    }

    @Test
    @DisplayName("Controller GET /returns/{id}/status succeeds with 200 OK")
    void testController_GetStatusEndpoint_Success() {
        prepareAndSubmitTestReturn();

        ResponseEntity<com.taxoryn.core.response.ApiResponse<TdsReturnStatusDto>> response =
                tdsController.getReturnStatus(testReturn.getId());

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getFilingStatus()).isEqualTo(TdsFilingStatus.SUBMITTED);
    }

    // =========================================================================
    // 7. Audit Logging Verification
    // =========================================================================

    @Test
    @DisplayName("Audit logging records TDS status check and filing events")
    void testAuditLogging_StatusCheckEvents() {
        prepareAndSubmitTestReturn();

        tdsGovIntegrationService.checkReturnStatus(testReturn.getId());

        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService, atLeastOnce()).logEvent(
                eq(testOrg.getId()),
                any(),
                eventTypeCaptor.capture(),
                any(),
                any(),
                any(),
                any()
        );

        List<String> loggedEvents = eventTypeCaptor.getAllValues();
        assertThat(loggedEvents).contains("TDS_RETURN_STATUS_CHECK_REQUESTED", "TDS_RETURN_FILED");
    }

    // =========================================================================
    // 8. Invariants Verification
    // =========================================================================

    @Test
    @DisplayName("Critical Invariant: SUBMITTED != FILED and filingDate is null until FILED")
    void testCriticalInvariant_SubmittedNotEqualToFiled() {
        prepareAndSubmitTestReturn();

        TdsReturnEntity submitted = tdsReturnRepository.findById(testReturn.getId()).orElseThrow();
        assertThat(submitted.getFilingStatus()).isEqualTo(TdsFilingStatus.SUBMITTED);
        assertThat(submitted.getFilingDate()).isNull();

        tdsGovIntegrationService.checkReturnStatus(testReturn.getId());

        TdsReturnEntity filed = tdsReturnRepository.findById(testReturn.getId()).orElseThrow();
        assertThat(filed.getFilingStatus()).isEqualTo(TdsFilingStatus.FILED);
        assertThat(filed.getFilingDate()).isNotNull();
    }
}
