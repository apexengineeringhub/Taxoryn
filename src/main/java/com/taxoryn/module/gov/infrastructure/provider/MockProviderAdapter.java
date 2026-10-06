package com.taxoryn.module.gov.infrastructure.provider;

import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.model.GovProviderHealth;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.spi.GovernmentProviderAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mock provider adapter used for local testing, CI test suites, and architecture verification.
 * Supports deterministic simulation of success, validation errors, timeouts, rate limits, and auth failures.
 * Makes zero external network requests.
 */
@Slf4j
@Component
public class MockProviderAdapter implements GovernmentProviderAdapter {

    public static final String ADAPTER_CODE = "MOCK_PROVIDER";

    @Override
    public GovProviderType getProviderType() {
        return GovProviderType.GST; // Default type, can handle requests across types in mock mode
    }

    @Override
    public String getAdapterCode() {
        return ADAPTER_CODE;
    }

    @Override
    public GovIntegrationResult execute(GovIntegrationRequest request) {
        log.info("[MOCK_PROVIDER] Executing simulated request: providerType={}, operationType={}, correlationId={}",
                request.getProviderType(), request.getOperationType(), request.getCorrelationId());

        String directive = resolveDirective(request);
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("mockExecuted", true);
        responseData.put("receivedDirective", directive);
        responseData.put("timestamp", System.currentTimeMillis());

        return switch (directive.toUpperCase()) {
            case "AUTH_REQUIRED" -> GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    request.getProviderType(),
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.AUTH_REQUIRED,
                    "Mock Auth Required: Simulated session or OTP expired",
                    false,
                    responseData
            );
            case "FORBIDDEN" -> GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    request.getProviderType(),
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.FORBIDDEN,
                    "Mock Forbidden: Unauthorized taxpayer identifier or access denied",
                    false,
                    responseData
            );
            case "VALIDATION_FAILED" -> GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    request.getProviderType(),
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.VALIDATION_FAILED,
                    "Mock Validation Failed: Schema constraint violation or invalid parameter format",
                    false,
                    responseData
            );
            case "RATE_LIMITED" -> GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    request.getProviderType(),
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.RATE_LIMITED,
                    "Mock Rate Limited: HTTP 429 quota threshold reached",
                    true,
                    responseData
            );
            case "TIMEOUT" -> GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    request.getProviderType(),
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.TIMEOUT,
                    "Mock Timeout: Simulated read timeout exceeding 30000ms",
                    true,
                    responseData
            );
            case "PROVIDER_UNAVAILABLE" -> GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    request.getProviderType(),
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.PROVIDER_UNAVAILABLE,
                    "Mock Provider Unavailable: Gateway HTTP 503 scheduled maintenance",
                    true,
                    responseData
            );
            default -> { // Default: SUCCESS
                String simulatedRef = "ARN-MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                responseData.put("ackNumber", simulatedRef);
                responseData.put("statusCode", "SUCCESS");
                yield GovIntegrationResult.success(
                        null,
                        request.getOrganizationId(),
                        request.getProviderType(),
                        request.getOperationType(),
                        request.getCorrelationId(),
                        request.getIdempotencyKey(),
                        simulatedRef,
                        responseData
                );
            }
        };
    }

    @Override
    public GovProviderHealth checkHealth() {
        return GovProviderHealth.up(GovProviderType.GST, ADAPTER_CODE, "Mock provider adapter is fully operational");
    }

    @Override
    public com.taxoryn.module.gov.dto.GovHandshakeResult handshake(com.taxoryn.module.gov.dto.GovHandshakeRequest request) {
        String directive = "SUCCESS";
        if (request != null && request.getMetadata() != null && request.getMetadata().containsKey("mockOutcome")) {
            directive = String.valueOf(request.getMetadata().get("mockOutcome"));
        }

        GovProviderType pType = request != null && request.getProviderType() != null ? request.getProviderType() : GovProviderType.GST;

        return switch (directive.toUpperCase()) {
            case "AUTH_REQUIRED" -> com.taxoryn.module.gov.dto.GovHandshakeResult.authRequired(
                    pType, ADAPTER_CODE, "Mock Handshake: Session expired or OTP re-authentication required");
            case "PROVIDER_UNAVAILABLE" -> com.taxoryn.module.gov.dto.GovHandshakeResult.unavailable(
                    pType, ADAPTER_CODE, "Mock Handshake: Provider gateway is currently unavailable");
            case "TIMEOUT", "ERROR" -> com.taxoryn.module.gov.dto.GovHandshakeResult.error(
                    pType, ADAPTER_CODE, GovErrorCode.TIMEOUT, "Mock Handshake: Handshake timed out after 30000ms");
            default -> com.taxoryn.module.gov.dto.GovHandshakeResult.healthy(
                    pType, ADAPTER_CODE, 15L, "Mock gateway handshake successful and operational");
        };
    }

    private String resolveDirective(GovIntegrationRequest request) {
        if (request.getRequestData() != null && request.getRequestData().containsKey("mockOutcome")) {
            return String.valueOf(request.getRequestData().get("mockOutcome"));
        }
        if (request.getMetadata() != null && request.getMetadata().containsKey("mockOutcome")) {
            return request.getMetadata().get("mockOutcome");
        }
        return "SUCCESS";
    }
}
