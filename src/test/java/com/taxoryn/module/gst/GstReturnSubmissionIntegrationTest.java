package com.taxoryn.module.gst;

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
import com.taxoryn.module.gst.dto.CreateGstProfileRequest;
import com.taxoryn.module.gst.dto.CreateGstReturnFilingRequest;
import com.taxoryn.module.gst.dto.GstPrepareReturnRequest;
import com.taxoryn.module.gst.dto.GstPreparedReturnDto;
import com.taxoryn.module.gst.dto.GstProfileDto;
import com.taxoryn.module.gst.dto.GstReturnFilingDto;
import com.taxoryn.module.gst.dto.GstReturnSubmissionResultDto;
import com.taxoryn.module.gst.dto.GstSubmitReturnRequest;
import com.taxoryn.module.gst.dto.SaveGstMonthlySummaryRequest;
import com.taxoryn.module.gst.entity.GstProfileEntity.FilingFrequency;
import com.taxoryn.module.gst.entity.GstProfileEntity.GstType;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstReturnType;
import com.taxoryn.module.gst.integration.GstGovernmentIntegrationService;
import com.taxoryn.module.gst.repository.GstReturnFilingRepository;
import com.taxoryn.module.gst.service.GstService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@ActiveProfiles("test")
public class GstReturnSubmissionIntegrationTest {

    @Autowired
    private GstGovernmentIntegrationService gstGovIntegrationService;

    @Autowired
    private GstService gstService;

    @Autowired
    private GstReturnFilingRepository gstReturnFilingRepository;

    @Autowired
    private GovernmentConnectionService govConnectionService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ClientRepository clientRepository;

    @MockBean
    private AuditService auditService;

    private OrganizationEntity testOrg;
    private ClientEntity testClient;
    private GstProfileDto testProfile;
    private GovConnectionDto gstConn;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Return Submission Practice " + UUID.randomUUID())
                .email("sub.test." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
        TenantContext.setTenantId(testOrg.getId());

        SecurityUser testUser = SecurityUser.builder()
                .userId(UUID.randomUUID())
                .organizationId(testOrg.getId())
                .email("admin@" + testOrg.getId() + ".taxoryn.com")
                .roles(Set.of("ROLE_ORG_ADMIN"))
                .permissions(Set.of("ROLE_ORG_ADMIN", "GST_VIEW", "GST_CREATE", "GST_UPDATE", "GST_WRITE", "CLIENT_VIEW"))
                .enabled(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities())
        );

        ClientEntity c = ClientEntity.builder()
                .displayName("Delta Dynamics Pvt Ltd")
                .legalName("Delta Dynamics Private Limited")
                .clientType(ClientType.COMPANY)
                .pan("AADCD1234D")
                .status(ClientStatus.ACTIVE)
                .build();
        c.setOrganizationId(testOrg.getId());
        testClient = clientRepository.save(c);

        gstConn = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("Maharashtra GST Gateway")
                .build());

        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(gstConn.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("sub_key_***")
                .rawSecret("SecretGstSubmissionKey123")
                .build());

        govConnectionService.activateConnection(gstConn.getId());

        testProfile = gstService.createProfile(CreateGstProfileRequest.builder()
                .clientId(testClient.getId())
                .gstin("27AADCD1234D1ZV")
                .legalName("Delta Dynamics Private Limited")
                .tradeName("Delta Dynamics")
                .gstType(GstType.REGULAR)
                .filingFrequency(FilingFrequency.MONTHLY)
                .stateCode("27")
                .registrationDate(LocalDate.of(2020, 1, 1))
                .build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private GstReturnFilingDto createAndPrepareFiling(String period) {
        GstReturnFilingDto filing = gstService.createFiling(CreateGstReturnFilingRequest.builder()
                .gstProfileId(testProfile.getId())
                .returnType(GstReturnType.GSTR1)
                .returnPeriod(period)
                .financialYear("2026-27")
                .dueDate(LocalDate.of(2026, 7, 11))
                .totalTaxableValue(new BigDecimal("100000.00"))
                .totalTaxLiability(new BigDecimal("18000.00"))
                .build());

        gstService.saveMonthlySummary(SaveGstMonthlySummaryRequest.builder()
                .gstProfileId(testProfile.getId())
                .period(period)
                .financialYear("2026-27")
                .totalSalesTaxable(new BigDecimal("100000.00"))
                .igstSales(new BigDecimal("18000.00"))
                .cgstSales(BigDecimal.ZERO)
                .sgstSales(BigDecimal.ZERO)
                .cessSales(BigDecimal.ZERO)
                .itcNetClaimed(BigDecimal.ZERO)
                .build());

        GstPreparedReturnDto prepResult = gstGovIntegrationService.prepareReturn(GstPrepareReturnRequest.builder()
                .filingId(filing.getId())
                .connectionId(gstConn.getId())
                .gstin(testProfile.getGstin())
                .returnType("GSTR1")
                .returnPeriod(period)
                .financialYear("2026-27")
                .build());

        assertThat(prepResult.isReadyForSubmission()).isTrue();
        return filing;
    }

    @Test
    @DisplayName("submitReturn successfully submits prepared return and records deterministic mock acknowledgement")
    void testSubmitPreparedReturnSuccess() {
        GstReturnFilingDto filing = createAndPrepareFiling("062026");

        GstReturnSubmissionResultDto result = gstGovIntegrationService.submitReturn(GstSubmitReturnRequest.builder()
                .filingId(filing.getId())
                .connectionId(gstConn.getId())
                .build());

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSubmissionStatus()).isEqualTo("SUBMITTED");
        assertThat(result.getAcknowledgementNumber()).startsWith("MOCK-GST-ACK-");
        assertThat(result.getProviderReference()).startsWith("REF-GST-SUB-");
        assertThat(result.getOperationId()).isNotNull();
        assertThat(result.getSubmittedAt()).isNotNull();

        // Verify Filing Entity state in DB: SUBMITTED != FILED
        GstReturnFilingEntity updatedFiling = gstReturnFilingRepository.findById(filing.getId()).orElseThrow();
        assertThat(updatedFiling.getFilingStatus()).isEqualTo(GstFilingStatus.SUBMITTED);
        assertThat(updatedFiling.getAcknowledgementNumber()).isEqualTo(result.getAcknowledgementNumber());
    }

    @Test
    @DisplayName("submitReturn rejects non-prepared return (PENDING status)")
    void testSubmitNonPreparedReturnRejected() {
        GstReturnFilingDto filing = gstService.createFiling(CreateGstReturnFilingRequest.builder()
                .gstProfileId(testProfile.getId())
                .returnType(GstReturnType.GSTR3B)
                .returnPeriod("072026")
                .financialYear("2026-27")
                .dueDate(LocalDate.of(2026, 8, 20))
                .build());

        assertThatThrownBy(() -> gstGovIntegrationService.submitReturn(GstSubmitReturnRequest.builder()
                .filingId(filing.getId())
                .connectionId(gstConn.getId())
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Return must be PREPARED before submission");
    }

    @Test
    @DisplayName("submitReturn is idempotent on already FILED returns")
    void testSubmitReturnIdempotency() {
        GstReturnFilingDto filing = createAndPrepareFiling("082026");

        // First submission
        GstReturnSubmissionResultDto firstResult = gstGovIntegrationService.submitReturn(GstSubmitReturnRequest.builder()
                .filingId(filing.getId())
                .connectionId(gstConn.getId())
                .build());

        assertThat(firstResult.isSuccess()).isTrue();
        String initialAck = firstResult.getAcknowledgementNumber();

        // Second submission of the same filing
        GstReturnSubmissionResultDto secondResult = gstGovIntegrationService.submitReturn(GstSubmitReturnRequest.builder()
                .filingId(filing.getId())
                .connectionId(gstConn.getId())
                .build());

        assertThat(secondResult.isSuccess()).isTrue();
        assertThat(secondResult.getAcknowledgementNumber()).isEqualTo(initialAck);
    }

    @Test
    @DisplayName("submitReturn handles mock AUTH_REQUIRED error safely")
    void testSubmitReturnAuthRequired() {
        GstReturnFilingDto filing = createAndPrepareFiling("092026");

        GstReturnSubmissionResultDto result = gstGovIntegrationService.submitReturn(GstSubmitReturnRequest.builder()
                .filingId(filing.getId())
                .connectionId(gstConn.getId())
                .options(Map.of("mockOutcome", "AUTH_REQUIRED"))
                .build());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getSubmissionStatus()).isEqualTo("FAILED");
        assertThat(result.getErrorCode()).isEqualTo("AUTH_REQUIRED");
        assertThat(result.getErrorMessage()).contains("expired");

        // Filing should transition to SUBMISSION_FAILED
        GstReturnFilingEntity updatedFiling = gstReturnFilingRepository.findById(filing.getId()).orElseThrow();
        assertThat(updatedFiling.getFilingStatus()).isEqualTo(GstFilingStatus.SUBMISSION_FAILED);
    }

    @Test
    @DisplayName("submitReturn handles mock PROVIDER_UNAVAILABLE, TIMEOUT and RATE_LIMITED")
    void testSubmitReturnTransientFailures() {
        GstReturnFilingDto filing1 = createAndPrepareFiling("102026");
        GstReturnSubmissionResultDto res1 = gstGovIntegrationService.submitReturn(GstSubmitReturnRequest.builder()
                .filingId(filing1.getId())
                .options(Map.of("mockOutcome", "PROVIDER_UNAVAILABLE"))
                .build());
        assertThat(res1.isSuccess()).isFalse();
        assertThat(res1.getErrorCode()).isEqualTo("PROVIDER_UNAVAILABLE");

        GstReturnFilingDto filing2 = createAndPrepareFiling("112026");
        GstReturnSubmissionResultDto res2 = gstGovIntegrationService.submitReturn(GstSubmitReturnRequest.builder()
                .filingId(filing2.getId())
                .options(Map.of("mockOutcome", "TIMEOUT"))
                .build());
        assertThat(res2.isSuccess()).isFalse();
        assertThat(res2.getErrorCode()).isEqualTo("TIMEOUT");

        GstReturnFilingDto filing3 = createAndPrepareFiling("122026");
        GstReturnSubmissionResultDto res3 = gstGovIntegrationService.submitReturn(GstSubmitReturnRequest.builder()
                .filingId(filing3.getId())
                .options(Map.of("mockOutcome", "RATE_LIMITED"))
                .build());
        assertThat(res3.isSuccess()).isFalse();
        assertThat(res3.getErrorCode()).isEqualTo("RATE_LIMITED");
    }

    @Test
    @DisplayName("submitReturn handles DUPLICATE_SUBMISSION mock outcome and sets existing ack")
    void testSubmitReturnDuplicateSubmission() {
        GstReturnFilingDto filing = createAndPrepareFiling("012027");

        GstReturnSubmissionResultDto result = gstGovIntegrationService.submitReturn(GstSubmitReturnRequest.builder()
                .filingId(filing.getId())
                .options(Map.of("mockOutcome", "DUPLICATE_SUBMISSION"))
                .build());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getSubmissionStatus()).isEqualTo("DUPLICATE_SUBMISSION");
        assertThat(result.getErrorCode()).isEqualTo("DUPLICATE_SUBMISSION");
        assertThat(result.getAcknowledgementNumber()).startsWith("MOCK-GST-ACK-DUP-");
    }

    @Test
    @DisplayName("submitReturn enforces tenant isolation: cannot submit filing from another org")
    void testSubmitReturnTenantIsolation() {
        GstReturnFilingDto filingOrgA = createAndPrepareFiling("022027");

        // Switch to Org B
        OrganizationEntity orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Unauthorized Practice " + UUID.randomUUID())
                .email("hacker." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
        TenantContext.setTenantId(orgB.getId());

        SecurityUser userB = SecurityUser.builder()
                .userId(UUID.randomUUID())
                .organizationId(orgB.getId())
                .email("admin@" + orgB.getId() + ".taxoryn.com")
                .roles(Set.of("ROLE_ORG_ADMIN"))
                .permissions(Set.of("ROLE_ORG_ADMIN", "GST_VIEW", "GST_CREATE", "GST_UPDATE", "GST_WRITE"))
                .enabled(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userB, null, userB.getAuthorities())
        );

        assertThatThrownBy(() -> gstGovIntegrationService.submitReturn(GstSubmitReturnRequest.builder()
                .filingId(filingOrgA.getId())
                .build()))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException appEx = (AppException) e;
                    assertThat(appEx.getErrorCode()).isIn(ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.UNAUTHORIZED, ErrorCode.FORBIDDEN);
                });
    }
}
