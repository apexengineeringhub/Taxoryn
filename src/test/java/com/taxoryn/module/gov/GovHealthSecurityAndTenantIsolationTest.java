package com.taxoryn.module.gov;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.exception.GovConnectionNotFoundException;
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
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@ActiveProfiles("test")
public class GovHealthSecurityAndTenantIsolationTest {

    @Autowired
    private GovernmentConnectionService connectionService;

    @Autowired
    private GovernmentHealthService healthService;

    @Autowired
    private OrganizationRepository organizationRepository;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;

    @BeforeEach
    void setUp() {
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Tenant A Practice Health " + UUID.randomUUID())
                .email("tenantA.health." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Tenant B Practice Health " + UUID.randomUUID())
                .email("tenantB.health." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Tenant B cannot execute health check or observe health telemetry of Tenant A's connection")
    void testTenantIsolationOnHealthCheck() {
        // Step 1: Create connection under Tenant A
        TenantContext.setTenantId(orgA.getId());
        GovConnectionDto orgAConn = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("Org A GST Gateway")
                .build());

        // Step 2: Switch context to Tenant B
        TenantContext.setTenantId(orgB.getId());

        // Attempt health check on Org A connection as Tenant B
        assertThatThrownBy(() -> healthService.checkConnectionHealth(orgAConn.getId()))
                .isInstanceOf(GovConnectionNotFoundException.class);
    }

    @Test
    @DisplayName("Health check execution without active tenant context must be rejected with UNAUTHORIZED")
    void testUnauthorizedHealthCheck() {
        TenantContext.clear();

        assertThatThrownBy(() -> healthService.checkConnectionHealth(UUID.randomUUID()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Active tenant required");

        assertThatThrownBy(() -> healthService.checkAllActiveConnections())
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Active tenant required");
    }
}
