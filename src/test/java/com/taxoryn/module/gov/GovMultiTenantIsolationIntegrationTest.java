package com.taxoryn.module.gov;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.model.GovProviderType;
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

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@ActiveProfiles("test")
public class GovMultiTenantIsolationIntegrationTest {

    @Autowired
    private GovernmentIntegrationService govIntegrationService;

    @Autowired
    private OrganizationRepository organizationRepository;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;

    @BeforeEach
    void setUp() {
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Practice Alpha " + UUID.randomUUID())
                .email("alpha." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Practice Beta " + UUID.randomUUID())
                .email("beta." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Organization B must not access, view, or replay operations created by Organization A")
    void testTenantDataIsolation() {
        // 1. Execute operation as Org A
        TenantContext.setTenantId(orgA.getId());
        GovIntegrationRequest requestA = GovIntegrationRequest.builder()
                .organizationId(orgA.getId())
                .providerType(GovProviderType.GST)
                .operationType("FETCH_GSTR2B")
                .correlationId("corr-org-a-01")
                .idempotencyKey("idemp-org-a-01")
                .requestData(Map.of("period", "092026"))
                .build();

        GovIntegrationResult resultA = govIntegrationService.executeOperation(requestA);
        assertThat(resultA.isSuccess()).isTrue();
        UUID opIdA = resultA.getOperationId();

        // 2. Switch context to Org B
        TenantContext.setTenantId(orgB.getId());

        // Org B cannot find Org A's operation by ID
        assertThatThrownBy(() -> govIntegrationService.getOperation(opIdA))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Government operation not found");

        // Org B cannot find Org A's operation by Correlation ID
        assertThatThrownBy(() -> govIntegrationService.getOperationByCorrelationId("corr-org-a-01"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Government operation not found");

        // Org B cannot find Org A's operation by Idempotency Key
        assertThatThrownBy(() -> govIntegrationService.getOperationByIdempotencyKey("idemp-org-a-01"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Government operation not found");

        // 3. Org B attempting to pass Org A's ID in request is rejected
        GovIntegrationRequest crossTenantRequest = GovIntegrationRequest.builder()
                .organizationId(orgA.getId()) // Intentionally spoofed
                .providerType(GovProviderType.GST)
                .operationType("FETCH_GSTR2B")
                .build();

        assertThatThrownBy(() -> govIntegrationService.executeOperation(crossTenantRequest))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException appEx = (AppException) e;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.TENANT_MISMATCH);
                });
    }
}
