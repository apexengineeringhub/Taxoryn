package com.taxoryn.module.itr;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
import com.taxoryn.module.gov.model.GovCredentialType;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.itr.integration.ItrGovernmentIntegrationService;
import com.taxoryn.module.itr.integration.dto.ItrHandshakeResponseDto;
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
public class ItrMockHandshakeIntegrationTest {

    @Autowired
    private ItrGovernmentIntegrationService itrIntegrationService;

    @Autowired
    private GovernmentConnectionService govConnectionService;

    @Autowired
    private OrganizationRepository organizationRepository;

    private OrganizationEntity testOrg;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("ITR Mock Practice " + UUID.randomUUID())
                .email("itr.mock." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
        TenantContext.setTenantId(testOrg.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("ITR mock handshake on active connection must succeed with HEALTHY status")
    void testSuccessfulItrMockHandshake() {
        GovConnectionDto connection = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.INCOME_TAX)
                .displayName("Income Tax Department Gateway")
                .build());

        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(connection.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("itd_live_***")
                .rawSecret("SecretItdKey98765")
                .build());

        govConnectionService.activateConnection(connection.getId());

        ItrHandshakeResponseDto response = itrIntegrationService.checkItrConnectionHealth(connection.getId());

        assertThat(response).isNotNull();
        assertThat(response.getConnectionId()).isEqualTo(connection.getId());
        assertThat(response.getHealthStatus()).isEqualTo("HEALTHY");
        assertThat(response.getMessage()).contains("handshake successful");
        assertThat(response.getLatencyMs()).isGreaterThanOrEqualTo(0);
        assertThat(response.getLastHealthCheckAt()).isNotNull();
    }

    @Test
    @DisplayName("ITR mock handshake with simulated AUTH_REQUIRED outcome")
    void testAuthRequiredItrMockHandshake() {
        GovConnectionDto connection = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.INCOME_TAX)
                .displayName("ITD Auth Required Gateway")
                .build());

        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(connection.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("itd_expired_***")
                .rawSecret("SecretExpiredKey123")
                .build());

        govConnectionService.activateConnection(connection.getId());

        ItrHandshakeResponseDto response = itrIntegrationService.checkItrConnectionHealth(
                connection.getId(), Map.of("mockOutcome", "AUTH_REQUIRED"));

        assertThat(response).isNotNull();
        assertThat(response.getHealthStatus()).isEqualTo("AUTH_REQUIRED");
        assertThat(response.getMessage()).contains("Auth session expired");
    }

    @Test
    @DisplayName("ITR mock handshake with simulated UNAVAILABLE outcome")
    void testUnavailableItrMockHandshake() {
        GovConnectionDto connection = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.INCOME_TAX)
                .displayName("ITD Maintenance Gateway")
                .build());

        ItrHandshakeResponseDto response = itrIntegrationService.checkItrConnectionHealth(
                connection.getId(), Map.of("mockOutcome", "PROVIDER_UNAVAILABLE"));

        assertThat(response).isNotNull();
        assertThat(response.getHealthStatus()).isEqualTo("UNAVAILABLE");
        assertThat(response.getMessage()).contains("scheduled maintenance");
    }
}
