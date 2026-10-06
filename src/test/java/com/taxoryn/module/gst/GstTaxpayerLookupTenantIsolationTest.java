package com.taxoryn.module.gst;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@ActiveProfiles("test")
public class GstTaxpayerLookupTenantIsolationTest {

    @Autowired
    private GstGovernmentIntegrationService gstGovIntegrationService;

    @Autowired
    private GovernmentConnectionService govConnectionService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @MockBean
    private AuditService auditService;

    private OrganizationEntity tenantA;
    private OrganizationEntity tenantB;
    private GovConnectionDto connectionA;

    @BeforeEach
    void setUp() {
        tenantA = organizationRepository.save(OrganizationEntity.builder()
                .name("Tenant A Practice " + UUID.randomUUID())
                .email("tenantA." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        tenantB = organizationRepository.save(OrganizationEntity.builder()
                .name("Tenant B Practice " + UUID.randomUUID())
                .email("tenantB." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(tenantA.getId());
        connectionA = govConnectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("Tenant A GST Gateway")
                .build());
        govConnectionService.registerCredential(com.taxoryn.module.gov.dto.RegisterGovCredentialRequest.builder()
                .connectionId(connectionA.getId())
                .credentialType(com.taxoryn.module.gov.model.GovCredentialType.API_KEY)
                .maskedIdentifier("tenant_a_***")
                .rawSecret("SecretA123")
                .build());
        govConnectionService.activateConnection(connectionA.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Tenant B cannot perform GST taxpayer lookup using Tenant A's connection")
    void testCrossTenantLookupDenied() {
        TenantContext.setTenantId(tenantB.getId());

        assertThatThrownBy(() -> gstGovIntegrationService.lookupTaxpayer(connectionA.getId(), "27AAAPL1234C1ZV", null))
                .isInstanceOf(com.taxoryn.module.gov.exception.GovConnectionNotFoundException.class);
    }
}
