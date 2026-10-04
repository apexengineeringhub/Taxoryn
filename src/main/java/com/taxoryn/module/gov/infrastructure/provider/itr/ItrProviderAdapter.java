package com.taxoryn.module.gov.infrastructure.provider.itr;

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
 * Income Tax (ITR) subsystem provider adapter implementing GovernmentProviderAdapter SPI.
 * Provides deterministic simulation of Income Tax Department e-filing gateway operations and handshakes without external network calls.
 */
@Slf4j
@Component
public class ItrProviderAdapter implements GovernmentProviderAdapter {

    public static final String ADAPTER_CODE = "ITR_MOCK_ADAPTER";

    @Override
    public GovProviderType getProviderType() {
        return GovProviderType.INCOME_TAX;
    }

    @Override
    public String getAdapterCode() {
        return ADAPTER_CODE;
    }

    @Override
    public GovProviderHealth checkHealth() {
        return GovProviderHealth.up(GovProviderType.INCOME_TAX, ADAPTER_CODE, "Income Tax mock gateway is operational");
    }

    @Override
    public GovHandshakeResult handshake(GovHandshakeRequest request) {
        log.info("[ITR_ADAPTER] Performing handshake for connection id={}", request != null ? request.getConnectionId() : null);

        String directive = "SUCCESS";
        if (request != null && request.getMetadata() != null && request.getMetadata().containsKey("mockOutcome")) {
            directive = String.valueOf(request.getMetadata().get("mockOutcome"));
        }

        return switch (directive.toUpperCase()) {
            case "AUTH_REQUIRED" -> GovHandshakeResult.authRequired(
                    GovProviderType.INCOME_TAX, ADAPTER_CODE, "ITD Handshake: Auth session expired, digital signature or OTP required");
            case "PROVIDER_UNAVAILABLE", "UNAVAILABLE" -> GovHandshakeResult.unavailable(
                    GovProviderType.INCOME_TAX, ADAPTER_CODE, "ITD Handshake: Income Tax Portal undergoing scheduled maintenance");
            case "TIMEOUT", "ERROR" -> GovHandshakeResult.error(
                    GovProviderType.INCOME_TAX, ADAPTER_CODE, GovErrorCode.TIMEOUT, "ITD Handshake: Gateway response timed out");
            default -> GovHandshakeResult.healthy(
                    GovProviderType.INCOME_TAX, ADAPTER_CODE, 25L, "ITD Gateway handshake successful");
        };
    }

    @Override
    public GovIntegrationResult execute(GovIntegrationRequest request) {
        log.info("[ITR_ADAPTER] Executing operation: operationType={}, correlationId={}",
                request.getOperationType(), request.getCorrelationId());

        String directive = resolveDirective(request);
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("adapterCode", ADAPTER_CODE);
        responseData.put("providerType", GovProviderType.INCOME_TAX.name());
        responseData.put("timestamp", System.currentTimeMillis());

        if ("AUTH_REQUIRED".equalsIgnoreCase(directive)) {
            return GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    GovProviderType.INCOME_TAX,
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.AUTH_REQUIRED,
                    "Income Tax e-filing session token expired or invalid",
                    false,
                    responseData
            );
        }

        if ("PROVIDER_UNAVAILABLE".equalsIgnoreCase(directive) || "UNAVAILABLE".equalsIgnoreCase(directive)) {
            return GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    GovProviderType.INCOME_TAX,
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.PROVIDER_UNAVAILABLE,
                    "Income Tax e-filing portal unavailable (HTTP 503)",
                    true,
                    responseData
            );
        }

        if ("RATE_LIMITED".equalsIgnoreCase(directive)) {
            return GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    GovProviderType.INCOME_TAX,
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.RATE_LIMITED,
                    "Too many requests to Income Tax Department API (HTTP 429)",
                    true,
                    responseData
            );
        }

        if ("TIMEOUT".equalsIgnoreCase(directive)) {
            return GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    GovProviderType.INCOME_TAX,
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.TIMEOUT,
                    "Income Tax Department gateway connection timed out",
                    true,
                    responseData
            );
        }

        if ("NOT_FOUND".equalsIgnoreCase(directive)) {
            return GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    GovProviderType.INCOME_TAX,
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.NOT_FOUND,
                    "Income Tax Department: PAN record not found",
                    false,
                    responseData
            );
        }

        if ("INVALID_PAN".equalsIgnoreCase(directive)) {
            return GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    GovProviderType.INCOME_TAX,
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.VALIDATION_FAILED,
                    "Income Tax Department: Invalid PAN format or checksum",
                    false,
                    responseData
            );
        }

        if ("VALIDATION_FAILED".equalsIgnoreCase(directive)) {
            return GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    GovProviderType.INCOME_TAX,
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.VALIDATION_FAILED,
                    "Income Tax Department: Return payload validation failed against ITD schema",
                    false,
                    responseData
            );
        }

        if ("DUPLICATE_SUBMISSION".equalsIgnoreCase(directive)) {
            String dupAck = "ITD-ACK-DUP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            responseData.put("existingAckNumber", dupAck);
            responseData.put("acknowledgementNumber", dupAck);
            return GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    GovProviderType.INCOME_TAX,
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.DUPLICATE_SUBMISSION,
                    "Income Tax Department: Duplicate submission detected. Return already filed for this PAN and Assessment Year",
                    false,
                    responseData
            );
        }

        if ("ERROR".equalsIgnoreCase(directive) || "UNKNOWN".equalsIgnoreCase(directive)) {
            return GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    GovProviderType.INCOME_TAX,
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.UNKNOWN,
                    "Income Tax Department internal gateway error (HTTP 500)",
                    false,
                    responseData
            );
        }

        String opType = request.getOperationType() != null ? request.getOperationType().toUpperCase() : "UNKNOWN";

        return switch (opType) {
            case "VERIFY_PAN" -> executeVerifyPan(request, responseData);
            case "ITR_RETURN_PREPARATION" -> executeReturnPreparation(request, responseData);
            case "ITR_RETURN_SUBMISSION" -> executeReturnSubmission(request, responseData);
            case "ITR_RETURN_STATUS" -> executeReturnStatus(request, responseData);
            default -> executeGenericOperation(request, responseData);
        };
    }

    private GovIntegrationResult executeVerifyPan(GovIntegrationRequest request, Map<String, Object> data) {
        String pan = request.getRequestData() != null && request.getRequestData().containsKey("pan")
                ? String.valueOf(request.getRequestData().get("pan")).toUpperCase()
                : "AAAAA0000A";

        data.put("pan", pan);
        data.put("status", "ACTIVE");
        data.put("panStatus", "OPERATIVE");
        data.put("taxpayerName", "Apex Enterprise Solutions");
        data.put("category", "COMPANY");
        data.put("aadhaarSeedingStatus", "LINKED");
        data.put("jurisdictionAssessingOfficer", "WARD 12(1), MUMBAI");

        String ackNumber = "ITD-PAN-ACK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        data.put("acknowledgementNumber", ackNumber);

        return GovIntegrationResult.success(
                null,
                request.getOrganizationId(),
                GovProviderType.INCOME_TAX,
                request.getOperationType(),
                request.getCorrelationId(),
                request.getIdempotencyKey(),
                ackNumber,
                data
        );
    }

    private GovIntegrationResult executeReturnPreparation(GovIntegrationRequest request, Map<String, Object> data) {
        data.put("status", "PREPARED");
        data.put("schemaVersion", "1.0");
        String refId = "PREP-ITR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        data.put("providerReferenceId", refId);

        return GovIntegrationResult.success(
                null,
                request.getOrganizationId(),
                GovProviderType.INCOME_TAX,
                request.getOperationType(),
                request.getCorrelationId(),
                request.getIdempotencyKey(),
                refId,
                data
        );
    }

    private GovIntegrationResult executeReturnSubmission(GovIntegrationRequest request, Map<String, Object> data) {
        String ackNumber = "ITD-ACK-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        data.put("acknowledgementNumber", ackNumber);
        data.put("submissionStatus", "SUBMITTED");
        data.put("filingStatus", "SUBMITTED");
        data.put("submissionReference", "ITD-REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        return GovIntegrationResult.success(
                null,
                request.getOrganizationId(),
                GovProviderType.INCOME_TAX,
                request.getOperationType(),
                request.getCorrelationId(),
                request.getIdempotencyKey(),
                ackNumber,
                data
        );
    }

    private GovIntegrationResult executeReturnStatus(GovIntegrationRequest request, Map<String, Object> data) {
        String mockStatus = "PROCESSED";
        if (request.getRequestData() != null && request.getRequestData().containsKey("mockStatus")) {
            mockStatus = String.valueOf(request.getRequestData().get("mockStatus")).toUpperCase();
        }

        data.put("filingStatus", mockStatus);
        data.put("providerStatus", mockStatus);

        return GovIntegrationResult.success(
                null,
                request.getOrganizationId(),
                GovProviderType.INCOME_TAX,
                request.getOperationType(),
                request.getCorrelationId(),
                request.getIdempotencyKey(),
                "ITD-STATUS-REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                data
        );
    }

    private GovIntegrationResult executeGenericOperation(GovIntegrationRequest request, Map<String, Object> data) {
        String ackNumber = "ITD-OP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        data.put("operationStatus", "COMPLETED");

        return GovIntegrationResult.success(
                null,
                request.getOrganizationId(),
                GovProviderType.INCOME_TAX,
                request.getOperationType(),
                request.getCorrelationId(),
                request.getIdempotencyKey(),
                ackNumber,
                data
        );
    }

    private String resolveDirective(GovIntegrationRequest request) {
        if (request.getRequestData() == null) {
            return "SUCCESS";
        }
        if (request.getRequestData().containsKey("mockOutcome")) {
            return String.valueOf(request.getRequestData().get("mockOutcome"));
        }
        if (request.getRequestData().containsKey("directive")) {
            return String.valueOf(request.getRequestData().get("directive"));
        }
        return "SUCCESS";
    }
}
