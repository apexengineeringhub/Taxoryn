package com.taxoryn.module.gst;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
import com.taxoryn.module.gov.model.GovCredentialType;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.gst.integration.GstGovernmentIntegrationService;
import com.taxoryn.module.gst.integration.dto.GstHandshakeResponseDto;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@ActiveProfiles("test")
public class GstMockHandshakeIntegrationTest {

    @Autowired
    private GstGovernmentIntegrationService gstIntegrationService;

    @Autowired
    private GovernmentConnectionService govConnectionService;

    @Autowired
    private OrganizationRepository organizationRepository;

    private OrganizationEntity testOrg;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("GST Mock Practice " + UUID.randomUUID())
                .email("gst.mock." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
        TenantContext.setTenantId(testOrg.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("GST mock handshake on active connection must succeed with HEALTHY status")
    void testSuccessfulGstMockHandshake() {
        GovConnectionDto conn = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("GST Practice Gateway")
                .build());

        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(conn.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("gstn_test_***")
                .rawSecret("MockSecretPassword123!")
                .build());

        govConnectionService.activateConnection(conn.getId());

        GstHandshakeResponseDto response = gstIntegrationService.checkGstConnectionHealth(conn.getId());

        assertThat(response).isNotNull();
        assertThat(response.getConnectionId()).isEqualTo(conn.getId());
        assertThat(response.getHealthStatus()).isEqualTo("HEALTHY");
        assertThat(response.getMessage()).containsIgnoringCase("successful");
    }

    @Test
    @DisplayName("GST mock handshake with AUTH_REQUIRED directive must return AUTH_REQUIRED")
    void testGstMockHandshakeAuthRequired() {
        GovConnectionDto conn = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("GST Auth Gateway")
                .build());

        GstHandshakeResponseDto response = gstIntegrationService.checkGstConnectionHealth(
                conn.getId(),
                Map.of("mockOutcome", "AUTH_REQUIRED")
        );

        assertThat(response).isNotNull();
        assertThat(response.getHealthStatus()).isEqualTo("AUTH_REQUIRED");
    }

    @Test
    @DisplayName("GST mock handshake with PROVIDER_UNAVAILABLE directive must return UNAVAILABLE")
    void testGstMockHandshakeUnavailable() {
        GovConnectionDto conn = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("GST Unavailable Gateway")
                .build());

        GstHandshakeResponseDto response = gstIntegrationService.checkGstConnectionHealth(
                conn.getId(),
                Map.of("mockOutcome", "PROVIDER_UNAVAILABLE")
        );

        assertThat(response).isNotNull();
        assertThat(response.getHealthStatus()).isEqualTo("UNAVAILABLE");
    }
}
