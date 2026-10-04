package com.taxoryn.module.gov;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.infrastructure.provider.MockProviderAdapter;
import com.taxoryn.module.gov.model.GovOperationStatus;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.repository.GovIntegrationOperationRepository;
import com.taxoryn.module.gov.service.GovernmentIntegrationServiceImpl;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@ActiveProfiles("test")
public class GovAuditAndEventIntegrationTest {

    @Autowired
    private GovIntegrationOperationRepository operationRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @MockBean
    private AuditService auditService;

    private GovernmentIntegrationServiceImpl govIntegrationService;
    private OrganizationEntity testOrg;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Audit Test Practice " + UUID.randomUUID())
                .email("audit." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
        TenantContext.setTenantId(testOrg.getId());

        govIntegrationService = new GovernmentIntegrationServiceImpl(
                operationRepository,
                auditService,
                new ObjectMapper(),
                List.of(new MockProviderAdapter())
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Executing operation must generate audit events (CREATED, STARTED, SUCCEEDED) without sensitive data")
    void testAuditEventsGeneratedForSuccessfulOperation() {
        GovIntegrationRequest request = GovIntegrationRequest.builder()
                .organizationId(testOrg.getId())
                .providerType(GovProviderType.GST)
                .operationType("VERIFY_GSTIN")
                .correlationId("corr-audit-01")
                .idempotencyKey("idemp-audit-01")
                .requestData(Map.of("gstin", "27AAAAA0000A1Z5"))
                .build();

        GovIntegrationResult result = govIntegrationService.executeOperation(request);
        assertThat(result.isSuccess()).isTrue();

        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);

        verify(auditService, atLeastOnce()).logEvent(
                eq(testOrg.getId()),
                isNull(),
                actionCaptor.capture(),
                eq("GOV_OPERATION"),
                eq(result.getOperationId().toString()),
                isNull(),
                payloadCaptor.capture()
        );

        List<String> recordedActions = actionCaptor.getAllValues();
        assertThat(recordedActions).contains("GOV_OPERATION_CREATED", "GOV_OPERATION_STARTED", "GOV_OPERATION_SUCCEEDED");

        // Verify no credentials / sensitive tokens are passed in audit payload
        for (Object payload : payloadCaptor.getAllValues()) {
            String payloadStr = String.valueOf(payload);
            assertThat(payloadStr).doesNotContain("password");
            assertThat(payloadStr).doesNotContain("clientSecret");
            assertThat(payloadStr).doesNotContain("privateKey");
            assertThat(payloadStr).doesNotContain("otp");
        }
    }

    @Test
    @DisplayName("Failed operation must generate GOV_OPERATION_FAILED audit event")
    void testAuditEventsGeneratedForFailedOperation() {
        GovIntegrationRequest request = GovIntegrationRequest.builder()
                .organizationId(testOrg.getId())
                .providerType(GovProviderType.INCOME_TAX)
                .operationType("VALIDATE_PAN")
                .correlationId("corr-audit-fail-01")
                .idempotencyKey("idemp-audit-fail-01")
                .requestData(Map.of("mockOutcome", "FORBIDDEN"))
                .build();

        GovIntegrationResult result = govIntegrationService.executeOperation(request);
        assertThat(result.isSuccess()).isFalse();

        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);

        verify(auditService, atLeastOnce()).logEvent(
                eq(testOrg.getId()),
                isNull(),
                actionCaptor.capture(),
                eq("GOV_OPERATION"),
                eq(result.getOperationId().toString()),
                isNull(),
                any()
        );

        List<String> recordedActions = actionCaptor.getAllValues();
        assertThat(recordedActions).contains("GOV_OPERATION_FAILED");
    }
}
