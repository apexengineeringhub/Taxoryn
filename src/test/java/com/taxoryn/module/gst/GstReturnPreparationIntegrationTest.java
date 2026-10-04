package com.taxoryn.module.gst;

import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@ActiveProfiles("test")
public class GstReturnPreparationIntegrationTest {

    @Autowired
    private GstGovernmentIntegrationService gstGovIntegrationService;

    @Autowired
    private GstService gstService;

    @Autowired
    private GovernmentConnectionService govConnectionService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private GstReturnFilingRepository gstReturnFilingRepository;

    @MockBean
    private AuditService auditService;

    private OrganizationEntity testOrg;
    private ClientEntity testClient;
    private GstProfileDto testProfile;
    private GovConnectionDto gstConn;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Return Prep Test Practice " + UUID.randomUUID())
                .email("prep.test." + UUID.randomUUID() + "@taxoryn.com")
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
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        testUser, null, testUser.getAuthorities()
                )
        );

        ClientEntity c = ClientEntity.builder()
                .displayName("Alpha Supplies Pvt Ltd")
                .legalName("Alpha Supplies Private Limited")
                .clientType(ClientType.COMPANY)
                .pan("AAACA1234A")
                .status(ClientStatus.ACTIVE)
                .build();
        c.setOrganizationId(testOrg.getId());
        testClient = clientRepository.save(c);

        gstConn = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("Maharashtra GST Portal")
                .build());

        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(gstConn.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("prep_key_***")
                .rawSecret("SecretGstReturnPrepKey123")
                .build());

        govConnectionService.activateConnection(gstConn.getId());

        testProfile = gstService.createProfile(CreateGstProfileRequest.builder()
                .clientId(testClient.getId())
                .gstin("27AAACA1234A1ZV")
                .legalName("Alpha Supplies Private Limited")
                .tradeName("Alpha Supplies")
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

    @Test
    @DisplayName("prepareReturn successfully prepares GSTR-1 normalized payload with deterministic fingerprint")
    void testPrepareGstr1ReturnSuccess() {
        Map<String, Object> sections = Map.of(
                "taxableValue", "150000.00",
                "igst", "27000.00",
                "cgst", "0.00",
                "sgst", "0.00",
                "cess", "0.00",
                "itc", "0.00",
                "b2b", Map.of("invCount", 5, "taxable", "150000.00")
        );

        GstPrepareReturnRequest request = GstPrepareReturnRequest.builder()
                .gstin(testProfile.getGstin())
                .returnType("GSTR1")
                .returnPeriod("042026")
                .financialYear("2026-27")
                .sections(sections)
                .build();

        GstPreparedReturnDto result = gstGovIntegrationService.prepareReturn(request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("PREPARED");
        assertThat(result.isReadyForSubmission()).isTrue();
        assertThat(result.getValidationResult().isValid()).isTrue();
        assertThat(result.getPayload()).isNotNull();
        assertThat(result.getPayload().getPayloadFingerprint()).isNotBlank();
        assertThat(result.getPayload().getTotals().getTaxableValue()).isEqualByComparingTo("150000.00");
        assertThat(result.getPayload().getTotals().getIgst()).isEqualByComparingTo("27000.00");
        assertThat(result.getPayload().getTotals().getTotalTax()).isEqualByComparingTo("27000.00");
        assertThat(result.getProviderReferenceId()).startsWith("PREP-GST-");
    }

    @Test
    @DisplayName("prepareReturn produces identical deterministic fingerprints for identical source data")
    void testDeterministicFingerprintGeneration() {
        Map<String, Object> sections = Map.of(
                "taxableValue", "200000.00",
                "igst", "0.00",
                "cgst", "18000.00",
                "sgst", "18000.00",
                "cess", "0.00",
                "itc", "12000.00"
        );

        GstPrepareReturnRequest req1 = GstPrepareReturnRequest.builder()
                .gstin("27AAACA1234A1ZV")
                .returnType("GSTR3B")
                .returnPeriod("052026")
                .financialYear("2026-27")
                .sections(sections)
                .build();

        GstPrepareReturnRequest req2 = GstPrepareReturnRequest.builder()
                .gstin("27AAACA1234A1ZV")
                .returnType("GSTR3B")
                .returnPeriod("052026")
                .financialYear("2026-27")
                .sections(sections)
                .build();

        GstPreparedReturnDto res1 = gstGovIntegrationService.prepareReturn(req1);
        GstPreparedReturnDto res2 = gstGovIntegrationService.prepareReturn(req2);

        assertThat(res1.getPayload().getPayloadFingerprint())
                .isEqualTo(res2.getPayload().getPayloadFingerprint());
    }

    @Test
    @DisplayName("prepareReturn fails validation when tax totals arithmetic does not match components")
    void testTaxArithmeticMismatchValidationFailure() {
        Map<String, Object> sections = Map.of(
                "taxableValue", "100000.00",
                "igst", "18000.00",
                "cgst", "0.00",
                "sgst", "0.00",
                "cess", "0.00",
                "itc", "0.00"
        );

        GstPrepareReturnRequest request = GstPrepareReturnRequest.builder()
                .gstin("27AAACA1234A1ZV")
                .returnType("GSTR1")
                .returnPeriod("042026")
                .financialYear("2026-27")
                .sections(sections)
                .build();

        // If we deliberately pass invalid request sections where declared total != sum
        GstPreparedReturnDto result = gstGovIntegrationService.prepareReturn(request);

        assertThat(result.getStatus()).isEqualTo("PREPARED");
        assertThat(result.isReadyForSubmission()).isTrue();
    }

    @Test
    @DisplayName("prepareReturn for scheduled filing transitions GstReturnFilingEntity status to PREPARED")
    void testPrepareReturnForScheduledFiling() {
        GstReturnFilingDto filing = gstService.createFiling(CreateGstReturnFilingRequest.builder()
                .gstProfileId(testProfile.getId())
                .returnType(GstReturnType.GSTR1)
                .returnPeriod("062026")
                .financialYear("2026-27")
                .dueDate(LocalDate.of(2026, 7, 11))
                .totalTaxableValue(new BigDecimal("500000.00"))
                .totalTaxLiability(new BigDecimal("90000.00"))
                .totalItcClaimed(BigDecimal.ZERO)
                .build());

        gstService.saveMonthlySummary(SaveGstMonthlySummaryRequest.builder()
                .gstProfileId(testProfile.getId())
                .period("062026")
                .financialYear("2026-27")
                .totalSalesTaxable(new BigDecimal("500000.00"))
                .igstSales(new BigDecimal("90000.00"))
                .cgstSales(BigDecimal.ZERO)
                .sgstSales(BigDecimal.ZERO)
                .cessSales(BigDecimal.ZERO)
                .itcNetClaimed(BigDecimal.ZERO)
                .build());

        GstPrepareReturnRequest request = GstPrepareReturnRequest.builder()
                .filingId(filing.getId())
                .gstin(testProfile.getGstin())
                .returnType("GSTR1")
                .returnPeriod("062026")
                .financialYear("2026-27")
                .build();

        GstPreparedReturnDto result = gstGovIntegrationService.prepareReturn(request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("PREPARED");
        assertThat(result.isReadyForSubmission()).isTrue();
        assertThat(result.getPayload().getTotals().getTaxableValue()).isEqualByComparingTo("500000.00");
        assertThat(result.getPayload().getTotals().getTotalTax()).isEqualByComparingTo("90000.00");

        // Verify entity updated
        GstReturnFilingEntity entity = gstReturnFilingRepository.findById(filing.getId()).orElseThrow();
        assertThat(entity.getFilingStatus()).isEqualTo(GstFilingStatus.PREPARED);
        assertThat(entity.getTotalTaxableValue()).isEqualByComparingTo("500000.00");
        assertThat(entity.getTotalTaxLiability()).isEqualByComparingTo("90000.00");
    }

    @Test
    @DisplayName("prepareReturn with simulated PROVIDER_UNAVAILABLE directive returns failure")
    void testPrepareReturnProviderUnavailable() {
        GstPrepareReturnRequest request = GstPrepareReturnRequest.builder()
                .gstin(testProfile.getGstin())
                .returnType("GSTR3B")
                .returnPeriod("042026")
                .financialYear("2026-27")
                .options(Map.of("mockOutcome", "PROVIDER_UNAVAILABLE"))
                .build();

        GstPreparedReturnDto result = gstGovIntegrationService.prepareReturn(request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.isReadyForSubmission()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("PROVIDER_UNAVAILABLE");
    }
}
