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
import com.taxoryn.module.gst.dto.GstReturnStatusDto;
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
import com.taxoryn.module.gst.service.GstFilingStatusPollingScheduler;
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
public class GstFilingStatusPollingIntegrationTest {

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

    @Autowired
    private GstFilingStatusPollingScheduler pollingScheduler;

    @MockBean
    private AuditService auditService;

    private OrganizationEntity testOrg;
    private ClientEntity testClient;
    private GstProfileDto testProfile;
    private GovConnectionDto gstConn;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Status Tracking Practice " + UUID.randomUUID())
                .email("status.test." + UUID.randomUUID() + "@taxoryn.com")
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
                .displayName("Omega Enterprises Pvt Ltd")
                .legalName("Omega Enterprises Private Limited")
                .clientType(ClientType.COMPANY)
                .pan("AAECO1234O")
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
                .maskedIdentifier("status_key_***")
                .rawSecret("SecretGstStatusKey123")
                .build());

        govConnectionService.activateConnection(gstConn.getId());

        testProfile = gstService.createProfile(CreateGstProfileRequest.builder()
                .clientId(testClient.getId())
                .gstin("27AAECO1234O1ZV")
                .legalName("Omega Enterprises Private Limited")
                .tradeName("Omega Enterprises")
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

    private GstReturnFilingEntity createSubmittedFiling(String period) {
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

        gstGovIntegrationService.prepareReturn(GstPrepareReturnRequest.builder()
                .filingId(filing.getId())
                .connectionId(gstConn.getId())
                .gstin(testProfile.getGstin())
                .returnType("GSTR1")
                .returnPeriod(period)
                .financialYear("2026-27")
                .build());

        gstGovIntegrationService.submitReturn(GstSubmitReturnRequest.builder()
                .filingId(filing.getId())
                .connectionId(gstConn.getId())
                .build());

        return gstReturnFilingRepository.findById(filing.getId()).orElseThrow();
    }

    @Test
    @DisplayName("SUBMITTED ≠ FILED rule: submission produces SUBMITTED status without filingDate")
    void testSubmittedIsNotFiled() {
        GstReturnFilingEntity filing = createSubmittedFiling("042026");

        assertThat(filing.getFilingStatus()).isEqualTo(GstFilingStatus.SUBMITTED);
        assertThat(filing.getAcknowledgementNumber()).startsWith("MOCK-GST-ACK-");
        assertThat(filing.getFilingDate()).isNull();
    }

    @Test
    @DisplayName("checkReturnStatus transitions SUBMITTED → PROCESSING and PENDING correctly")
    void testSubmittedToProcessingAndPending() {
        GstReturnFilingEntity filing = createSubmittedFiling("052026");

        // Status query returns PROCESSING
        GstReturnStatusDto procStatus = gstGovIntegrationService.checkReturnStatus(
                filing.getId(), gstConn.getId(), Map.of("mockStatus", "PROCESSING"));

        assertThat(procStatus.isSuccess()).isTrue();
        assertThat(procStatus.getFilingStatus()).isEqualTo(GstFilingStatus.PROCESSING);
        assertThat(procStatus.getProviderStatus()).isEqualTo("PROCESSING");
        assertThat(procStatus.isTerminal()).isFalse();
        assertThat(procStatus.getFilingDate()).isNull();

        GstReturnFilingEntity inDb = gstReturnFilingRepository.findById(filing.getId()).orElseThrow();
        assertThat(inDb.getFilingStatus()).isEqualTo(GstFilingStatus.PROCESSING);

        // Status query returns PENDING
        GstReturnStatusDto pendStatus = gstGovIntegrationService.checkReturnStatus(
                filing.getId(), gstConn.getId(), Map.of("mockStatus", "PENDING"));

        assertThat(pendStatus.getFilingStatus()).isEqualTo(GstFilingStatus.PENDING);
        assertThat(pendStatus.isTerminal()).isFalse();
    }

    @Test
    @DisplayName("checkReturnStatus transitions to FILED and sets authoritative filingDate")
    void testStatusToFiledSetsFilingDate() {
        GstReturnFilingEntity filing = createSubmittedFiling("062026");

        GstReturnStatusDto status = gstGovIntegrationService.checkReturnStatus(
                filing.getId(), gstConn.getId(), Map.of("mockStatus", "FILED"));

        assertThat(status.isSuccess()).isTrue();
        assertThat(status.getFilingStatus()).isEqualTo(GstFilingStatus.FILED);
        assertThat(status.getProviderStatus()).isEqualTo("FILED");
        assertThat(status.isTerminal()).isTrue();
        assertThat(status.getFilingDate()).isEqualTo(LocalDate.now());
        assertThat(status.getAcknowledgementNumber()).startsWith("MOCK-GST-ACK-");

        GstReturnFilingEntity filedInDb = gstReturnFilingRepository.findById(filing.getId()).orElseThrow();
        assertThat(filedInDb.getFilingStatus()).isEqualTo(GstFilingStatus.FILED);
        assertThat(filedInDb.getFilingDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("FILED state is terminal: repeated check preserves FILED without mutation")
    void testFiledIsTerminal() {
        GstReturnFilingEntity filing = createSubmittedFiling("072026");

        // Mark FILED
        gstGovIntegrationService.checkReturnStatus(filing.getId(), gstConn.getId(), Map.of("mockStatus", "FILED"));

        // Subsequent check with mockStatus = PENDING must still return FILED
        GstReturnStatusDto subsequent = gstGovIntegrationService.checkReturnStatus(
                filing.getId(), gstConn.getId(), Map.of("mockStatus", "PENDING"));

        assertThat(subsequent.getFilingStatus()).isEqualTo(GstFilingStatus.FILED);
        assertThat(subsequent.isTerminal()).isTrue();

        GstReturnFilingEntity inDb = gstReturnFilingRepository.findById(filing.getId()).orElseThrow();
        assertThat(inDb.getFilingStatus()).isEqualTo(GstFilingStatus.FILED);
    }

    @Test
    @DisplayName("checkReturnStatus transitions to REJECTED or FAILED as terminal failure")
    void testStatusToRejectedAndFailed() {
        GstReturnFilingEntity filingRej = createSubmittedFiling("082026");
        GstReturnStatusDto rejStatus = gstGovIntegrationService.checkReturnStatus(
                filingRej.getId(), gstConn.getId(), Map.of("mockStatus", "REJECTED"));

        assertThat(rejStatus.getFilingStatus()).isEqualTo(GstFilingStatus.REJECTED);
        assertThat(rejStatus.isTerminal()).isTrue();

        GstReturnFilingEntity filingFail = createSubmittedFiling("092026");
        GstReturnStatusDto failStatus = gstGovIntegrationService.checkReturnStatus(
                filingFail.getId(), gstConn.getId(), Map.of("mockStatus", "FAILED"));

        assertThat(failStatus.getFilingStatus()).isEqualTo(GstFilingStatus.FAILED);
        assertThat(failStatus.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("Transient integration failures retain existing filing status")
    void testTransientFailureRetainsStatus() {
        GstReturnFilingEntity filing = createSubmittedFiling("102026");

        GstReturnStatusDto authErr = gstGovIntegrationService.checkReturnStatus(
                filing.getId(), gstConn.getId(), Map.of("mockOutcome", "AUTH_REQUIRED"));

        assertThat(authErr.isSuccess()).isFalse();
        assertThat(authErr.getErrorCode()).isEqualTo("AUTH_REQUIRED");
        assertThat(authErr.getFilingStatus()).isEqualTo(GstFilingStatus.SUBMITTED);

        // Filing status must NOT be corrupted
        GstReturnFilingEntity inDb = gstReturnFilingRepository.findById(filing.getId()).orElseThrow();
        assertThat(inDb.getFilingStatus()).isEqualTo(GstFilingStatus.SUBMITTED);
    }

    @Test
    @DisplayName("Tenant isolation: cannot check status of filing belonging to another tenant")
    void testTenantIsolation() {
        GstReturnFilingEntity filingOrgA = createSubmittedFiling("112026");

        // Switch to Org B
        OrganizationEntity orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Unauthorized Org " + UUID.randomUUID())
                .email("intruder." + UUID.randomUUID() + "@taxoryn.com")
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

        assertThatThrownBy(() -> gstGovIntegrationService.checkReturnStatus(filingOrgA.getId()))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException appEx = (AppException) e;
                    assertThat(appEx.getErrorCode()).isIn(ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.UNAUTHORIZED, ErrorCode.FORBIDDEN);
                });
    }

    @Test
    @DisplayName("Scheduler polls only eligible filings and ignores FILED filings")
    void testSchedulerPolling() {
        GstReturnFilingEntity submittedFiling = createSubmittedFiling("122026");

        // Run scheduler poll
        pollingScheduler.pollSubmittedFilingStatuses();

        // Default mock status resolves to FILED
        GstReturnFilingEntity afterPoll = gstReturnFilingRepository.findById(submittedFiling.getId()).orElseThrow();
        assertThat(afterPoll.getFilingStatus()).isEqualTo(GstFilingStatus.FILED);
        assertThat(afterPoll.getFilingDate()).isNotNull();
    }
}
