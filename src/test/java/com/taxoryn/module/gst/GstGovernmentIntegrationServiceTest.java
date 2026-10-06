package com.taxoryn.module.gst;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
import com.taxoryn.module.gov.model.GovCredentialType;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.gst.integration.GstGovernmentIntegrationService;
import com.taxoryn.module.gst.integration.dto.GstIntegrationResultDto;
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
public class GstGovernmentIntegrationServiceTest {

    @Autowired
    private GstGovernmentIntegrationService gstIntegrationService;

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
                .name("GST Service Test Practice " + UUID.randomUUID())
                .email("gst.svc." + UUID.randomUUID() + "@taxoryn.com")
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
                .maskedIdentifier("gst_test_***")
                .rawSecret("GstApiKeySecret123")
                .build());

        govConnectionService.activateConnection(gstConn.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("verifyGstin must route through Government Framework and return normalized GstIntegrationResultDto")
    void testVerifyGstinSuccess() {
        String testGstin = "27AAAPL1234C1ZV";

        GstIntegrationResultDto result = gstIntegrationService.verifyGstin(gstConn.getId(), testGstin);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getGstin()).isEqualTo(testGstin);
        assertThat(result.getOperationType()).isEqualTo("VERIFY_GSTIN");
        assertThat(result.getOperationId()).isNotNull();
        assertThat(result.getCorrelationId()).isNotBlank();
        assertThat(result.getProviderReferenceId()).contains("ARN-");
        assertThat(result.getData()).containsKey("legalName");
        assertThat(result.getData().get("status")).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("verifyGstin with simulated RATE_LIMITED directive must return failure with RATE_LIMITED error code")
    void testVerifyGstinRateLimited() {
        String testGstin = "27AAAPL1234C1ZV";

        GstIntegrationResultDto result = gstIntegrationService.verifyGstin(
                gstConn.getId(),
                testGstin,
                Map.of("mockOutcome", "RATE_LIMITED")
        );

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("RATE_LIMITED");
        assertThat(result.getErrorMessage()).containsIgnoringCase("rate limit");
    }

    @Test
    @DisplayName("Invoking GST operation against non-GST connection must fail validation")
    void testRejectNonGstConnection() {
        GovConnectionDto itdConn = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.INCOME_TAX)
                .displayName("ITD Portal")
                .build());

        assertThatThrownBy(() -> gstIntegrationService.verifyGstin(itdConn.getId(), "27AAAPL1234C1ZV"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("not a GST provider connection");
    }
}
