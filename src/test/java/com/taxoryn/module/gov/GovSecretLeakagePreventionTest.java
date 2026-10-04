package com.taxoryn.module.gov;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.GovCredentialReferenceDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@ActiveProfiles("test")
public class GovSecretLeakagePreventionTest {

    @Autowired
    private GovernmentConnectionService connectionService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @MockBean
    private AuditService auditService;

    private OrganizationEntity testOrg;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Gov Secret Leak Test " + UUID.randomUUID())
                .email("gov.leak." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
        TenantContext.setTenantId(testOrg.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Secrets must never leak into DTO responses, Audit payloads, or toString representations")
    void testSecretLeakagePreventionAcrossAllBoundaries() {
        String ultraSensitiveSecret = "TOP_SECRET_PASSWORD_DO_NOT_LEAK_998877!";

        // 1. Check RegisterGovCredentialRequest.toString() does not leak secret
        RegisterGovCredentialRequest regRequest = RegisterGovCredentialRequest.builder()
                .connectionId(UUID.randomUUID())
                .credentialType(GovCredentialType.BASIC_AUTH)
                .maskedIdentifier("user_masked_123")
                .rawSecret(ultraSensitiveSecret)
                .build();

        assertThat(regRequest.toString()).doesNotContain(ultraSensitiveSecret);
        assertThat(regRequest.toString()).doesNotContain("TOP_SECRET");

        // 2. Create connection
        GovConnectionDto conn = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("Gov Secret Leak Test Conn")
                .build());

        // 3. Register Credential
        regRequest.setConnectionId(conn.getId());
        GovCredentialReferenceDto credDto = connectionService.registerCredential(regRequest);

        // 4. Verify DTO responses do NOT contain the secret
        assertThat(credDto.toString()).doesNotContain(ultraSensitiveSecret);
        assertThat(conn.toString()).doesNotContain(ultraSensitiveSecret);

        // 5. Verify AuditService recorded events do NOT contain the raw secret
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(auditService, atLeastOnce()).logEvent(
                eq(testOrg.getId()),
                isNull(),
                eq("GOVERNMENT_CREDENTIAL_REGISTERED"),
                eq("GOV_CREDENTIAL"),
                eq(credDto.getId().toString()),
                isNull(),
                payloadCaptor.capture()
        );

        for (Object payload : payloadCaptor.getAllValues()) {
            String payloadStr = String.valueOf(payload);
            assertThat(payloadStr).doesNotContain(ultraSensitiveSecret);
            assertThat(payloadStr).doesNotContain("TOP_SECRET");
        }
    }
}
