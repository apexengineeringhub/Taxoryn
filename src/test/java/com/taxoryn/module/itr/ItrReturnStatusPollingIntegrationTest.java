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
import com.taxoryn.module.itr.dto.ItrPrepareReturnRequest;
import com.taxoryn.module.itr.dto.ItrPreparedReturnDto;
import com.taxoryn.module.itr.dto.ItrProfileDto;
import com.taxoryn.module.itr.dto.ItrReturnDto;
import com.taxoryn.module.itr.dto.ItrReturnStatusDto;
import com.taxoryn.module.itr.dto.ItrReturnSubmissionResultDto;
import com.taxoryn.module.itr.dto.ItrSubmitReturnRequest;
import com.taxoryn.module.itr.dto.ItrTaxSummaryDto;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrType;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ResidentialStatus;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
import com.taxoryn.module.itr.entity.ItrReturnEntity;
import com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus;
import com.taxoryn.module.itr.integration.ItrGovernmentIntegrationService;
import com.taxoryn.module.itr.repository.ItrReturnRepository;
import com.taxoryn.module.itr.service.ItrFilingStatusPollingScheduler;
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
public class ItrReturnStatusPollingIntegrationTest {

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
    private ItrFilingStatusPollingScheduler statusPollingScheduler;

    @MockBean
    private AuditService auditService;

    private OrganizationEntity testOrg;
    private OrganizationEntity otherOrg;
    private ClientEntity testClient;
    private ItrProfileDto testProfile;
    private ItrReturnDto testReturn;
    private GovConnectionDto itrConn;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("ITR Status Polling Practice " + UUID.randomUUID())
                .email("status.itr." + UUID.randomUUID() + "@taxoryn.com")
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
                .permissions(Set.of("ROLE_ORG_ADMIN", "ITR_VIEW", "ITR_CREATE", "ITR_UPDATE", "ITR_WRITE", "CLIENT_VIEW"))
                .enabled(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities())
        );

        testClient = clientRepository.save(ClientEntity.builder()
                .displayName("Ananya Deshmukh")
                .legalName("Ananya Deshmukh")
                .pan("ABCDE1234F")
                .clientType(ClientType.INDIVIDUAL)
                .status(ClientStatus.ACTIVE)
                .build());

        itrConn = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.INCOME_TAX)
                .displayName("Income Tax Status Gateway")
                .build());

        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(itrConn.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("itd_stat_***")
                .rawSecret("SecretItrStatusKey123")
                .build());

        govConnectionService.activateConnection(itrConn.getId());

        testProfile = itrService.createProfile(CreateItrProfileRequest.builder()
                .clientId(testClient.getId())
                .pan("ABCDE1234F")
                .taxpayerType(TaxpayerType.INDIVIDUAL)
                .defaultItrType(ItrType.ITR_1)
                .residentialStatus(ResidentialStatus.RESIDENT)
                .build());

        testReturn = itrService.createReturn(CreateItrReturnRequest.builder()
                .clientId(testClient.getId())
                .assessmentYear("2026-27")
                .financialYear("2025-26")
                .itrType(ItrType.ITR_1)
                .taxpayerType(TaxpayerType.INDIVIDUAL)
                .dueDate(LocalDate.of(2026, 7, 31))
                .build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private void prepareAndSubmitTestReturn() {
        ItrTaxSummaryDto summary = ItrTaxSummaryDto.builder()
                .grossTotalIncome(new BigDecimal("1200000.00"))
                .totalDeductions(new BigDecimal("150000.00"))
                .taxableIncome(new BigDecimal("1050000.00"))
                .taxPayable(new BigDecimal("127500.00"))
                .surcharge(BigDecimal.ZERO)
                .cess(new BigDecimal("5100.00"))
                .totalTaxLiability(new BigDecimal("132600.00"))
                .tdsTcsCredit(new BigDecimal("132600.00"))
                .advanceTaxPaid(BigDecimal.ZERO)
                .selfAssessmentTaxPaid(BigDecimal.ZERO)
                .totalTaxesPaid(new BigDecimal("132600.00"))
                .refundDue(BigDecimal.ZERO)
                .balancePayable(BigDecimal.ZERO)
                .build();

        itrGovIntegrationService.prepareReturn(ItrPrepareReturnRequest.builder()
                .returnId(testReturn.getId())
                .clientId(testClient.getId())
                .profileId(testProfile.getId())
                .pan("ABCDE1234F")
                .returnType("ITR-1")
                .assessmentYear("2026-27")
                .taxpayerType("INDIVIDUAL")
                .residentialStatus("RESIDENT")
                .taxSummary(summary)
                .build());

        ItrReturnSubmissionResultDto subResult = itrGovIntegrationService.submitReturn(ItrSubmitReturnRequest.builder()
                .returnId(testReturn.getId())
                .connectionId(itrConn.getId())
                .build());

        assertThat(subResult.isSuccess()).isTrue();
        assertThat(subResult.getSubmissionStatus()).isEqualTo("SUBMITTED");

        ItrReturnEntity entity = itrReturnRepository.findById(testReturn.getId()).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo(ItrStatus.VERIFICATION_PENDING);
        assertThat(entity.getFilingDate()).isNull();
    }

    @Test
    @DisplayName("checkReturnStatus authoritatively transitions return from VERIFICATION_PENDING to FILED")
    void testProviderFiledTransitionsToFiled() {
        prepareAndSubmitTestReturn();

        ItrReturnStatusDto statusResult = itrGovIntegrationService.checkReturnStatus(
                testReturn.getId(),
                itrConn.getId(),
                Map.of("mockStatus", "FILED")
        );

        assertThat(statusResult).isNotNull();
        assertThat(statusResult.isSuccess()).isTrue();
        assertThat(statusResult.getFilingStatus()).isEqualTo(ItrStatus.FILED);
        assertThat(statusResult.getProviderStatus()).isEqualTo("FILED");
        assertThat(statusResult.isTerminal()).isTrue();
        assertThat(statusResult.getFilingDate()).isNotNull();
        assertThat(statusResult.getAcknowledgementNumber()).startsWith("ITD-ACK-");
        assertThat(statusResult.getProviderReference()).startsWith("ITD-STATUS-REF-");

        // Verify Database Persistence
        ItrReturnEntity entity = itrReturnRepository.findById(testReturn.getId()).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo(ItrStatus.FILED);
        assertThat(entity.getFilingDate()).isNotNull();
        assertThat(entity.getVerificationDate()).isNotNull();
        assertThat(entity.getNotes()).contains("Filing confirmed authoritative");
    }

    @Test
    @DisplayName("checkReturnStatus with PROCESSED transitions return to FILED")
    void testProviderProcessedTransitionsToFiled() {
        prepareAndSubmitTestReturn();

        ItrReturnStatusDto statusResult = itrGovIntegrationService.checkReturnStatus(
                testReturn.getId(),
                itrConn.getId(),
                Map.of("mockStatus", "PROCESSED")
        );

        assertThat(statusResult.isSuccess()).isTrue();
        assertThat(statusResult.getFilingStatus()).isEqualTo(ItrStatus.FILED);
        assertThat(statusResult.getFilingDate()).isNotNull();
    }

    @Test
    @DisplayName("Provider status PROCESSING keeps return in VERIFICATION_PENDING and leaves filingDate null")
    void testProviderProcessingRemainsVerificationPending() {
        prepareAndSubmitTestReturn();

        ItrReturnStatusDto statusResult = itrGovIntegrationService.checkReturnStatus(
                testReturn.getId(),
                itrConn.getId(),
                Map.of("mockStatus", "PROCESSING")
        );

        assertThat(statusResult.isSuccess()).isTrue();
        assertThat(statusResult.getFilingStatus()).isEqualTo(ItrStatus.VERIFICATION_PENDING);
        assertThat(statusResult.getProviderStatus()).isEqualTo("PROCESSING");
        assertThat(statusResult.isTerminal()).isFalse();
        assertThat(statusResult.getFilingDate()).isNull();

        ItrReturnEntity entity = itrReturnRepository.findById(testReturn.getId()).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo(ItrStatus.VERIFICATION_PENDING);
        assertThat(entity.getFilingDate()).isNull();
    }

    @Test
    @DisplayName("Provider status PENDING keeps return in VERIFICATION_PENDING and leaves filingDate null")
    void testProviderPendingRemainsVerificationPending() {
        prepareAndSubmitTestReturn();

        ItrReturnStatusDto statusResult = itrGovIntegrationService.checkReturnStatus(
                testReturn.getId(),
                itrConn.getId(),
                Map.of("mockStatus", "PENDING")
        );

        assertThat(statusResult.isSuccess()).isTrue();
        assertThat(statusResult.getFilingStatus()).isEqualTo(ItrStatus.VERIFICATION_PENDING);
        assertThat(statusResult.isTerminal()).isFalse();
        assertThat(statusResult.getFilingDate()).isNull();
    }

    @Test
    @DisplayName("Provider status REJECTED transitions return to CANCELLED and captures rejection note")
    void testProviderRejectedTransitionsToCancelled() {
        prepareAndSubmitTestReturn();

        ItrReturnStatusDto statusResult = itrGovIntegrationService.checkReturnStatus(
                testReturn.getId(),
                itrConn.getId(),
                Map.of("mockStatus", "REJECTED")
        );

        assertThat(statusResult.isSuccess()).isTrue();
        assertThat(statusResult.getFilingStatus()).isEqualTo(ItrStatus.CANCELLED);
        assertThat(statusResult.getProviderStatus()).isEqualTo("REJECTED");
        assertThat(statusResult.isTerminal()).isTrue();

        ItrReturnEntity entity = itrReturnRepository.findById(testReturn.getId()).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo(ItrStatus.CANCELLED);
        assertThat(entity.getNotes()).contains("REJECTED");
    }

    @Test
    @DisplayName("Provider status FAILED transitions return to CANCELLED")
    void testProviderFailedTransitionsToCancelled() {
        prepareAndSubmitTestReturn();

        ItrReturnStatusDto statusResult = itrGovIntegrationService.checkReturnStatus(
                testReturn.getId(),
                itrConn.getId(),
                Map.of("mockStatus", "FAILED")
        );

        assertThat(statusResult.isSuccess()).isTrue();
        assertThat(statusResult.getFilingStatus()).isEqualTo(ItrStatus.CANCELLED);
        assertThat(statusResult.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("AUTH_REQUIRED error preserves VERIFICATION_PENDING state and does not mark FILED")
    void testAuthRequiredPreservesVerificationPending() {
        prepareAndSubmitTestReturn();

        ItrReturnStatusDto statusResult = itrGovIntegrationService.checkReturnStatus(
                testReturn.getId(),
                itrConn.getId(),
                Map.of("directive", "AUTH_REQUIRED")
        );

        assertThat(statusResult.isSuccess()).isFalse();
        assertThat(statusResult.getErrorCode()).isEqualTo("AUTH_REQUIRED");
        assertThat(statusResult.getFilingStatus()).isEqualTo(ItrStatus.VERIFICATION_PENDING);
        assertThat(statusResult.getFilingDate()).isNull();

        ItrReturnEntity entity = itrReturnRepository.findById(testReturn.getId()).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo(ItrStatus.VERIFICATION_PENDING);
        assertThat(entity.getFilingDate()).isNull();
    }

    @Test
    @DisplayName("PROVIDER_UNAVAILABLE, TIMEOUT and RATE_LIMITED preserve VERIFICATION_PENDING state")
    void testTransientErrorsPreserveState() {
        prepareAndSubmitTestReturn();

        ItrReturnStatusDto res1 = itrGovIntegrationService.checkReturnStatus(
                testReturn.getId(),
                itrConn.getId(),
                Map.of("directive", "PROVIDER_UNAVAILABLE")
        );
        assertThat(res1.isSuccess()).isFalse();
        assertThat(res1.getErrorCode()).isEqualTo("PROVIDER_UNAVAILABLE");

        ItrReturnStatusDto res2 = itrGovIntegrationService.checkReturnStatus(
                testReturn.getId(),
                itrConn.getId(),
                Map.of("directive", "TIMEOUT")
        );
        assertThat(res2.isSuccess()).isFalse();
        assertThat(res2.getErrorCode()).isEqualTo("TIMEOUT");

        ItrReturnStatusDto res3 = itrGovIntegrationService.checkReturnStatus(
                testReturn.getId(),
                itrConn.getId(),
                Map.of("directive", "RATE_LIMITED")
        );
        assertThat(res3.isSuccess()).isFalse();
        assertThat(res3.getErrorCode()).isEqualTo("RATE_LIMITED");

        ItrReturnEntity entity = itrReturnRepository.findById(testReturn.getId()).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo(ItrStatus.VERIFICATION_PENDING);
        assertThat(entity.getFilingDate()).isNull();
    }

    @Test
    @DisplayName("Re-checking an already FILED return returns terminal state without calling provider")
    void testAlreadyFiledProtection() {
        ItrReturnEntity entity = itrReturnRepository.findById(testReturn.getId()).orElseThrow();
        entity.setStatus(ItrStatus.FILED);
        entity.setAcknowledgementNumber("ITD-ACK-FINAL-777");
        entity.setFilingDate(LocalDate.of(2026, 7, 20));
        itrReturnRepository.save(entity);

        ItrReturnStatusDto statusResult = itrGovIntegrationService.checkReturnStatus(testReturn.getId());

        assertThat(statusResult.isSuccess()).isTrue();
        assertThat(statusResult.getFilingStatus()).isEqualTo(ItrStatus.FILED);
        assertThat(statusResult.getAcknowledgementNumber()).isEqualTo("ITD-ACK-FINAL-777");
        assertThat(statusResult.getFilingDate()).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(statusResult.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("Checking status on an unsubmitted return (DOCUMENTS_PENDING) throws VALIDATION_FAILED")
    void testUnsubmittedReturnStatusCheckRejected() {
        assertThatThrownBy(() -> itrGovIntegrationService.checkReturnStatus(testReturn.getId()))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException appEx = (AppException) e;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                    assertThat(appEx.getMessage()).contains("Return must be submitted first");
                });
    }

    @Test
    @DisplayName("Enforces tenant isolation: cannot check status of return belonging to another organization")
    void testCrossTenantStatusCheckRejected() {
        prepareAndSubmitTestReturn();

        // Switch to Org B
        TenantContext.setTenantId(otherOrg.getId());

        SecurityUser userB = SecurityUser.builder()
                .userId(UUID.randomUUID())
                .organizationId(otherOrg.getId())
                .email("admin@" + otherOrg.getId() + ".taxoryn.com")
                .roles(Set.of("ROLE_ORG_ADMIN"))
                .permissions(Set.of("ROLE_ORG_ADMIN", "ITR_VIEW", "ITR_CREATE", "ITR_UPDATE", "ITR_WRITE"))
                .enabled(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userB, null, userB.getAuthorities())
        );

        assertThatThrownBy(() -> itrGovIntegrationService.checkReturnStatus(testReturn.getId()))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException appEx = (AppException) e;
                    assertThat(appEx.getErrorCode()).isIn(ErrorCode.TENANT_MISMATCH, ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.FORBIDDEN);
                });
    }

    @Test
    @DisplayName("getReturnStatus reads persisted database status without invoking external government provider")
    void testGetStatusReturnsPersistedStateWithoutProviderCall() {
        prepareAndSubmitTestReturn();

        ItrReturnStatusDto status = itrGovIntegrationService.getReturnStatus(testReturn.getId());

        assertThat(status).isNotNull();
        assertThat(status.isSuccess()).isTrue();
        assertThat(status.getFilingStatus()).isEqualTo(ItrStatus.VERIFICATION_PENDING);
        assertThat(status.getAcknowledgementNumber()).startsWith("ITD-ACK-");
        assertThat(status.getFilingDate()).isNull();
        assertThat(status.getPan()).isEqualTo("ABCDE1234F");
        assertThat(status.getMaskedPan()).isEqualTo("ABCDE****F");
    }

    @Test
    @DisplayName("Scheduler polls only eligible VERIFICATION_PENDING returns and transitions them to FILED")
    void testSchedulerPollsPendingReturns() {
        prepareAndSubmitTestReturn();

        // Execute background scheduler
        statusPollingScheduler.pollSubmittedReturnStatuses();

        // After polling with default mock outcome (FILED), return should transition to FILED
        ItrReturnEntity entity = itrReturnRepository.findById(testReturn.getId()).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo(ItrStatus.FILED);
        assertThat(entity.getFilingDate()).isNotNull();
    }

    @Test
    @DisplayName("Controller POST /api/v1/itr/returns/{id}/status-check executes status check")
    void testControllerStatusCheck() {
        prepareAndSubmitTestReturn();

        var response = itrController.checkReturnStatus(testReturn.getId(), Map.of("mockStatus", "FILED"));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getFilingStatus()).isEqualTo(ItrStatus.FILED);
        assertThat(response.getBody().getData().getFilingDate()).isNotNull();
    }

    @Test
    @DisplayName("Controller GET /api/v1/itr/returns/{id}/status reads persisted status")
    void testControllerGetStatus() {
        prepareAndSubmitTestReturn();

        var response = itrController.getReturnStatus(testReturn.getId());

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getFilingStatus()).isEqualTo(ItrStatus.VERIFICATION_PENDING);
    }

    @Test
    @DisplayName("Audit events are logged with masked PAN and zero credential exposure")
    void testAuditLogging() {
        prepareAndSubmitTestReturn();

        itrGovIntegrationService.checkReturnStatus(testReturn.getId(), null, Map.of("mockStatus", "FILED"));

        ArgumentCaptor<Map<String, Object>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService, atLeastOnce()).logEvent(
                eq(testOrg.getId()),
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
        assertThat(loggedData).doesNotContainKey("secret");
    }
}
