package com.taxoryn.module.tds;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
import com.taxoryn.module.gov.exception.GovConnectionNotFoundException;
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

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@ActiveProfiles("test")
public class TdsIntegrationTenantIsolationTest {

    @Autowired
    private TdsGovernmentIntegrationService tdsIntegrationService;

    @Autowired
    private GovernmentConnectionService govConnectionService;

    @Autowired
    private OrganizationRepository organizationRepository;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private GovConnectionDto orgAConnection;

    @BeforeEach
    void setUp() {
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Tenant Org A " + UUID.randomUUID())
                .email("orgA." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Tenant Org B " + UUID.randomUUID())
                .email("orgB." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgA.getId());
        orgAConnection = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.TDS)
                .displayName("Org A TRACES Gateway")
                .build());

        govConnectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(orgAConnection.getId())
                .credentialType(com.taxoryn.module.gov.model.GovCredentialType.API_KEY)
                .maskedIdentifier("traces_orgA_***")
                .rawSecret("TracesSecretKeyOrgA123")
                .build());

        govConnectionService.activateConnection(orgAConnection.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Tenant A can perform handshake on its own connection")
    void testTenantCanAccessOwnConnection() {
        TenantContext.setTenantId(orgA.getId());
        TdsHandshakeResponseDto response = tdsIntegrationService.checkTdsConnectionHealth(orgAConnection.getId());
        assertThat(response).isNotNull();
        assertThat(response.getHealthStatus()).isEqualTo("HEALTHY");
    }

    @Test
    @DisplayName("Tenant B cannot perform handshake on Tenant A's connection")
    void testCrossTenantHandshakeIsBlocked() {
        TenantContext.setTenantId(orgB.getId());

        assertThatThrownBy(() -> tdsIntegrationService.checkTdsConnectionHealth(orgAConnection.getId()))
                .isInstanceOf(GovConnectionNotFoundException.class);
    }

    @Test
    @DisplayName("Tenant B cannot execute operations on Tenant A's connection")
    void testCrossTenantOperationExecutionIsBlocked() {
        TenantContext.setTenantId(orgB.getId());

        assertThatThrownBy(() -> tdsIntegrationService.executeTdsOperation(
                orgAConnection.getId(), "VERIFY_TAN", Map.of("tan", "MUMB12345A")))
                .isInstanceOf(GovConnectionNotFoundException.class);
    }

    @Test
    @DisplayName("Operation without active TenantContext must fail with UNAUTHORIZED")
    void testMissingTenantContextThrowsUnauthorized() {
        TenantContext.clear();

        assertThatThrownBy(() -> tdsIntegrationService.checkTdsConnectionHealth(orgAConnection.getId()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Active tenant context is required");
    }
}
