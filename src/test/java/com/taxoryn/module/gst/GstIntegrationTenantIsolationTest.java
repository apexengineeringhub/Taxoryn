package com.taxoryn.module.gst;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.exception.GovConnectionNotFoundException;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.gst.integration.GstGovernmentIntegrationService;
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
public class GstIntegrationTenantIsolationTest {

    @Autowired
    private GstGovernmentIntegrationService gstIntegrationService;

    @Autowired
    private GovernmentConnectionService govConnectionService;

    @Autowired
    private OrganizationRepository organizationRepository;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;

    @BeforeEach
    void setUp() {
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Tenant A Practice GST " + UUID.randomUUID())
                .email("tenantA.gst." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Tenant B Practice GST " + UUID.randomUUID())
                .email("tenantB.gst." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Tenant B cannot execute GST operations or handshakes on Tenant A's connection")
    void testTenantIsolationOnGstIntegration() {
        // Step 1: Create connection under Tenant A
        TenantContext.setTenantId(orgA.getId());
        GovConnectionDto orgAConn = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("Org A GST Connection")
                .build());

        // Step 2: Switch to Tenant B
        TenantContext.setTenantId(orgB.getId());

        // Attempt verifyGstin as Tenant B using Org A connection
        assertThatThrownBy(() -> gstIntegrationService.verifyGstin(orgAConn.getId(), "27AAAPL1234C1ZV"))
                .isInstanceOf(GovConnectionNotFoundException.class);

        // Attempt handshake as Tenant B using Org A connection
        assertThatThrownBy(() -> gstIntegrationService.checkGstConnectionHealth(orgAConn.getId()))
                .isInstanceOf(GovConnectionNotFoundException.class);
    }
}
