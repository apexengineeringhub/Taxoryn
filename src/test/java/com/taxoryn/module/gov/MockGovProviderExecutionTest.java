package com.taxoryn.module.gov;

import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.infrastructure.provider.MockProviderAdapter;
import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.model.GovOperationStatus;
import com.taxoryn.module.gov.model.GovProviderHealth;
import com.taxoryn.module.gov.model.GovProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MockGovProviderExecutionTest {

    private MockProviderAdapter mockAdapter;
    private UUID testOrgId;

    @BeforeEach
    void setUp() {
        mockAdapter = new MockProviderAdapter();
        testOrgId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Mock Provider should return successful result with generated reference ID by default")
    void testMockSuccessOutcome() {
        GovIntegrationRequest request = GovIntegrationRequest.builder()
                .organizationId(testOrgId)
                .providerType(GovProviderType.GST)
                .operationType("FILE_GSTR1")
                .correlationId("corr-123")
                .idempotencyKey("idemp-123")
                .build();

        GovIntegrationResult result = mockAdapter.execute(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getStatus()).isEqualTo(GovOperationStatus.SUCCEEDED);
        assertThat(result.getProviderReferenceId()).startsWith("ARN-MOCK-");
        assertThat(result.getErrorCode()).isNull();
        assertThat(result.isTransientError()).isFalse();
        assertThat(result.getResponseMetadata()).containsKey("mockExecuted");
        assertThat(result.getResponseMetadata().get("mockExecuted")).isEqualTo(true);
    }

    @ParameterizedTest(name = "Directive {0} should map to ErrorCode {1} with isTransient={2}")
    @CsvSource({
            "AUTH_REQUIRED, AUTH_REQUIRED, false",
            "FORBIDDEN, FORBIDDEN, false",
            "VALIDATION_FAILED, VALIDATION_FAILED, false",
            "RATE_LIMITED, RATE_LIMITED, true",
            "TIMEOUT, TIMEOUT, true",
            "PROVIDER_UNAVAILABLE, PROVIDER_UNAVAILABLE, true"
    })
    @DisplayName("Mock Provider should accurately simulate specified failure outcomes")
    void testMockSimulatedFailures(String directive, String expectedErrorCodeStr, boolean expectedTransient) {
        GovErrorCode expectedErrorCode = GovErrorCode.valueOf(expectedErrorCodeStr);

        GovIntegrationRequest request = GovIntegrationRequest.builder()
                .organizationId(testOrgId)
                .providerType(GovProviderType.INCOME_TAX)
                .operationType("VERIFY_ITR")
                .correlationId("corr-err")
                .idempotencyKey("idemp-err")
                .requestData(Map.of("mockOutcome", directive))
                .build();

        GovIntegrationResult result = mockAdapter.execute(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(expectedErrorCode);
        assertThat(result.isTransientError()).isEqualTo(expectedTransient);
        assertThat(result.getErrorMessage()).containsIgnoringCase("Mock " + directive.replace("_", " "));
    }

    @Test
    @DisplayName("Mock Provider health check should return UP status")
    void testMockHealthCheck() {
        GovProviderHealth health = mockAdapter.checkHealth();
        assertThat(health.getStatus()).isEqualTo(GovProviderHealth.Status.UP);
        assertThat(health.getAdapterCode()).isEqualTo(MockProviderAdapter.ADAPTER_CODE);
        assertThat(health.getMessage()).contains("operational");
    }
}
