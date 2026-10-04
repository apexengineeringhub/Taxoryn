package com.taxoryn.module.tds;

import com.taxoryn.core.security.TenantContext;
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
import com.taxoryn.module.tds.integration.dto.TdsHandshakeResponseDto;
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
public class TdsMockHandshakeIntegrationTest {

    @Autowired
    private TdsGovernmentIntegrationService tdsIntegrationService;

    @Autowired
    private GovernmentConnectionService govConnectionService;

    @Autowired
    private OrganizationRepository organizationRepository;

    private OrganizationEntity testOrg;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("TDS Mock Practice " + UUID.randomUUID())
                .email("tds.mock." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
        TenantContext.setTenantId(testOrg.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("TDS mock handshake on active connection must succeed with HEALTHY status")
    void testSuccessfulTdsMockHandshake() {
        GovConnectionDto connection = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.TDS)
                .displayName("TRACES Portal TDS Gateway")
                .build());

        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(connection.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("traces_live_***")
                .rawSecret("SecretTracesKey98765")
                .build());

        govConnectionService.activateConnection(connection.getId());

        TdsHandshakeResponseDto response = tdsIntegrationService.checkTdsConnectionHealth(connection.getId());

        assertThat(response).isNotNull();
        assertThat(response.getConnectionId()).isEqualTo(connection.getId());
        assertThat(response.getHealthStatus()).isEqualTo("HEALTHY");
        assertThat(response.getMessage()).contains("handshake successful");
        assertThat(response.getLatencyMs()).isGreaterThanOrEqualTo(0);
        assertThat(response.getLastHealthCheckAt()).isNotNull();
    }

    @Test
    @DisplayName("TDS mock handshake with simulated AUTH_REQUIRED outcome")
    void testAuthRequiredTdsMockHandshake() {
        GovConnectionDto connection = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.TDS)
                .displayName("TRACES Auth Required Gateway")
                .build());

        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(connection.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("traces_expired_***")
                .rawSecret("SecretExpiredKey123")
                .build());

        govConnectionService.activateConnection(connection.getId());

        TdsHandshakeResponseDto response = tdsIntegrationService.checkTdsConnectionHealth(
                connection.getId(), Map.of("mockOutcome", "AUTH_REQUIRED"));

        assertThat(response).isNotNull();
        assertThat(response.getHealthStatus()).isEqualTo("AUTH_REQUIRED");
        assertThat(response.getMessage()).contains("Auth session expired");
    }

    @Test
    @DisplayName("TDS mock handshake with simulated UNAVAILABLE outcome")
    void testUnavailableTdsMockHandshake() {
        GovConnectionDto connection = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.TDS)
                .displayName("TRACES Maintenance Gateway")
                .build());

        TdsHandshakeResponseDto response = tdsIntegrationService.checkTdsConnectionHealth(
                connection.getId(), Map.of("mockOutcome", "PROVIDER_UNAVAILABLE"));

        assertThat(response).isNotNull();
        assertThat(response.getHealthStatus()).isEqualTo("UNAVAILABLE");
        assertThat(response.getMessage()).contains("scheduled maintenance");
    }

    @Test
    @DisplayName("TDS mock handshake with simulated TIMEOUT outcome")
    void testTimeoutTdsMockHandshake() {
        GovConnectionDto connection = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.TDS)
                .displayName("TRACES Timeout Gateway")
                .build());

        TdsHandshakeResponseDto response = tdsIntegrationService.checkTdsConnectionHealth(
                connection.getId(), Map.of("mockOutcome", "TIMEOUT"));

        assertThat(response).isNotNull();
        assertThat(response.getHealthStatus()).isEqualTo("ERROR");
        assertThat(response.getMessage()).contains("timed out");
    }

    @Test
    @DisplayName("TDS mock handshake with simulated RATE_LIMITED outcome")
    void testRateLimitedTdsMockHandshake() {
        GovConnectionDto connection = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.TDS)
                .displayName("TRACES Rate Limited Gateway")
                .build());

        TdsHandshakeResponseDto response = tdsIntegrationService.checkTdsConnectionHealth(
                connection.getId(), Map.of("mockOutcome", "RATE_LIMITED"));

        assertThat(response).isNotNull();
        assertThat(response.getHealthStatus()).isEqualTo("ERROR");
        assertThat(response.getMessage()).contains("rate limit exceeded");
    }
}
