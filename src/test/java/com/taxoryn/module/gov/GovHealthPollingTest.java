package com.taxoryn.module.gov;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.GovConnectionHealthDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
import com.taxoryn.module.gov.model.GovConnectionHealthStatus;
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
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@ActiveProfiles("test")
public class GovHealthPollingTest {

    @Autowired
    private GovernmentConnectionService connectionService;

    @Autowired
    private GovernmentHealthService healthService;

    @Autowired
    private OrganizationRepository organizationRepository;

    private OrganizationEntity testOrg;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Gov Polling Practice " + UUID.randomUUID())
                .email("gov.polling." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
        TenantContext.setTenantId(testOrg.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Polling active connections must probe all ACTIVE connections and update their health status")
    void testCheckAllActiveConnections() {
        // 1. Create and activate GST connection
        GovConnectionDto gstConn = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("Active GST Connection")
                .build());

        connectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(gstConn.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("gst_key_***")
                .rawSecret("GstApiKeySecret123")
                .build());

        connectionService.activateConnection(gstConn.getId());

        // 2. Create un-activated (CREATED) ITD connection
        GovConnectionDto itdConn = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.INCOME_TAX)
                .displayName("Inactive ITD Connection")
                .build());

        // 3. Execute batch health poll
        List<GovConnectionHealthDto> polledResults = healthService.checkAllActiveConnections();

        // Must include the active GST connection and exclude the inactive ITD connection
        assertThat(polledResults).hasSize(1);
        assertThat(polledResults.get(0).getConnectionId()).isEqualTo(gstConn.getId());
        assertThat(polledResults.get(0).getHealthStatus()).isEqualTo(GovConnectionHealthStatus.HEALTHY);
    }
}
