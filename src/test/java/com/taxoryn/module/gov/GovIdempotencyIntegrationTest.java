package com.taxoryn.module.gov;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.model.GovOperationStatus;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentIntegrationService;
import com.taxoryn.module.gov.service.GovIdempotencyKeyGenerator;
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

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@ActiveProfiles("test")
public class GovIdempotencyIntegrationTest {

    @Autowired
    private GovernmentIntegrationService govIntegrationService;

    @Autowired
    private OrganizationRepository organizationRepository;

    private OrganizationEntity testOrg;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Idempotency Test Practice " + UUID.randomUUID())
                .email("idempotency." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
        TenantContext.setTenantId(testOrg.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Duplicate request with identical idempotency key should safely replay cached SUCCEEDED operation")
    void testIdempotentExecutionReplay() {
        UUID entityId = UUID.randomUUID();
        String idempotencyKey = GovIdempotencyKeyGenerator.generateKey(
                testOrg.getId(),
                GovProviderType.GST,
                "GST_RETURN",
                entityId,
                "FILE_GSTR1",
                "sha256:abcd1234ef5678"
        );

        GovIntegrationRequest request1 = GovIntegrationRequest.builder()
                .organizationId(testOrg.getId())
                .providerType(GovProviderType.GST)
                .operationType("FILE_GSTR1")
                .businessEntityType("GST_RETURN")
                .businessEntityId(entityId)
                .correlationId("corr-req-1")
                .idempotencyKey(idempotencyKey)
                .requestData(Map.of("amount", 50000))
                .build();

        // First Execution
        GovIntegrationResult result1 = govIntegrationService.executeOperation(request1);
        assertThat(result1.isSuccess()).isTrue();
        assertThat(result1.getStatus()).isEqualTo(GovOperationStatus.SUCCEEDED);
        assertThat(result1.getOperationId()).isNotNull();
        String originalArn = result1.getProviderReferenceId();
        assertThat(originalArn).isNotNull();

        // Second duplicate Execution with different correlationId but identical idempotencyKey
        GovIntegrationRequest request2 = GovIntegrationRequest.builder()
                .organizationId(testOrg.getId())
                .providerType(GovProviderType.GST)
                .operationType("FILE_GSTR1")
                .businessEntityType("GST_RETURN")
                .businessEntityId(entityId)
                .correlationId("corr-req-2-duplicate")
                .idempotencyKey(idempotencyKey)
                .requestData(Map.of("amount", 50000))
                .build();

        GovIntegrationResult result2 = govIntegrationService.executeOperation(request2);

        // Assert that result2 matches cached operation
        assertThat(result2.isSuccess()).isTrue();
        assertThat(result2.getStatus()).isEqualTo(GovOperationStatus.SUCCEEDED);
        assertThat(result2.getOperationId()).isEqualTo(result1.getOperationId());
        assertThat(result2.getProviderReferenceId()).isEqualTo(originalArn);
    }

    @Test
    @DisplayName("Idempotency key generator should be deterministic and distinct across parameter changes")
    void testIdempotencyKeyGeneratorDeterminism() {
        UUID orgId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();

        String key1 = GovIdempotencyKeyGenerator.generateKey(orgId, GovProviderType.INCOME_TAX, "ITR", entityId, "SUBMIT", "hash1");
        String key2 = GovIdempotencyKeyGenerator.generateKey(orgId, GovProviderType.INCOME_TAX, "ITR", entityId, "SUBMIT", "hash1");
        String keyDifferentHash = GovIdempotencyKeyGenerator.generateKey(orgId, GovProviderType.INCOME_TAX, "ITR", entityId, "SUBMIT", "hash2");
        String keyDifferentOrg = GovIdempotencyKeyGenerator.generateKey(UUID.randomUUID(), GovProviderType.INCOME_TAX, "ITR", entityId, "SUBMIT", "hash1");

        assertThat(key1).isEqualTo(key2);
        assertThat(key1).isNotEqualTo(keyDifferentHash);
        assertThat(key1).isNotEqualTo(keyDifferentOrg);
        assertThat(key1).hasSize(64); // Valid SHA-256 hex string
    }
}
