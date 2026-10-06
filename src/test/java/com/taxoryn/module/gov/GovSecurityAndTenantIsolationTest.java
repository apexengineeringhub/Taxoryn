package com.taxoryn.module.gov;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.GovCredentialReferenceDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
import com.taxoryn.module.gov.exception.GovConnectionNotFoundException;
import com.taxoryn.module.gov.exception.GovCredentialNotFoundException;
import com.taxoryn.module.gov.model.GovCredentialType;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@ActiveProfiles("test")
public class GovSecurityAndTenantIsolationTest {

    @Autowired
    private GovernmentConnectionService connectionService;

    @Autowired
    private OrganizationRepository organizationRepository;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;

    @BeforeEach
    void setUp() {
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Tenant A Practice " + UUID.randomUUID())
                .email("tenantA." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Tenant B Practice " + UUID.randomUUID())
                .email("tenantB." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Tenant B cannot view, list, update, activate, or access secrets of Tenant A's connection")
    void testTenantIsolationOnGovernmentConnectionsAndCredentials() {
        // Step 1: Under Tenant A context, create connection and register credential
        TenantContext.setTenantId(orgA.getId());

        GovConnectionDto orgAConnection = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("Org A Primary GST Portal")
                .build());

        GovCredentialReferenceDto orgACredential = connectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(orgAConnection.getId())
                .credentialType(GovCredentialType.BASIC_AUTH)
                .maskedIdentifier("orgA_gst_***")
                .rawSecret("OrgASecretPassword123!")
                .build());

        assertThat(orgAConnection.getId()).isNotNull();
        assertThat(orgACredential.getId()).isNotNull();

        // Step 2: Switch to Tenant B context
        TenantContext.setTenantId(orgB.getId());

        // Attempt to view Org A connection as Tenant B
        assertThatThrownBy(() -> connectionService.getConnection(orgAConnection.getId()))
                .isInstanceOf(GovConnectionNotFoundException.class);

        // Attempt to view Org A credential metadata as Tenant B
        assertThatThrownBy(() -> connectionService.getCredentialMetadata(orgACredential.getId()))
                .isInstanceOf(GovCredentialNotFoundException.class);

        // Attempt to activate Org A connection as Tenant B
        assertThatThrownBy(() -> connectionService.activateConnection(orgAConnection.getId()))
                .isInstanceOf(GovConnectionNotFoundException.class);

        // Attempt to retrieve secret of Org A connection as Tenant B
        assertThatThrownBy(() -> connectionService.getDecryptedSecret(orgAConnection.getId()))
                .isInstanceOf(GovConnectionNotFoundException.class);

        // List connections under Tenant B: must NOT contain Tenant A's connection
        List<GovConnectionDto> tenantBConnections = connectionService.listConnections();
        assertThat(tenantBConnections).noneMatch(c -> c.getId().equals(orgAConnection.getId()));
    }

    @Test
    @DisplayName("Invoking connection management without active tenant context must be rejected with UNAUTHORIZED")
    void testUnauthorizedWithoutTenantContext() {
        TenantContext.clear();

        assertThatThrownBy(() -> connectionService.listConnections())
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Active tenant required");

        assertThatThrownBy(() -> connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("No Tenant Conn")
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Active tenant required");
    }
}
