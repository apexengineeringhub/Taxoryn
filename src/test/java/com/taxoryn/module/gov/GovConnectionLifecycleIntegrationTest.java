package com.taxoryn.module.gov;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
import com.taxoryn.module.gov.dto.UpdateGovConnectionRequest;
import com.taxoryn.module.gov.exception.GovConnectionStateTransitionException;
import com.taxoryn.module.gov.model.GovConnectionStatus;
import com.taxoryn.module.gov.model.GovCredentialType;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.repository.GovConnectionRepository;
import com.taxoryn.module.gov.repository.GovCredentialReferenceRepository;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@ActiveProfiles("test")
public class GovConnectionLifecycleIntegrationTest {

    @Autowired
    private GovernmentConnectionService connectionService;

    @Autowired
    private GovConnectionRepository connectionRepository;

    @Autowired
    private GovCredentialReferenceRepository credentialRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @MockBean
    private AuditService auditService;

    private OrganizationEntity testOrg;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Gov Lifecycle Practice " + UUID.randomUUID())
                .email("gov.lifecycle." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
        TenantContext.setTenantId(testOrg.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should successfully create connection with CREATED state and update metadata")
    void testCreateAndUpdateConnection() {
        CreateGovConnectionRequest createReq = CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("Maharashtra GST Portal")
                .description("Primary GST connection for MH state filing")
                .environment("PRODUCTION")
                .metadata("{\"stateCode\": \"27\"}")
                .build();

        GovConnectionDto created = connectionService.createConnection(createReq);
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(GovConnectionStatus.CREATED);
        assertThat(created.getProviderType()).isEqualTo(GovProviderType.GST);
        assertThat(created.getDisplayName()).isEqualTo("Maharashtra GST Portal");

        // Update metadata
        UpdateGovConnectionRequest updateReq = UpdateGovConnectionRequest.builder()
                .displayName("Maharashtra Primary GST Portal")
                .description("Updated description")
                .environment("PRODUCTION")
                .metadata("{\"stateCode\": \"27\", \"version\": \"2.0\"}")
                .build();

        GovConnectionDto updated = connectionService.updateConnection(created.getId(), updateReq);
        assertThat(updated.getDisplayName()).isEqualTo("Maharashtra Primary GST Portal");
        assertThat(updated.getDescription()).isEqualTo("Updated description");
    }

    @Test
    @DisplayName("Activating connection without valid credential reference must fail with state transition exception")
    void testActivateWithoutCredentialFails() {
        CreateGovConnectionRequest createReq = CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.INCOME_TAX)
                .displayName("ITD Portal e-Filing")
                .build();

        GovConnectionDto created = connectionService.createConnection(createReq);

        assertThatThrownBy(() -> connectionService.activateConnection(created.getId()))
                .isInstanceOf(GovConnectionStateTransitionException.class)
                .hasMessageContaining("Cannot activate connection without a registered credential reference");
    }

    @Test
    @DisplayName("Complete lifecycle: CREATED -> Register Credential -> ACTIVE -> INACTIVE -> Re-activate -> AUTH_REQUIRED -> FAILED")
    void testCompleteConnectionLifecycle() {
        // 1. Create
        GovConnectionDto conn = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.TRACES)
                .displayName("TRACES Portal TDS")
                .build());
        assertThat(conn.getStatus()).isEqualTo(GovConnectionStatus.CREATED);

        // 2. Register credential
        connectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(conn.getId())
                .credentialType(GovCredentialType.BASIC_AUTH)
                .maskedIdentifier("traces_tan_***")
                .rawSecret("SuperSecretTracesPassword123!")
                .build());

        // 3. Activate
        GovConnectionDto activated = connectionService.activateConnection(conn.getId());
        assertThat(activated.getStatus()).isEqualTo(GovConnectionStatus.ACTIVE);

        // 4. Deactivate
        GovConnectionDto deactivated = connectionService.deactivateConnection(conn.getId());
        assertThat(deactivated.getStatus()).isEqualTo(GovConnectionStatus.INACTIVE);

        // 5. Re-activate
        GovConnectionDto reactivated = connectionService.activateConnection(conn.getId());
        assertThat(reactivated.getStatus()).isEqualTo(GovConnectionStatus.ACTIVE);

        // 6. Mark Auth Required
        GovConnectionDto authReq = connectionService.markAuthRequired(conn.getId());
        assertThat(authReq.getStatus()).isEqualTo(GovConnectionStatus.AUTH_REQUIRED);

        // 7. Mark Failed
        GovConnectionDto failed = connectionService.markFailed(conn.getId(), "Provider handshake failed");
        assertThat(failed.getStatus()).isEqualTo(GovConnectionStatus.FAILED);
    }

    @Test
    @DisplayName("Should filter connections by provider type correctly")
    void testFilterConnectionsByProvider() {
        connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("GST Conn 1")
                .build());

        connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("GST Conn 2")
                .build());

        connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.INCOME_TAX)
                .displayName("ITD Conn 1")
                .build());

        List<GovConnectionDto> gstConns = connectionService.listConnectionsByProvider(GovProviderType.GST);
        assertThat(gstConns).hasSize(2);

        List<GovConnectionDto> itdConns = connectionService.listConnectionsByProvider(GovProviderType.INCOME_TAX);
        assertThat(itdConns).hasSize(1);

        List<GovConnectionDto> tracesConns = connectionService.listConnectionsByProvider(GovProviderType.TRACES);
        assertThat(tracesConns).isEmpty();
    }
}
