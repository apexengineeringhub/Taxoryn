package com.taxoryn.module.gov;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.GovConnectionHealthDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
import com.taxoryn.module.gov.model.GovConnectionHealthStatus;
import com.taxoryn.module.gov.model.GovConnectionStatus;
import com.taxoryn.module.gov.model.GovCredentialType;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.gov.service.GovernmentHealthService;
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

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@ActiveProfiles("test")
public class GovConnectionHealthAndHandshakeTest {

    @Autowired
    private GovernmentConnectionService connectionService;

    @Autowired
    private GovernmentHealthService healthService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @MockBean
    private AuditService auditService;

    private OrganizationEntity testOrg;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Gov Health Test Practice " + UUID.randomUUID())
                .email("gov.health." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
        TenantContext.setTenantId(testOrg.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Handshake against healthy connection must update status to HEALTHY and record latency")
    void testSuccessfulHandshake() {
        GovConnectionDto conn = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("GST Maharashtra Portal")
                .build());

        connectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(conn.getId())
                .credentialType(GovCredentialType.BASIC_AUTH)
                .maskedIdentifier("gst_user_***")
                .rawSecret("Password123!")
                .build());

        connectionService.activateConnection(conn.getId());

        GovConnectionHealthDto health = healthService.checkConnectionHealth(conn.getId());

        assertThat(health).isNotNull();
        assertThat(health.getHealthStatus()).isEqualTo(GovConnectionHealthStatus.HEALTHY);
        assertThat(health.getLatencyMs()).isGreaterThanOrEqualTo(0L);
        assertThat(health.getMessage()).containsIgnoringCase("successful");
        assertThat(health.getLastHealthCheckAt()).isNotNull();

        // Verify connection record also reflects new health state
        GovConnectionDto refreshed = connectionService.getConnection(conn.getId());
        assertThat(refreshed.getHealthStatus()).isEqualTo(GovConnectionHealthStatus.HEALTHY);
        assertThat(refreshed.getLastHealthCheckAt()).isNotNull();
    }

    @Test
    @DisplayName("Handshake with simulated AUTH_REQUIRED must update healthStatus to AUTH_REQUIRED")
    void testHandshakeAuthRequired() {
        GovConnectionDto conn = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.INCOME_TAX)
                .displayName("ITD Portal e-Filing")
                .build());

        GovConnectionHealthDto health = healthService.checkConnectionHealth(
                conn.getId(),
                Map.of("mockOutcome", "AUTH_REQUIRED")
        );

        assertThat(health.getHealthStatus()).isEqualTo(GovConnectionHealthStatus.AUTH_REQUIRED);
        assertThat(health.getMessage()).containsIgnoringCase("OTP re-authentication required");
    }

    @Test
    @DisplayName("Handshake with simulated PROVIDER_UNAVAILABLE must update healthStatus to UNAVAILABLE")
    void testHandshakeProviderUnavailable() {
        GovConnectionDto conn = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.TRACES)
                .displayName("TRACES Portal TDS")
                .build());

        GovConnectionHealthDto health = healthService.checkConnectionHealth(
                conn.getId(),
                Map.of("mockOutcome", "PROVIDER_UNAVAILABLE")
        );

        assertThat(health.getHealthStatus()).isEqualTo(GovConnectionHealthStatus.UNAVAILABLE);
        assertThat(health.getMessage()).containsIgnoringCase("unavailable");
    }

    @Test
    @DisplayName("Handshake with simulated TIMEOUT must update healthStatus to ERROR")
    void testHandshakeTimeoutError() {
        GovConnectionDto conn = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("GST Timeout Conn")
                .build());

        GovConnectionHealthDto health = healthService.checkConnectionHealth(
                conn.getId(),
                Map.of("mockOutcome", "TIMEOUT")
        );

        assertThat(health.getHealthStatus()).isEqualTo(GovConnectionHealthStatus.ERROR);
        assertThat(health.getMessage()).containsIgnoringCase("timed out");
    }

    @Test
    @DisplayName("Deactivated/INACTIVE connection must report UNKNOWN/deactivated health without invoking gateway")
    void testInactiveConnectionHealthCheck() {
        GovConnectionDto conn = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("Inactive GST Conn")
                .build());

        connectionService.deactivateConnection(conn.getId());

        GovConnectionHealthDto health = healthService.checkConnectionHealth(conn.getId());
        assertThat(health.getConnectionStatus()).isEqualTo(GovConnectionStatus.INACTIVE);
        assertThat(health.getHealthStatus()).isEqualTo(GovConnectionHealthStatus.UNKNOWN);
        assertThat(health.getMessage()).containsIgnoringCase("deactivated");
    }
}
