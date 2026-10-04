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
public class ItrReturnSubmissionIntegrationTest {

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
                .name("ITR Submission Practice " + UUID.randomUUID())
                .email("sub.itr." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        otherOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Other Practice " + UUID.randomUUID())
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
                .displayName("Ramesh Kumar Sharma")
                .legalName("Ramesh Kumar Sharma")
                .pan("ABCDE1234F")
                .clientType(ClientType.INDIVIDUAL)
                .status(ClientStatus.ACTIVE)
                .build());

        itrConn = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.INCOME_TAX)
                .displayName("ITD e-Filing Gateway")
                .build());

        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(itrConn.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("itd_key_***")
                .rawSecret("SecretItrKey123")
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

    private void prepareTestReturn() {
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

        ItrPreparedReturnDto prepResult = itrGovIntegrationService.prepareReturn(ItrPrepareReturnRequest.builder()
                .returnId(testReturn.getId())
                .clientId(testClient.getId())
                .profileId(testProfile.getId())
                .pan("ABCDE1234F")
                .returnType("ITR1")
                .assessmentYear("2026-27")
                .taxpayerType("INDIVIDUAL")
                .residentialStatus("RESIDENT")
                .taxSummary(summary)
                .build());

        assertThat(prepResult.isReadyForSubmission()).isTrue();
    }

    @Test
    @DisplayName("submitReturn successfully submits prepared return and records deterministic mock acknowledgement")
    void testSubmitPreparedReturnSuccess() {
        prepareTestReturn();

        ItrSubmitReturnRequest request = ItrSubmitReturnRequest.builder()
                .returnId(testReturn.getId())
                .connectionId(itrConn.getId())
                .build();

        ItrReturnSubmissionResultDto result = itrGovIntegrationService.submitReturn(request);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSubmissionStatus()).isEqualTo("SUBMITTED");
        assertThat(result.getAcknowledgementNumber()).startsWith("ITD-ACK-");
        assertThat(result.getProviderReference()).startsWith("ITD-REF-");
        assertThat(result.getOperationId()).isNotNull();
        assertThat(result.getSubmittedAt()).isNotNull();
        assertThat(result.getPan()).isEqualTo("ABCDE1234F");
        assertThat(result.getMaskedPan()).isEqualTo("ABCDE****F");
        assertThat(result.getAssessmentYear()).isEqualTo("2026-27");
        assertThat(result.getReturnType()).isIn("ITR1", "ITR-1", "ITR_1");

        // Verify Database State: SUBMITTED != FILED
        ItrReturnEntity entity = itrReturnRepository.findById(testReturn.getId()).orElseThrow();
        assertThat(entity.getAcknowledgementNumber()).isEqualTo(result.getAcknowledgementNumber());
        assertThat(entity.getStatus()).isEqualTo(ItrStatus.VERIFICATION_PENDING);
        assertThat(entity.getFilingDate()).isNull(); // Crucial: filingDate must NOT be set until verified in Phase 24.5
        assertThat(entity.getNotes()).contains("Submitted to ITD Gateway");
    }

    @Test
    @DisplayName("Verification that SUBMITTED != FILED: filingDate remains null and status is not FILED")
    void testSubmittedIsNotFiled() {
        prepareTestReturn();

        ItrReturnSubmissionResultDto result = itrGovIntegrationService.submitReturn(ItrSubmitReturnRequest.builder()
                .returnId(testReturn.getId())
                .build());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSubmissionStatus()).isEqualTo("SUBMITTED");

        ItrReturnEntity entity = itrReturnRepository.findById(testReturn.getId()).orElseThrow();
        assertThat(entity.getFilingDate()).isNull();
        assertThat(entity.getStatus()).isNotEqualTo(ItrStatus.FILED);
    }

    @Test
    @DisplayName("submitReturn rejects non-prepared return (DOCUMENTS_PENDING status)")
    void testSubmitNonPreparedReturnRejected() {
        // Return is in DOCUMENTS_PENDING (unprepared)
        ItrSubmitReturnRequest request = ItrSubmitReturnRequest.builder()
                .returnId(testReturn.getId())
                .build();

        assertThatThrownBy(() -> itrGovIntegrationService.submitReturn(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException appEx = (AppException) e;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                    assertThat(appEx.getMessage()).contains("Return must be prepared and in READY_TO_FILE status");
                });
    }

    @Test
    @DisplayName("submitReturn is idempotent on already submitted return")
    void testSubmitReturnIdempotencyOnSubmitted() {
        prepareTestReturn();

        // First submission
        ItrReturnSubmissionResultDto firstResult = itrGovIntegrationService.submitReturn(ItrSubmitReturnRequest.builder()
                .returnId(testReturn.getId())
                .build());
        assertThat(firstResult.isSuccess()).isTrue();
        String ackNumber = firstResult.getAcknowledgementNumber();

        // Second submission
        ItrReturnSubmissionResultDto secondResult = itrGovIntegrationService.submitReturn(ItrSubmitReturnRequest.builder()
                .returnId(testReturn.getId())
                .build());

        assertThat(secondResult.isSuccess()).isTrue();
        assertThat(secondResult.getAcknowledgementNumber()).isEqualTo(ackNumber);
    }

    @Test
    @DisplayName("submitReturn handles already FILED return safely without overwrite")
    void testSubmitReturnAlreadyFiledProtection() {
        ItrReturnEntity entity = itrReturnRepository.findById(testReturn.getId()).orElseThrow();
        entity.setStatus(ItrStatus.FILED);
        entity.setAcknowledgementNumber("ITD-ACK-HISTORIC-999");
        entity.setFilingDate(LocalDate.of(2026, 7, 20));
        itrReturnRepository.save(entity);

        ItrReturnSubmissionResultDto result = itrGovIntegrationService.submitReturn(ItrSubmitReturnRequest.builder()
                .returnId(testReturn.getId())
                .build());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSubmissionStatus()).isEqualTo("FILED");
        assertThat(result.getAcknowledgementNumber()).isEqualTo("ITD-ACK-HISTORIC-999");
    }

    @Test
    @DisplayName("submitReturn handles mock AUTH_REQUIRED directive safely")
    void testSubmitReturnAuthRequired() {
        prepareTestReturn();

        ItrReturnSubmissionResultDto result = itrGovIntegrationService.submitReturn(ItrSubmitReturnRequest.builder()
                .returnId(testReturn.getId())
                .options(Map.of("mockOutcome", "AUTH_REQUIRED"))
                .build());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getSubmissionStatus()).isEqualTo("FAILED");
        assertThat(result.getErrorCode()).isEqualTo("AUTH_REQUIRED");
        assertThat(result.getErrorMessage()).contains("expired");
    }

    @Test
    @DisplayName("submitReturn handles mock PROVIDER_UNAVAILABLE, TIMEOUT and RATE_LIMITED")
    void testSubmitReturnTransientFailures() {
        prepareTestReturn();

        ItrReturnSubmissionResultDto res1 = itrGovIntegrationService.submitReturn(ItrSubmitReturnRequest.builder()
                .returnId(testReturn.getId())
                .options(Map.of("mockOutcome", "PROVIDER_UNAVAILABLE"))
                .build());
        assertThat(res1.isSuccess()).isFalse();
        assertThat(res1.getErrorCode()).isEqualTo("PROVIDER_UNAVAILABLE");

        ItrReturnSubmissionResultDto res2 = itrGovIntegrationService.submitReturn(ItrSubmitReturnRequest.builder()
                .returnId(testReturn.getId())
                .options(Map.of("mockOutcome", "TIMEOUT"))
                .build());
        assertThat(res2.isSuccess()).isFalse();
        assertThat(res2.getErrorCode()).isEqualTo("TIMEOUT");

        ItrReturnSubmissionResultDto res3 = itrGovIntegrationService.submitReturn(ItrSubmitReturnRequest.builder()
                .returnId(testReturn.getId())
                .options(Map.of("mockOutcome", "RATE_LIMITED"))
                .build());
        assertThat(res3.isSuccess()).isFalse();
        assertThat(res3.getErrorCode()).isEqualTo("RATE_LIMITED");
    }

    @Test
    @DisplayName("submitReturn handles mock VALIDATION_FAILED directive")
    void testSubmitReturnValidationFailedDirective() {
        prepareTestReturn();

        ItrReturnSubmissionResultDto result = itrGovIntegrationService.submitReturn(ItrSubmitReturnRequest.builder()
                .returnId(testReturn.getId())
                .options(Map.of("mockOutcome", "VALIDATION_FAILED"))
                .build());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getSubmissionStatus()).isEqualTo("FAILED");
        assertThat(result.getErrorCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(result.getErrorMessage()).contains("schema");
    }

    @Test
    @DisplayName("submitReturn handles mock DUPLICATE_SUBMISSION directive and records existing ACK")
    void testSubmitReturnDuplicateSubmissionOutcome() {
        prepareTestReturn();

        ItrReturnSubmissionResultDto result = itrGovIntegrationService.submitReturn(ItrSubmitReturnRequest.builder()
                .returnId(testReturn.getId())
                .options(Map.of("mockOutcome", "DUPLICATE_SUBMISSION"))
                .build());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getSubmissionStatus()).isEqualTo("DUPLICATE_SUBMISSION");
        assertThat(result.getErrorCode()).isEqualTo("DUPLICATE_SUBMISSION");
        assertThat(result.getAcknowledgementNumber()).startsWith("ITD-ACK-DUP-");
    }

    @Test
    @DisplayName("submitReturn enforces tenant isolation: cannot submit return belonging to another organization")
    void testSubmitReturnTenantIsolation() {
        prepareTestReturn();

        // Switch context to other organization
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

        assertThatThrownBy(() -> itrGovIntegrationService.submitReturn(ItrSubmitReturnRequest.builder()
                .returnId(testReturn.getId())
                .build()))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException appEx = (AppException) e;
                    assertThat(appEx.getErrorCode()).isIn(ErrorCode.TENANT_MISMATCH, ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.FORBIDDEN);
                });
    }

    @Test
    @DisplayName("submitReturn rejects unregistered PAN for current tenant")
    void testSubmitReturnUnregisteredPanRejected() {
        assertThatThrownBy(() -> itrGovIntegrationService.submitReturn(ItrSubmitReturnRequest.builder()
                .pan("ZZZZZ9999Z")
                .assessmentYear("2026-27")
                .returnType("ITR1")
                .build()))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException appEx = (AppException) e;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
                });
    }

    @Test
    @DisplayName("submitReturn rejects malformed PAN")
    void testSubmitReturnInvalidPanRejected() {
        assertThatThrownBy(() -> itrGovIntegrationService.submitReturn(ItrSubmitReturnRequest.builder()
                .pan("INVALID_PAN")
                .build()))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException appEx = (AppException) e;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                });
    }

    @Test
    @DisplayName("submitReturn emits audit events with masked PAN and zero credential leakage")
    void testSubmitReturnAuditLogging() {
        prepareTestReturn();

        ItrReturnSubmissionResultDto result = itrGovIntegrationService.submitReturn(ItrSubmitReturnRequest.builder()
                .returnId(testReturn.getId())
                .build());

        assertThat(result.isSuccess()).isTrue();

        ArgumentCaptor<Map<String, Object>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService, atLeastOnce()).logEvent(
                eq(testOrg.getId()),
                any(),
                eq("ITR_RETURN_SUBMITTED"),
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
        assertThat(loggedData).doesNotContainKey("password");
    }

    @Test
    @DisplayName("ItrController POST /api/v1/itr/returns/{id}/submit endpoint processes submission")
    void testControllerSubmitById() {
        prepareTestReturn();

        var response = itrController.submitReturnById(testReturn.getId(), null);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getSubmissionStatus()).isEqualTo("SUBMITTED");
        assertThat(response.getBody().getData().getAcknowledgementNumber()).startsWith("ITD-ACK-");
    }

    @Test
    @DisplayName("ItrController POST /api/v1/itr/returns/submit endpoint processes full request")
    void testControllerSubmitWithBody() {
        prepareTestReturn();

        ItrSubmitReturnRequest request = ItrSubmitReturnRequest.builder()
                .returnId(testReturn.getId())
                .connectionId(itrConn.getId())
                .build();

        var response = itrController.submitReturn(request);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getSubmissionStatus()).isEqualTo("SUBMITTED");
    }
}
