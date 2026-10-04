package com.taxoryn.module.gov.infrastructure.provider.gst;

import com.taxoryn.module.gov.dto.GovHandshakeRequest;
import com.taxoryn.module.gov.dto.GovHandshakeResult;
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
 * GST subsystem provider adapter implementing GovernmentProviderAdapter SPI.
 * Provides deterministic simulation of GSTN gateway operations and handshakes without external network calls.
 */
@Slf4j
@Component
public class GstProviderAdapter implements GovernmentProviderAdapter {

    public static final String ADAPTER_CODE = "GSTN_MOCK_ADAPTER";

    @Override
    public GovProviderType getProviderType() {
        return GovProviderType.GST;
    }

    @Override
    public String getAdapterCode() {
        return ADAPTER_CODE;
    }

    @Override
    public GovProviderHealth checkHealth() {
        return GovProviderHealth.up(GovProviderType.GST, ADAPTER_CODE, "GSTN mock gateway is operational");
    }

    @Override
    public GovHandshakeResult handshake(GovHandshakeRequest request) {
        log.info("[GST_ADAPTER] Performing handshake for connection id={}", request != null ? request.getConnectionId() : null);

        String directive = "SUCCESS";
        if (request != null && request.getMetadata() != null && request.getMetadata().containsKey("mockOutcome")) {
            directive = String.valueOf(request.getMetadata().get("mockOutcome"));
        }

        return switch (directive.toUpperCase()) {
            case "AUTH_REQUIRED" -> GovHandshakeResult.authRequired(
                    GovProviderType.GST, ADAPTER_CODE, "GSTN Handshake: Auth session expired or OTP required");
            case "PROVIDER_UNAVAILABLE" -> GovHandshakeResult.unavailable(
                    GovProviderType.GST, ADAPTER_CODE, "GSTN Handshake: GST Portal undergoing scheduled maintenance");
            case "TIMEOUT", "ERROR" -> GovHandshakeResult.error(
                    GovProviderType.GST, ADAPTER_CODE, GovErrorCode.TIMEOUT, "GSTN Handshake: Gateway response timed out");
            default -> GovHandshakeResult.healthy(
                    GovProviderType.GST, ADAPTER_CODE, 20L, "GSTN Gateway handshake successful");
        };
    }

    @Override
    public GovIntegrationResult execute(GovIntegrationRequest request) {
        log.info("[GST_ADAPTER] Executing operation: operationType={}, correlationId={}",
                request.getOperationType(), request.getCorrelationId());

        String directive = resolveDirective(request);
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("adapterCode", ADAPTER_CODE);
        responseData.put("providerType", GovProviderType.GST.name());
        responseData.put("timestamp", System.currentTimeMillis());

        if ("AUTH_REQUIRED".equalsIgnoreCase(directive)) {
            return GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    GovProviderType.GST,
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.AUTH_REQUIRED,
                    "GSTN session token expired or invalid",
                    false,
                    responseData
            );
        }

        if ("PROVIDER_UNAVAILABLE".equalsIgnoreCase(directive)) {
            return GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    GovProviderType.GST,
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.PROVIDER_UNAVAILABLE,
                    "GSTN portal unavailable (HTTP 503)",
                    true,
                    responseData
            );
        }

        if ("RATE_LIMITED".equalsIgnoreCase(directive)) {
            return GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    GovProviderType.GST,
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.RATE_LIMITED,
                    "GSTN API rate limit exceeded (HTTP 429)",
                    true,
                    responseData
            );
        }

        if ("VALIDATION_FAILED".equalsIgnoreCase(directive)) {
            return GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    GovProviderType.GST,
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.VALIDATION_FAILED,
                    "Invalid GSTIN or schema validation error",
                    false,
                    responseData
            );
        }

        // Default: Mock Successful Response
        String gstin = request.getRequestData() != null && request.getRequestData().containsKey("gstin")
                ? String.valueOf(request.getRequestData().get("gstin"))
                : "27AAAAA0000A1Z5";

        String simulatedAck = "ARN-GST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        responseData.put("gstin", gstin);
        responseData.put("ackNumber", simulatedAck);
        responseData.put("status", "ACTIVE");
        responseData.put("legalName", "Taxoryn Demo Enterprises Pvt Ltd");
        responseData.put("tradeName", "Taxoryn Demo");
        responseData.put("stateCode", gstin.length() >= 2 ? gstin.substring(0, 2) : "27");
        responseData.put("registrationType", "REGULAR");

        return GovIntegrationResult.success(
                null,
                request.getOrganizationId(),
                GovProviderType.GST,
                request.getOperationType(),
                request.getCorrelationId(),
                request.getIdempotencyKey(),
                simulatedAck,
                responseData
        );
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
