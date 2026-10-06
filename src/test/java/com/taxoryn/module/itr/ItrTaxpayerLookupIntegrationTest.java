package com.taxoryn.module.itr;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
import com.taxoryn.module.gov.model.GovCredentialType;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.itr.dto.ItrTaxpayerProfileDto;
import com.taxoryn.module.itr.integration.ItrGovernmentIntegrationService;
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
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@ActiveProfiles("test")
public class ItrTaxpayerLookupIntegrationTest {

    @Autowired
    private ItrGovernmentIntegrationService itrGovIntegrationService;

    @Autowired
    private GovernmentConnectionService govConnectionService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @MockBean
    private AuditService auditService;

    private OrganizationEntity testOrg;
    private GovConnectionDto itrConn;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("ITR Taxpayer Test Org " + UUID.randomUUID())
                .email("itr.taxpayer." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
        TenantContext.setTenantId(testOrg.getId());

        itrConn = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.INCOME_TAX)
                .displayName("Income Tax Department Gateway")
                .build());

        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(itrConn.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("itd_key_***")
                .rawSecret("SecretItdApiKey987")
                .build());

        govConnectionService.activateConnection(itrConn.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("lookupTaxpayer with valid PAN and explicit connection returns full normalized profile")
    void testLookupTaxpayerSuccess() {
        String testPan = "ABCDE1234F";

        ItrTaxpayerProfileDto profile = itrGovIntegrationService.lookupTaxpayer(itrConn.getId(), testPan, null);

        assertThat(profile).isNotNull();
        assertThat(profile.isValid()).isTrue();
        assertThat(profile.isVerified()).isTrue();
        assertThat(profile.getPan()).isEqualTo(testPan);
        assertThat(profile.getTaxpayerName()).isEqualTo("Apex Enterprise Solutions");
        assertThat(profile.getStatus()).isEqualTo("ACTIVE");
        assertThat(profile.getPanStatus()).isEqualTo("OPERATIVE");
        assertThat(profile.getCategory()).isEqualTo("COMPANY");
        assertThat(profile.getAadhaarSeedingStatus()).isEqualTo("LINKED");
        assertThat(profile.getJurisdictionAssessingOfficer()).contains("WARD 12(1)");
        assertThat(profile.getProviderReferenceId()).startsWith("ITD-PAN-ACK-");
        assertThat(profile.getOperationId()).isNotNull();
        assertThat(profile.getVerifiedAt()).isNotNull();
        assertThat(profile.getRawData()).isNotNull();
        assertThat(profile.getRawData()).containsKey("adapterCode");
    }

    @Test
    @DisplayName("lookupTaxpayer without connectionId auto-resolves active tenant Income Tax connection")
    void testLookupTaxpayerAutoResolveConnection() {
        String testPan = "XYZPA9999Z";

        ItrTaxpayerProfileDto profile = itrGovIntegrationService.lookupTaxpayer(testPan);

        assertThat(profile).isNotNull();
        assertThat(profile.isValid()).isTrue();
        assertThat(profile.isVerified()).isTrue();
        assertThat(profile.getPan()).isEqualTo(testPan);
        assertThat(profile.getTaxpayerName()).isNotBlank();
    }

    @Test
    @DisplayName("lookupTaxpayer normalizes lowercase PAN to uppercase")
    void testLookupTaxpayerNormalization() {
        String lowercasePan = "abcde1234f";

        ItrTaxpayerProfileDto profile = itrGovIntegrationService.lookupTaxpayer(itrConn.getId(), lowercasePan, null);

        assertThat(profile).isNotNull();
        assertThat(profile.isValid()).isTrue();
        assertThat(profile.getPan()).isEqualTo("ABCDE1234F");
    }

    @Test
    @DisplayName("lookupTaxpayer with invalid PAN format throws VALIDATION_FAILED exception")
    void testLookupTaxpayerInvalidPanFormat() {
        assertThatThrownBy(() -> itrGovIntegrationService.lookupTaxpayer(itrConn.getId(), "INVALID_PAN_123", null))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Invalid PAN format");

        assertThatThrownBy(() -> itrGovIntegrationService.lookupTaxpayer(itrConn.getId(), "", null))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Invalid PAN format");
    }

    @Test
    @DisplayName("lookupTaxpayer with NOT_FOUND simulation returns invalid profile with NOT_FOUND error")
    void testLookupTaxpayerNotFoundOutcome() {
        String testPan = "ABCDE1234F";

        ItrTaxpayerProfileDto profile = itrGovIntegrationService.lookupTaxpayer(
                itrConn.getId(),
                testPan,
                Map.of("mockOutcome", "NOT_FOUND")
        );

        assertThat(profile).isNotNull();
        assertThat(profile.isValid()).isFalse();
        assertThat(profile.isVerified()).isFalse();
        assertThat(profile.getPan()).isEqualTo(testPan);
        assertThat(profile.getErrorCode()).isEqualTo("NOT_FOUND");
        assertThat(profile.getErrorMessage()).containsIgnoringCase("not found");
    }

    @Test
    @DisplayName("lookupTaxpayer with INVALID_PAN simulation returns invalid profile with VALIDATION_FAILED error")
    void testLookupTaxpayerInvalidPanOutcome() {
        String testPan = "ABCDE1234F";

        ItrTaxpayerProfileDto profile = itrGovIntegrationService.lookupTaxpayer(
                itrConn.getId(),
                testPan,
                Map.of("mockOutcome", "INVALID_PAN")
        );

        assertThat(profile).isNotNull();
        assertThat(profile.isValid()).isFalse();
        assertThat(profile.isVerified()).isFalse();
        assertThat(profile.getErrorCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(profile.getErrorMessage()).containsIgnoringCase("Invalid PAN");
    }

    @Test
    @DisplayName("lookupTaxpayer with AUTH_REQUIRED simulation returns invalid profile with AUTH_REQUIRED error")
    void testLookupTaxpayerAuthRequiredOutcome() {
        String testPan = "ABCDE1234F";

        ItrTaxpayerProfileDto profile = itrGovIntegrationService.lookupTaxpayer(
                itrConn.getId(),
                testPan,
                Map.of("mockOutcome", "AUTH_REQUIRED")
        );

        assertThat(profile).isNotNull();
        assertThat(profile.isValid()).isFalse();
        assertThat(profile.isVerified()).isFalse();
        assertThat(profile.getErrorCode()).isEqualTo("AUTH_REQUIRED");
        assertThat(profile.getErrorMessage()).containsIgnoringCase("expired or invalid");
    }

    @Test
    @DisplayName("lookupTaxpayer with PROVIDER_UNAVAILABLE simulation returns invalid profile with PROVIDER_UNAVAILABLE error")
    void testLookupTaxpayerProviderUnavailableOutcome() {
        String testPan = "ABCDE1234F";

        ItrTaxpayerProfileDto profile = itrGovIntegrationService.lookupTaxpayer(
                itrConn.getId(),
                testPan,
                Map.of("mockOutcome", "PROVIDER_UNAVAILABLE")
        );

        assertThat(profile).isNotNull();
        assertThat(profile.isValid()).isFalse();
        assertThat(profile.isVerified()).isFalse();
        assertThat(profile.getErrorCode()).isEqualTo("PROVIDER_UNAVAILABLE");
        assertThat(profile.getErrorMessage()).containsIgnoringCase("unavailable");
    }

    @Test
    @DisplayName("lookupTaxpayer with TIMEOUT simulation returns invalid profile with TIMEOUT error")
    void testLookupTaxpayerTimeoutOutcome() {
        String testPan = "ABCDE1234F";

        ItrTaxpayerProfileDto profile = itrGovIntegrationService.lookupTaxpayer(
                itrConn.getId(),
                testPan,
                Map.of("mockOutcome", "TIMEOUT")
        );

        assertThat(profile).isNotNull();
        assertThat(profile.isValid()).isFalse();
        assertThat(profile.isVerified()).isFalse();
        assertThat(profile.getErrorCode()).isEqualTo("TIMEOUT");
        assertThat(profile.getErrorMessage()).containsIgnoringCase("timed out");
    }

    @Test
    @DisplayName("lookupTaxpayer with RATE_LIMITED simulation returns invalid profile with RATE_LIMITED error")
    void testLookupTaxpayerRateLimitedOutcome() {
        String testPan = "ABCDE1234F";

        ItrTaxpayerProfileDto profile = itrGovIntegrationService.lookupTaxpayer(
                itrConn.getId(),
                testPan,
                Map.of("mockOutcome", "RATE_LIMITED")
        );

        assertThat(profile).isNotNull();
        assertThat(profile.isValid()).isFalse();
        assertThat(profile.isVerified()).isFalse();
        assertThat(profile.getErrorCode()).isEqualTo("RATE_LIMITED");
        assertThat(profile.getErrorMessage()).containsIgnoringCase("Too many requests");
    }

    @Test
    @DisplayName("lookupTaxpayer with UNKNOWN / ERROR simulation returns invalid profile with UNKNOWN error")
    void testLookupTaxpayerUnknownErrorOutcome() {
        String testPan = "ABCDE1234F";

        ItrTaxpayerProfileDto profile = itrGovIntegrationService.lookupTaxpayer(
                itrConn.getId(),
                testPan,
                Map.of("mockOutcome", "UNKNOWN")
        );

        assertThat(profile).isNotNull();
        assertThat(profile.isValid()).isFalse();
        assertThat(profile.isVerified()).isFalse();
        assertThat(profile.getErrorCode()).isEqualTo("UNKNOWN");
        assertThat(profile.getErrorMessage()).containsIgnoringCase("gateway error");
    }

    @Test
    @DisplayName("lookupTaxpayer audit logging securely masks PAN in audit details and avoids secret leakage")
    @SuppressWarnings("unchecked")
    void testLookupTaxpayerAuditMasking() {
        String testPan = "ABCDE1234F";

        itrGovIntegrationService.lookupTaxpayer(itrConn.getId(), testPan, null);

        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService, atLeastOnce()).logEvent(
                eq(testOrg.getId()),
                any(),
                eq("ITR_TAXPAYER_LOOKUP_REQUESTED"),
                eq("ITR_CONNECTION"),
                eq(itrConn.getId().toString()),
                any(),
                detailsCaptor.capture()
        );

        Map<String, Object> details = detailsCaptor.getValue();
        assertThat(details).containsKey("pan");
        assertThat(details.get("pan")).isEqualTo("ABCDE****F");
        assertThat(details).doesNotContainKey("rawSecret");
        assertThat(details).doesNotContainKey("secret");
    }

    @Test
    @DisplayName("lookupTaxpayer when organization has no Income Tax connection throws VALIDATION_FAILED exception")
    void testLookupTaxpayerNoConnection() {
        OrganizationEntity emptyOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Empty ITR Org " + UUID.randomUUID())
                .email("empty.itr." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
        TenantContext.setTenantId(emptyOrg.getId());

        assertThatThrownBy(() -> itrGovIntegrationService.lookupTaxpayer("ABCDE1234F"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("No Income Tax government connection configured");
    }
}
