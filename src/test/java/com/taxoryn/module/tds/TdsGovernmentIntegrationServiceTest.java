package com.taxoryn.module.tds;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
import com.taxoryn.module.gov.model.GovCredentialType;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.tds.integration.TdsGovernmentIntegrationService;
import com.taxoryn.module.tds.integration.dto.TdsIntegrationResultDto;
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
public class TdsGovernmentIntegrationServiceTest {

    @Autowired
    private TdsGovernmentIntegrationService tdsIntegrationService;

    @Autowired
    private GovernmentConnectionService govConnectionService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @MockBean
    private AuditService auditService;

    private OrganizationEntity testOrg;
    private GovConnectionDto tdsConn;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("TDS Service Test Practice " + UUID.randomUUID())
                .email("tds.svc." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
        TenantContext.setTenantId(testOrg.getId());

        tdsConn = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.TDS)
                .displayName("TRACES Portal Main Gateway")
                .build());

        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(tdsConn.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("traces_test_***")
                .rawSecret("TracesSecretKey123")
                .build());

        govConnectionService.activateConnection(tdsConn.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("executeTdsOperation must route through Government Framework and return normalized TdsIntegrationResultDto")
    void testExecuteTdsOperationSuccess() {
        String testTan = "MUMB12345A";

        TdsIntegrationResultDto result = tdsIntegrationService.executeTdsOperation(
                tdsConn.getId(), "VERIFY_TAN", Map.of("tan", testTan));

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTan()).isEqualTo(testTan);
        assertThat(result.getOperationType()).isEqualTo("VERIFY_TAN");
        assertThat(result.getOperationId()).isNotNull();
        assertThat(result.getCorrelationId()).isNotBlank();
        assertThat(result.getProviderReferenceId()).contains("TRACES-VERIFY_TAN-ACK-");
        assertThat(result.getData()).containsKey("deductorName");
        assertThat(result.getData().get("status")).isEqualTo("ACTIVE");
        assertThat(result.getData().get("tanStatus")).isEqualTo("VALID");
    }

    @Test
    @DisplayName("executeTdsOperation with simulated RATE_LIMITED directive must return failure with RATE_LIMITED error code")
    void testExecuteTdsOperationRateLimited() {
        TdsIntegrationResultDto result = tdsIntegrationService.executeTdsOperation(
                tdsConn.getId(), "VERIFY_TAN", Map.of("tan", "MUMB12345A", "mockOutcome", "RATE_LIMITED"));

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("RATE_LIMITED");
        assertThat(result.getErrorMessage()).contains("rate limit exceeded");
    }

    @Test
    @DisplayName("executeTdsOperation with simulated AUTH_REQUIRED directive must return failure with AUTH_REQUIRED error code")
    void testExecuteTdsOperationAuthRequired() {
        TdsIntegrationResultDto result = tdsIntegrationService.executeTdsOperation(
                tdsConn.getId(), "VERIFY_TAN", Map.of("tan", "MUMB12345A", "mockOutcome", "AUTH_REQUIRED"));

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("AUTH_REQUIRED");
        assertThat(result.getErrorMessage()).contains("session token expired");
    }

    @Test
    @DisplayName("executeTdsOperation with simulated TIMEOUT directive must return failure with TIMEOUT error code")
    void testExecuteTdsOperationTimeout() {
        TdsIntegrationResultDto result = tdsIntegrationService.executeTdsOperation(
                tdsConn.getId(), "VERIFY_TAN", Map.of("tan", "MUMB12345A", "mockOutcome", "TIMEOUT"));

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("TIMEOUT");
        assertThat(result.getErrorMessage()).contains("timed out");
    }

    @Test
    @DisplayName("executeTdsOperation with non-TDS connection must throw VALIDATION_FAILED exception")
    void testExecuteWithNonTdsConnectionThrowsValidationException() {
        GovConnectionDto nonTdsConn = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("GST Portal Gateway")
                .build());

        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(nonTdsConn.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("gst_test_***")
                .rawSecret("GstSecretKey123")
                .build());

        govConnectionService.activateConnection(nonTdsConn.getId());

        assertThatThrownBy(() -> tdsIntegrationService.executeTdsOperation(
                nonTdsConn.getId(), "VERIFY_TAN", Map.of("tan", "MUMB12345A")))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("is not a TDS provider connection");
    }
}
