package com.taxoryn.module.gov;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.dto.GovOperationDto;
import com.taxoryn.module.gov.entity.GovIntegrationOperationEntity;
import com.taxoryn.module.gov.exception.GovStateTransitionException;
import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.model.GovOperationStatus;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.repository.GovIntegrationOperationRepository;
import com.taxoryn.module.gov.service.GovernmentIntegrationService;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@ActiveProfiles("test")
public class GovOperationLifecycleIntegrationTest {

    @Autowired
    private GovernmentIntegrationService govIntegrationService;

    @Autowired
    private GovIntegrationOperationRepository operationRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private OrganizationEntity testOrg;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Lifecycle Test Practice " + UUID.randomUUID())
                .email("lifecycle." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
        TenantContext.setTenantId(testOrg.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should successfully execute operation from CREATED to IN_PROGRESS to SUCCEEDED")
    void testSuccessfulOperationLifecycle() {
        UUID entityId = UUID.randomUUID();
        GovIntegrationRequest request = GovIntegrationRequest.builder()
                .organizationId(testOrg.getId())
                .providerType(GovProviderType.GST)
                .operationType("SUBMIT_GSTR3B")
                .businessEntityType("GST_RETURN")
                .businessEntityId(entityId)
                .correlationId("corr-gst-3b-01")
                .requestData(Map.of("returnPeriod", "092026", "taxableAmount", 150000.00))
                .build();

        GovIntegrationResult result = govIntegrationService.executeOperation(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getStatus()).isEqualTo(GovOperationStatus.SUCCEEDED);
        assertThat(result.getOperationId()).isNotNull();
        assertThat(result.getProviderReferenceId()).startsWith("ARN-MOCK-");

        // Inspect persisted operation
        GovOperationDto persisted = govIntegrationService.getOperation(result.getOperationId());
        assertThat(persisted).isNotNull();
        assertThat(persisted.getOrganizationId()).isEqualTo(testOrg.getId());
        assertThat(persisted.getStatus()).isEqualTo(GovOperationStatus.SUCCEEDED);
        assertThat(persisted.getAttemptCount()).isEqualTo(1);
        assertThat(persisted.getProviderReferenceId()).isEqualTo(result.getProviderReferenceId());
        assertThat(persisted.getCompletedAt()).isNotNull();
        assertThat(persisted.getErrorCode()).isNull();

        // Query by correlation ID
        GovOperationDto byCorr = govIntegrationService.getOperationByCorrelationId("corr-gst-3b-01");
        assertThat(byCorr.getId()).isEqualTo(result.getOperationId());

        // Query by business entity
        List<GovOperationDto> byEntity = govIntegrationService.getOperationsByBusinessEntity("GST_RETURN", entityId);
        assertThat(byEntity).hasSize(1);
        assertThat(byEntity.get(0).getId()).isEqualTo(result.getOperationId());
    }

    @Test
    @DisplayName("Should transition operation to FAILED when permanent validation error occurs")
    void testFailedOperationLifecycle() {
        GovIntegrationRequest request = GovIntegrationRequest.builder()
                .organizationId(testOrg.getId())
                .providerType(GovProviderType.INCOME_TAX)
                .operationType("E_FILE_ITR1")
                .correlationId("corr-itr-fail-01")
                .requestData(Map.of("mockOutcome", "VALIDATION_FAILED"))
                .build();

        GovIntegrationResult result = govIntegrationService.executeOperation(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getStatus()).isEqualTo(GovOperationStatus.FAILED);
        assertThat(result.getErrorCode()).isEqualTo(GovErrorCode.VALIDATION_FAILED);

        GovOperationDto persisted = govIntegrationService.getOperation(result.getOperationId());
        assertThat(persisted.getStatus()).isEqualTo(GovOperationStatus.FAILED);
        assertThat(persisted.getErrorCode()).isEqualTo(GovErrorCode.VALIDATION_FAILED);
        assertThat(persisted.getErrorMessage()).contains("Validation Failed");
        assertThat(persisted.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should transition operation to RETRYING when transient timeout occurs and attempts remain")
    void testRetryingOperationLifecycle() {
        GovIntegrationRequest request = GovIntegrationRequest.builder()
                .organizationId(testOrg.getId())
                .providerType(GovProviderType.TRACES)
                .operationType("DOWNLOAD_26AS")
                .correlationId("corr-traces-retry-01")
                .requestData(Map.of("mockOutcome", "TIMEOUT"))
                .build();

        GovIntegrationResult result = govIntegrationService.executeOperation(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getStatus()).isEqualTo(GovOperationStatus.RETRYING);
        assertThat(result.isTransientError()).isTrue();
        assertThat(result.getErrorCode()).isEqualTo(GovErrorCode.TIMEOUT);

        GovOperationDto persisted = govIntegrationService.getOperation(result.getOperationId());
        assertThat(persisted.getStatus()).isEqualTo(GovOperationStatus.RETRYING);
        assertThat(persisted.getAttemptCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Entity should enforce valid state transitions and reject illegal transitions")
    void testEntityStateTransitionRules() {
        GovIntegrationOperationEntity entity = GovIntegrationOperationEntity.builder()
                .providerType(GovProviderType.GST)
                .operationType("VERIFY_GSTIN")
                .correlationId("corr-state-01")
                .idempotencyKey("idemp-state-01")
                .status(GovOperationStatus.CREATED)
                .build();

        // Valid: CREATED -> IN_PROGRESS
        entity.transitionTo(GovOperationStatus.IN_PROGRESS);
        assertThat(entity.getStatus()).isEqualTo(GovOperationStatus.IN_PROGRESS);

        // Valid: IN_PROGRESS -> SUCCEEDED
        entity.transitionTo(GovOperationStatus.SUCCEEDED);
        assertThat(entity.getStatus()).isEqualTo(GovOperationStatus.SUCCEEDED);
        assertThat(entity.getCompletedAt()).isNotNull();

        // Invalid: SUCCEEDED -> IN_PROGRESS (Terminal state mutation)
        assertThatThrownBy(() -> entity.transitionTo(GovOperationStatus.IN_PROGRESS))
                .isInstanceOf(GovStateTransitionException.class);
    }
}
