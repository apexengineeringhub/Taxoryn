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
import com.taxoryn.module.itr.integration.ItrGovernmentIntegrationService;
import com.taxoryn.module.itr.integration.dto.ItrIntegrationResultDto;
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
public class ItrGovernmentIntegrationServiceTest {

    @Autowired
    private ItrGovernmentIntegrationService itrIntegrationService;

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
                .name("ITR Service Test Practice " + UUID.randomUUID())
                .email("itr.svc." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
        TenantContext.setTenantId(testOrg.getId());

        itrConn = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.INCOME_TAX)
                .displayName("ITD Portal Main Gateway")
                .build());

        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(itrConn.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("itd_test_***")
                .rawSecret("ItdSecretKey123")
                .build());

        govConnectionService.activateConnection(itrConn.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("verifyPan must route through Government Framework and return normalized ItrIntegrationResultDto")
    void testVerifyPanSuccess() {
        String testPan = "ABCDE1234F";

        ItrIntegrationResultDto result = itrIntegrationService.verifyPan(itrConn.getId(), testPan);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPan()).isEqualTo(testPan);
        assertThat(result.getOperationType()).isEqualTo("VERIFY_PAN");
        assertThat(result.getOperationId()).isNotNull();
        assertThat(result.getCorrelationId()).isNotBlank();
        assertThat(result.getProviderReferenceId()).contains("ITD-PAN-ACK-");
        assertThat(result.getData()).containsKey("taxpayerName");
        assertThat(result.getData().get("status")).isEqualTo("ACTIVE");
        assertThat(result.getData().get("panStatus")).isEqualTo("OPERATIVE");
    }

    @Test
    @DisplayName("verifyPan with simulated RATE_LIMITED directive must return failure with RATE_LIMITED error code")
    void testVerifyPanRateLimited() {
        String testPan = "XYZPA9999Z";

        ItrIntegrationResultDto result = itrIntegrationService.verifyPan(
                itrConn.getId(), testPan, Map.of("mockOutcome", "RATE_LIMITED"));

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("RATE_LIMITED");
        assertThat(result.getErrorMessage()).contains("Too many requests");
    }

    @Test
    @DisplayName("executeItrOperation on non-INCOME_TAX connection must throw AppException")
    void testExecuteOnWrongProviderType() {
        GovConnectionDto gstConn = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("GST Gateway Misconfigured")
                .build());

        assertThatThrownBy(() -> itrIntegrationService.verifyPan(gstConn.getId(), "ABCDE1234F"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("not an Income Tax provider connection");
    }
}
