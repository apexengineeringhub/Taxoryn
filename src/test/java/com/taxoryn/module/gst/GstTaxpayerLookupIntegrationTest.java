package com.taxoryn.module.gst;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
import com.taxoryn.module.gov.model.GovCredentialType;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.gst.dto.GstTaxpayerProfileDto;
import com.taxoryn.module.gst.integration.GstGovernmentIntegrationService;
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

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@ActiveProfiles("test")
public class GstTaxpayerLookupIntegrationTest {

    @Autowired
    private GstGovernmentIntegrationService gstGovIntegrationService;

    @Autowired
    private GovernmentConnectionService govConnectionService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @MockBean
    private AuditService auditService;

    private OrganizationEntity testOrg;
    private GovConnectionDto gstConn;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("GST Taxpayer Test Org " + UUID.randomUUID())
                .email("gst.taxpayer." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
        TenantContext.setTenantId(testOrg.getId());

        gstConn = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("Maharashtra GST Gateway")
                .build());

        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(gstConn.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("gst_key_***")
                .rawSecret("SecretGstApiKey987")
                .build());

        govConnectionService.activateConnection(gstConn.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("lookupTaxpayer with valid GSTIN and explicit connection returns full normalized profile")
    void testLookupTaxpayerSuccess() {
        String testGstin = "27AAAPL1234C1ZV";

        GstTaxpayerProfileDto profile = gstGovIntegrationService.lookupTaxpayer(gstConn.getId(), testGstin, null);

        assertThat(profile).isNotNull();
        assertThat(profile.isValid()).isTrue();
        assertThat(profile.getGstin()).isEqualTo(testGstin);
        assertThat(profile.getLegalName()).isEqualTo("Apex Enterprises Private Limited");
        assertThat(profile.getTradeName()).isEqualTo("Apex Solutions");
        assertThat(profile.getStatus()).isEqualTo("ACTIVE");
        assertThat(profile.getRegistrationDate()).isEqualTo("2017-07-01");
        assertThat(profile.getRegistrationType()).isEqualTo("REGULAR");
        assertThat(profile.getStateCode()).isEqualTo("27");
        assertThat(profile.getCenterJurisdiction()).contains("COMMISSIONERATE MUMBAI WEST");
        assertThat(profile.getStateJurisdiction()).contains("MAHARASHTRA");
        assertThat(profile.getConstitutionOfBusiness()).isEqualTo("Private Limited Company");
        assertThat(profile.getAddress()).contains("Bandra Kurla Complex");
        assertThat(profile.getProviderReferenceId()).startsWith("ARN-GST-");
        assertThat(profile.getOperationId()).isNotNull();
        assertThat(profile.getVerifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("lookupTaxpayer without connectionId auto-resolves active tenant GST connection")
    void testLookupTaxpayerAutoResolveConnection() {
        String testGstin = "29BBBPK9999M1Z2";

        GstTaxpayerProfileDto profile = gstGovIntegrationService.lookupTaxpayer(testGstin);

        assertThat(profile).isNotNull();
        assertThat(profile.isValid()).isTrue();
        assertThat(profile.getGstin()).isEqualTo(testGstin);
        assertThat(profile.getStateCode()).isEqualTo("29");
        assertThat(profile.getLegalName()).isNotBlank();
    }

    @Test
    @DisplayName("lookupTaxpayer with invalid GSTIN regex throws VALIDATION_FAILED exception")
    void testLookupTaxpayerInvalidGstinFormat() {
        assertThatThrownBy(() -> gstGovIntegrationService.lookupTaxpayer(gstConn.getId(), "INVALID_GSTIN_123", null))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Invalid GSTIN format");

        assertThatThrownBy(() -> gstGovIntegrationService.lookupTaxpayer(gstConn.getId(), "", null))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    @DisplayName("lookupTaxpayer with NOT_FOUND simulation returns invalid profile with NOT_FOUND error")
    void testLookupTaxpayerNotFoundOutcome() {
        String testGstin = "27AAAPL1234C1ZV";

        GstTaxpayerProfileDto profile = gstGovIntegrationService.lookupTaxpayer(
                gstConn.getId(),
                testGstin,
                Map.of("mockOutcome", "NOT_FOUND")
        );

        assertThat(profile).isNotNull();
        assertThat(profile.isValid()).isFalse();
        assertThat(profile.getGstin()).isEqualTo(testGstin);
        assertThat(profile.getErrorCode()).isEqualTo("NOT_FOUND");
        assertThat(profile.getErrorMessage()).containsIgnoringCase("not found");
    }

    @Test
    @DisplayName("lookupTaxpayer with RATE_LIMITED simulation returns invalid profile with RATE_LIMITED error")
    void testLookupTaxpayerRateLimitedOutcome() {
        String testGstin = "27AAAPL1234C1ZV";

        GstTaxpayerProfileDto profile = gstGovIntegrationService.lookupTaxpayer(
                gstConn.getId(),
                testGstin,
                Map.of("mockOutcome", "RATE_LIMITED")
        );

        assertThat(profile).isNotNull();
        assertThat(profile.isValid()).isFalse();
        assertThat(profile.getErrorCode()).isEqualTo("RATE_LIMITED");
        assertThat(profile.getErrorMessage()).containsIgnoringCase("rate limit");
    }

    @Test
    @DisplayName("lookupTaxpayer with TIMEOUT simulation returns invalid profile with TIMEOUT error")
    void testLookupTaxpayerTimeoutOutcome() {
        String testGstin = "27AAAPL1234C1ZV";

        GstTaxpayerProfileDto profile = gstGovIntegrationService.lookupTaxpayer(
                gstConn.getId(),
                testGstin,
                Map.of("mockOutcome", "TIMEOUT")
        );

        assertThat(profile).isNotNull();
        assertThat(profile.isValid()).isFalse();
        assertThat(profile.getErrorCode()).isEqualTo("TIMEOUT");
        assertThat(profile.getErrorMessage()).containsIgnoringCase("timed out");
    }

    @Test
    @DisplayName("lookupTaxpayer when organization has no GST connection throws VALIDATION_FAILED exception")
    void testLookupTaxpayerNoConnection() {
        OrganizationEntity emptyOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Empty GST Org " + UUID.randomUUID())
                .email("empty.gst." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
        TenantContext.setTenantId(emptyOrg.getId());

        assertThatThrownBy(() -> gstGovIntegrationService.lookupTaxpayer("27AAAPL1234C1ZV"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("No GST government connection configured");
    }
}
