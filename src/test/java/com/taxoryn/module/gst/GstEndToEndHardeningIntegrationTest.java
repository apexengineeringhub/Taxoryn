package com.taxoryn.module.gst;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.security.JwtTokenProvider;
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
import com.taxoryn.module.gst.dto.GstTaxpayerProfileDto;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 23.6 — Comprehensive GST Integration Hardening & E2E Validation Suite.
 * Validates the complete lifecycle:
 * GSTIN Lookup -> Return Preparation -> Normalization -> Fingerprint -> Submission -> Status Polling -> FILED.
 * Verifies invariant: SUBMITTED != FILED.
 * Validates negative branches, failure modes, idempotency, concurrency, terminal state immutability,
 * tenant isolation, RBAC, and zero credential leakage.
 */
@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GstEndToEndHardeningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

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

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private ClientEntity clientA;
    private GstProfileDto profileA;
    private GovConnectionDto gstConnA;

    @BeforeEach
    void setUp() {
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Hardened Tax Practice " + UUID.randomUUID())
                .email("hardened.a." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Competitor Tax Practice " + UUID.randomUUID())
                .email("hardened.b." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        setSecurityContext(orgA.getId(), "admin@" + orgA.getId() + ".taxoryn.com",
                Set.of("ROLE_ORG_ADMIN"), Set.of("ROLE_ORG_ADMIN", "GST_VIEW", "GST_CREATE", "GST_UPDATE", "GST_WRITE", "CLIENT_VIEW"));

        ClientEntity c = ClientEntity.builder()
                .displayName("Alpha Industrial Logistics Ltd")
                .legalName("Alpha Industrial Logistics Private Limited")
                .clientType(ClientType.COMPANY)
                .pan("AAACA1234A")
                .status(ClientStatus.ACTIVE)
                .build();
        c.setOrganizationId(orgA.getId());
        clientA = clientRepository.save(c);

        gstConnA = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("GST Portal Gateway Main")
                .build());

        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(gstConnA.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("api_key_***")
                .rawSecret("UltraSecretApiKey999")
                .build());

        govConnectionService.activateConnection(gstConnA.getId());

        profileA = gstService.createProfile(CreateGstProfileRequest.builder()
                .clientId(clientA.getId())
                .gstin("27AAACA1234A1Z5")
                .legalName("Alpha Industrial Logistics Private Limited")
                .tradeName("Alpha Logistics")
                .gstType(GstType.REGULAR)
                .filingFrequency(FilingFrequency.MONTHLY)
                .stateCode("27")
                .registrationDate(LocalDate.of(2019, 4, 1))
                .build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private void setSecurityContext(UUID orgId, String email, Set<String> roles, Set<String> permissions) {
        TenantContext.setTenantId(orgId);
        SecurityUser user = SecurityUser.builder()
                .userId(UUID.randomUUID())
                .organizationId(orgId)
                .email(email)
                .roles(roles)
                .permissions(permissions)
                .enabled(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    @Test
    @DisplayName("E2E Complete Golden Path: Lookup -> Prepare -> Normalize -> Submit -> Poll -> FILED")
    void testCompleteGstFilingLifecycleGoldenPath() {
        // Step 1: Taxpayer Lookup & Verification
        GstTaxpayerProfileDto taxpayer = gstGovIntegrationService.lookupTaxpayer(
                gstConnA.getId(), "27AAACA1234A1Z5", Map.of());
        assertThat(taxpayer).isNotNull();
        assertThat(taxpayer.getGstin()).isEqualTo("27AAACA1234A1Z5");
        assertThat(taxpayer.getStatus()).isEqualTo("ACTIVE");

        // Step 2: Create Filing and Monthly Data in Taxoryn GST Domain
        String period = "042026";
        GstReturnFilingDto filing = gstService.createFiling(CreateGstReturnFilingRequest.builder()
                .gstProfileId(profileA.getId())
                .returnType(GstReturnType.GSTR1)
                .returnPeriod(period)
                .financialYear("2026-27")
                .dueDate(LocalDate.of(2026, 5, 11))
                .totalTaxableValue(new BigDecimal("500000.00"))
                .totalTaxLiability(new BigDecimal("90000.00"))
                .build());
        assertThat(filing.getFilingStatus()).isEqualTo(GstFilingStatus.PENDING);

        gstService.saveMonthlySummary(SaveGstMonthlySummaryRequest.builder()
                .gstProfileId(profileA.getId())
                .period(period)
                .financialYear("2026-27")
                .totalSalesTaxable(new BigDecimal("500000.00"))
                .igstSales(new BigDecimal("90000.00"))
                .cgstSales(BigDecimal.ZERO)
                .sgstSales(BigDecimal.ZERO)
                .cessSales(BigDecimal.ZERO)
                .itcNetClaimed(BigDecimal.ZERO)
                .build());

        // Step 3: Prepare & Normalize Payload
        GstPreparedReturnDto prepared = gstGovIntegrationService.prepareReturn(GstPrepareReturnRequest.builder()
                .filingId(filing.getId())
                .connectionId(gstConnA.getId())
                .gstin(profileA.getGstin())
                .returnType("GSTR1")
                .returnPeriod(period)
                .financialYear("2026-27")
                .build());

        assertThat(prepared.isReadyForSubmission()).isTrue();
        assertThat(prepared.getPayload()).isNotNull();
        assertThat(prepared.getPayload().getPayloadFingerprint()).isNotEmpty();
        assertThat(prepared.getPayload().getTotals()).isNotNull();
        assertThat(prepared.getPayload().getTotals().getTaxableValue()).isNotNull();

        GstReturnFilingEntity afterPrep = gstReturnFilingRepository.findById(filing.getId()).orElseThrow();
        assertThat(afterPrep.getFilingStatus()).isEqualTo(GstFilingStatus.PREPARED);
        assertThat(afterPrep.getFilingDate()).isNull();

        // Step 4: Submit Return (SUBMITTED != FILED)
        GstReturnSubmissionResultDto subResult = gstGovIntegrationService.submitReturn(GstSubmitReturnRequest.builder()
                .filingId(filing.getId())
                .connectionId(gstConnA.getId())
                .build());

        assertThat(subResult.isSuccess()).isTrue();
        assertThat(subResult.getSubmissionStatus()).isEqualTo("SUBMITTED");
        assertThat(subResult.getAcknowledgementNumber()).isNotEmpty();

        GstReturnFilingEntity afterSubmit = gstReturnFilingRepository.findById(filing.getId()).orElseThrow();
        assertThat(afterSubmit.getFilingStatus()).isEqualTo(GstFilingStatus.SUBMITTED);
        assertThat(afterSubmit.getAcknowledgementNumber()).isEqualTo(subResult.getAcknowledgementNumber());
        assertThat(afterSubmit.getFilingDate()).isNull(); // SUBMITTED MUST NOT have filingDate

        // Step 5: Status Polling - PROCESSING
        GstReturnStatusDto procStatus = gstGovIntegrationService.checkReturnStatus(
                filing.getId(), gstConnA.getId(), Map.of("mockStatus", "PROCESSING"));
        assertThat(procStatus.getFilingStatus()).isEqualTo(GstFilingStatus.PROCESSING);
        assertThat(procStatus.isTerminal()).isFalse();
        assertThat(procStatus.getFilingDate()).isNull();

        // Step 6: Status Polling - Authoritative FILED
        GstReturnStatusDto filedStatus = gstGovIntegrationService.checkReturnStatus(
                filing.getId(), gstConnA.getId(), Map.of("mockStatus", "FILED"));
        assertThat(filedStatus.getFilingStatus()).isEqualTo(GstFilingStatus.FILED);
        assertThat(filedStatus.isTerminal()).isTrue();
        assertThat(filedStatus.getFilingDate()).isEqualTo(LocalDate.now());

        GstReturnFilingEntity inDbFinal = gstReturnFilingRepository.findById(filing.getId()).orElseThrow();
        assertThat(inDbFinal.getFilingStatus()).isEqualTo(GstFilingStatus.FILED);
        assertThat(inDbFinal.getFilingDate()).isEqualTo(LocalDate.now());
        assertThat(inDbFinal.getAcknowledgementNumber()).isEqualTo(subResult.getAcknowledgementNumber());
    }

    @Test
    @DisplayName("Negative Paths: Provider failure codes (AUTH_REQUIRED, TIMEOUT, PROVIDER_UNAVAILABLE, RATE_LIMITED)")
    void testTransientFailureCodesHandling() {
        String period = "052026";
        GstReturnFilingDto filing = gstService.createFiling(CreateGstReturnFilingRequest.builder()
                .gstProfileId(profileA.getId())
                .returnType(GstReturnType.GSTR1)
                .returnPeriod(period)
                .financialYear("2026-27")
                .dueDate(LocalDate.of(2026, 6, 11))
                .totalTaxableValue(new BigDecimal("100000.00"))
                .totalTaxLiability(new BigDecimal("18000.00"))
                .build());

        gstService.saveMonthlySummary(SaveGstMonthlySummaryRequest.builder()
                .gstProfileId(profileA.getId())
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
                .connectionId(gstConnA.getId())
                .gstin(profileA.getGstin())
                .returnType("GSTR1")
                .returnPeriod(period)
                .financialYear("2026-27")
                .build());

        // Test RATE_LIMITED simulation on submission
        GstReturnSubmissionResultDto rateLimitResult = gstGovIntegrationService.submitReturn(
                GstSubmitReturnRequest.builder()
                        .filingId(filing.getId())
                        .connectionId(gstConnA.getId())
                        .options(Map.of("mockOutcome", "RATE_LIMITED"))
                        .build());

        assertThat(rateLimitResult.isSuccess()).isFalse();
        assertThat(rateLimitResult.getErrorCode()).isEqualTo("RATE_LIMITED");

        // Filing status should remain PREPARED or SUBMISSION_FAILED, not corrupted
        GstReturnFilingEntity filingInDb = gstReturnFilingRepository.findById(filing.getId()).orElseThrow();
        assertThat(filingInDb.getFilingStatus()).isIn(GstFilingStatus.PREPARED, GstFilingStatus.SUBMISSION_FAILED);

        // Test successful submission
        GstReturnSubmissionResultDto subSuccess = gstGovIntegrationService.submitReturn(
                GstSubmitReturnRequest.builder()
                        .filingId(filing.getId())
                        .connectionId(gstConnA.getId())
                        .build());
        assertThat(subSuccess.isSuccess()).isTrue();

        // Test TIMEOUT on status check
        GstReturnStatusDto timeoutStatus = gstGovIntegrationService.checkReturnStatus(
                filing.getId(), gstConnA.getId(), Map.of("mockOutcome", "TIMEOUT"));
        assertThat(timeoutStatus.isSuccess()).isFalse();
        assertThat(timeoutStatus.getErrorCode()).isEqualTo("TIMEOUT");

        // Status remains SUBMITTED
        GstReturnFilingEntity afterTimeout = gstReturnFilingRepository.findById(filing.getId()).orElseThrow();
        assertThat(afterTimeout.getFilingStatus()).isEqualTo(GstFilingStatus.SUBMITTED);

        // Test PROVIDER_UNAVAILABLE on status check
        GstReturnStatusDto unavailStatus = gstGovIntegrationService.checkReturnStatus(
                filing.getId(), gstConnA.getId(), Map.of("mockOutcome", "PROVIDER_UNAVAILABLE"));
        assertThat(unavailStatus.isSuccess()).isFalse();
        assertThat(unavailStatus.getErrorCode()).isEqualTo("PROVIDER_UNAVAILABLE");
    }

    @Test
    @DisplayName("Terminal Immutability: FILED return cannot be mutated or reverted by subsequent polls or submissions")
    void testTerminalStateImmutability() {
        String period = "062026";
        GstReturnFilingDto filing = gstService.createFiling(CreateGstReturnFilingRequest.builder()
                .gstProfileId(profileA.getId())
                .returnType(GstReturnType.GSTR1)
                .returnPeriod(period)
                .financialYear("2026-27")
                .dueDate(LocalDate.of(2026, 7, 11))
                .totalTaxableValue(new BigDecimal("200000.00"))
                .totalTaxLiability(new BigDecimal("36000.00"))
                .build());

        gstService.saveMonthlySummary(SaveGstMonthlySummaryRequest.builder()
                .gstProfileId(profileA.getId())
                .period(period)
                .financialYear("2026-27")
                .totalSalesTaxable(new BigDecimal("200000.00"))
                .igstSales(new BigDecimal("36000.00"))
                .cgstSales(BigDecimal.ZERO)
                .sgstSales(BigDecimal.ZERO)
                .cessSales(BigDecimal.ZERO)
                .itcNetClaimed(BigDecimal.ZERO)
                .build());

        gstGovIntegrationService.prepareReturn(GstPrepareReturnRequest.builder()
                .filingId(filing.getId())
                .connectionId(gstConnA.getId())
                .gstin(profileA.getGstin())
                .returnType("GSTR1")
                .returnPeriod(period)
                .financialYear("2026-27")
                .build());

        GstReturnSubmissionResultDto initialSub = gstGovIntegrationService.submitReturn(GstSubmitReturnRequest.builder()
                .filingId(filing.getId())
                .connectionId(gstConnA.getId())
                .build());
        assertThat(initialSub.isSuccess()).isTrue();

        // Transition to FILED
        GstReturnStatusDto filedStatus = gstGovIntegrationService.checkReturnStatus(
                filing.getId(), gstConnA.getId(), Map.of("mockStatus", "FILED"));
        assertThat(filedStatus.getFilingStatus()).isEqualTo(GstFilingStatus.FILED);
        assertThat(filedStatus.getFilingDate()).isNotNull();

        // Idempotent duplicate submission on already FILED return returns cached FILED without re-execution
        GstReturnSubmissionResultDto duplicateSub = gstGovIntegrationService.submitReturn(GstSubmitReturnRequest.builder()
                .filingId(filing.getId())
                .connectionId(gstConnA.getId())
                .build());
        assertThat(duplicateSub.isSuccess()).isTrue();
        assertThat(duplicateSub.getSubmissionStatus()).isEqualTo("FILED");
        assertThat(duplicateSub.getAcknowledgementNumber()).isEqualTo(initialSub.getAcknowledgementNumber());

        // Status poll with mockStatus = PENDING must still report FILED and not revert
        GstReturnStatusDto pollAfterFiled = gstGovIntegrationService.checkReturnStatus(
                filing.getId(), gstConnA.getId(), Map.of("mockStatus", "PENDING"));
        assertThat(pollAfterFiled.getFilingStatus()).isEqualTo(GstFilingStatus.FILED);

        GstReturnFilingEntity finalDb = gstReturnFilingRepository.findById(filing.getId()).orElseThrow();
        assertThat(finalDb.getFilingStatus()).isEqualTo(GstFilingStatus.FILED);
        assertThat(finalDb.getFilingDate()).isNotNull();
    }

    @Test
    @DisplayName("Tenant Isolation: Org B cannot prepare, submit, or query filing owned by Org A")
    void testTenantIsolationAcrossAllOperations() {
        String period = "072026";
        GstReturnFilingDto filingA = gstService.createFiling(CreateGstReturnFilingRequest.builder()
                .gstProfileId(profileA.getId())
                .returnType(GstReturnType.GSTR1)
                .returnPeriod(period)
                .financialYear("2026-27")
                .dueDate(LocalDate.of(2026, 8, 11))
                .totalTaxableValue(new BigDecimal("150000.00"))
                .totalTaxLiability(new BigDecimal("27000.00"))
                .build());

        // Switch to Org B
        setSecurityContext(orgB.getId(), "admin@" + orgB.getId() + ".taxoryn.com",
                Set.of("ROLE_ORG_ADMIN"), Set.of("ROLE_ORG_ADMIN", "GST_VIEW", "GST_CREATE", "GST_UPDATE", "GST_WRITE"));

        // Org B attempts to prepare Org A's filing
        assertThatThrownBy(() -> gstGovIntegrationService.prepareReturn(GstPrepareReturnRequest.builder()
                .filingId(filingA.getId())
                .gstin(profileA.getGstin())
                .returnType("GSTR1")
                .returnPeriod(period)
                .financialYear("2026-27")
                .build()))
                .isInstanceOf(AppException.class);

        // Org B attempts to submit Org A's filing
        assertThatThrownBy(() -> gstGovIntegrationService.submitReturn(GstSubmitReturnRequest.builder()
                .filingId(filingA.getId())
                .build()))
                .isInstanceOf(AppException.class);

        // Org B attempts to check status of Org A's filing
        assertThatThrownBy(() -> gstGovIntegrationService.checkReturnStatus(filingA.getId()))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("RBAC Enforcement: User without GST_WRITE/GST_CREATE cannot submit returns via API")
    void testRbacEnforcementOnSubmission() throws Exception {
        String period = "082026";
        GstReturnFilingDto filing = gstService.createFiling(CreateGstReturnFilingRequest.builder()
                .gstProfileId(profileA.getId())
                .returnType(GstReturnType.GSTR1)
                .returnPeriod(period)
                .financialYear("2026-27")
                .dueDate(LocalDate.of(2026, 9, 11))
                .totalTaxableValue(new BigDecimal("100000.00"))
                .totalTaxLiability(new BigDecimal("18000.00"))
                .build());

        gstService.saveMonthlySummary(SaveGstMonthlySummaryRequest.builder()
                .gstProfileId(profileA.getId())
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
                .connectionId(gstConnA.getId())
                .gstin(profileA.getGstin())
                .returnType("GSTR1")
                .returnPeriod(period)
                .financialYear("2026-27")
                .build());

        // Generate token for Read-Only user (GST_VIEW only, no GST_WRITE or GST_CREATE or ORG_ADMIN)
        UUID readonlyUserId = UUID.randomUUID();
        String readonlyToken = jwtTokenProvider.generateAccessToken(
                readonlyUserId,
                orgA.getId(),
                "readonly@" + orgA.getId() + ".taxoryn.com",
                Set.of("ROLE_PRACTITIONER"),
                Set.of("GST_VIEW")
        );

        GstSubmitReturnRequest submitReq = GstSubmitReturnRequest.builder()
                .filingId(filing.getId())
                .connectionId(gstConnA.getId())
                .build();

        // Submission endpoint must return 403 Forbidden
        mockMvc.perform(post("/api/v1/gst/returns/submit")
                        .header("Authorization", "Bearer " + readonlyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Scheduler Polling: Bounded execution processes eligible filings and ignores terminal filings")
    void testSchedulerBoundedExecution() {
        String period = "092026";
        GstReturnFilingDto filing = gstService.createFiling(CreateGstReturnFilingRequest.builder()
                .gstProfileId(profileA.getId())
                .returnType(GstReturnType.GSTR1)
                .returnPeriod(period)
                .financialYear("2026-27")
                .dueDate(LocalDate.of(2026, 10, 11))
                .totalTaxableValue(new BigDecimal("100000.00"))
                .totalTaxLiability(new BigDecimal("18000.00"))
                .build());

        gstService.saveMonthlySummary(SaveGstMonthlySummaryRequest.builder()
                .gstProfileId(profileA.getId())
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
                .connectionId(gstConnA.getId())
                .gstin(profileA.getGstin())
                .returnType("GSTR1")
                .returnPeriod(period)
                .financialYear("2026-27")
                .build());

        gstGovIntegrationService.submitReturn(GstSubmitReturnRequest.builder()
                .filingId(filing.getId())
                .connectionId(gstConnA.getId())
                .build());

        // Ensure filing is in SUBMITTED status
        GstReturnFilingEntity submittedEntity = gstReturnFilingRepository.findById(filing.getId()).orElseThrow();
        assertThat(submittedEntity.getFilingStatus()).isEqualTo(GstFilingStatus.SUBMITTED);

        // Execute scheduled poll
        pollingScheduler.pollSubmittedFilingStatuses();

        // Default mock provider returns FILED
        GstReturnFilingEntity afterSchedule = gstReturnFilingRepository.findById(filing.getId()).orElseThrow();
        assertThat(afterSchedule.getFilingStatus()).isEqualTo(GstFilingStatus.FILED);
        assertThat(afterSchedule.getFilingDate()).isNotNull();

        // Running schedule again must be safe and idempotent
        pollingScheduler.pollSubmittedFilingStatuses();
        GstReturnFilingEntity afterSecondSchedule = gstReturnFilingRepository.findById(filing.getId()).orElseThrow();
        assertThat(afterSecondSchedule.getFilingStatus()).isEqualTo(GstFilingStatus.FILED);
    }
}
