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

        if ("NOT_FOUND".equalsIgnoreCase(directive)) {
            return GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    GovProviderType.GST,
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.NOT_FOUND,
                    "GSTIN not found on GST Portal",
                    false,
                    responseData
            );
        }

        if ("TIMEOUT".equalsIgnoreCase(directive)) {
            return GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    GovProviderType.GST,
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.TIMEOUT,
                    "GST Portal request timed out",
                    true,
                    responseData
            );
        }

        if ("FORBIDDEN".equalsIgnoreCase(directive)) {
            return GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    GovProviderType.GST,
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.FORBIDDEN,
                    "Access denied: Unauthorized GSTIN request",
                    false,
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

        if ("DUPLICATE_SUBMISSION".equalsIgnoreCase(directive)) {
            String duplicateAck = "MOCK-GST-ACK-DUP-" + (request.getCorrelationId() != null
                    ? request.getCorrelationId().substring(0, Math.min(8, request.getCorrelationId().length())).toUpperCase()
                    : "00000000");
            responseData.put("existingAckNumber", duplicateAck);
            responseData.put("status", "DUPLICATE");
            return GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    GovProviderType.GST,
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.DUPLICATE_SUBMISSION,
                    "GST return already submitted for this GSTIN and return period",
                    false,
                    responseData
            );
        }

        // Default: Mock Successful Response
        String gstin = request.getRequestData() != null && request.getRequestData().containsKey("gstin")
                ? String.valueOf(request.getRequestData().get("gstin"))
                : "27AAAAA0000A1Z5";

        if ("GST_RETURN_SUBMISSION".equalsIgnoreCase(request.getOperationType())
                || "SUBMIT_GST_RETURN".equalsIgnoreCase(request.getOperationType())) {
            String returnType = request.getRequestData() != null && request.getRequestData().containsKey("returnType")
                    ? String.valueOf(request.getRequestData().get("returnType")) : "GSTR1";
            String returnPeriod = request.getRequestData() != null && request.getRequestData().containsKey("returnPeriod")
                    ? String.valueOf(request.getRequestData().get("returnPeriod")) : "042026";
            String fingerprint = request.getRequestData() != null && request.getRequestData().containsKey("payloadFingerprint")
                    ? String.valueOf(request.getRequestData().get("payloadFingerprint")) : null;

            String ackSeed = fingerprint != null && fingerprint.length() >= 8
                    ? fingerprint.substring(0, 8).toUpperCase()
                    : (request.getCorrelationId() != null
                    ? request.getCorrelationId().substring(0, Math.min(8, request.getCorrelationId().length())).toUpperCase()
                    : UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            String simulatedAck = "MOCK-GST-ACK-" + ackSeed;
            String providerRef = "REF-GST-SUB-" + ackSeed;

            responseData.put("gstin", gstin);
            responseData.put("returnType", returnType);
            responseData.put("returnPeriod", returnPeriod);
            responseData.put("ackNumber", simulatedAck);
            responseData.put("providerReference", providerRef);
            responseData.put("status", "SUBMITTED");
            responseData.put("submissionTimestamp", System.currentTimeMillis());
            responseData.put("payloadFingerprint", fingerprint);

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

        if ("GST_RETURN_STATUS".equalsIgnoreCase(request.getOperationType())
                || "CHECK_GST_RETURN_STATUS".equalsIgnoreCase(request.getOperationType())
                || "GET_GST_RETURN_STATUS".equalsIgnoreCase(request.getOperationType())) {
            String returnType = request.getRequestData() != null && request.getRequestData().containsKey("returnType")
                    ? String.valueOf(request.getRequestData().get("returnType")) : "GSTR1";
            String returnPeriod = request.getRequestData() != null && request.getRequestData().containsKey("returnPeriod")
                    ? String.valueOf(request.getRequestData().get("returnPeriod")) : "042026";
            String ackNumber = request.getRequestData() != null && request.getRequestData().containsKey("ackNumber")
                    ? String.valueOf(request.getRequestData().get("ackNumber")) : null;

            String statusDirective = "FILED";
            if (request.getRequestData() != null && request.getRequestData().containsKey("mockStatus")) {
                statusDirective = String.valueOf(request.getRequestData().get("mockStatus"));
            } else if (request.getMetadata() != null && request.getMetadata().containsKey("mockStatus")) {
                statusDirective = request.getMetadata().get("mockStatus");
            }

            String ackSeed = ackNumber != null ? ackNumber.replace("MOCK-GST-ACK-", "")
                    : (request.getCorrelationId() != null
                    ? request.getCorrelationId().substring(0, Math.min(8, request.getCorrelationId().length())).toUpperCase()
                    : UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            String finalAck = ackNumber != null ? ackNumber : ("MOCK-GST-ACK-" + ackSeed);
            String providerRef = "REF-GST-STAT-" + ackSeed;

            responseData.put("gstin", gstin);
            responseData.put("returnType", returnType);
            responseData.put("returnPeriod", returnPeriod);
            responseData.put("providerStatus", statusDirective.toUpperCase());
            responseData.put("providerReference", providerRef);
            responseData.put("statusCheckedAt", System.currentTimeMillis());

            switch (statusDirective.toUpperCase()) {
                case "PENDING" -> {
                    responseData.put("message", "Filing received and queued for processing");
                    responseData.put("terminal", false);
                    return GovIntegrationResult.success(
                            null, request.getOrganizationId(), GovProviderType.GST,
                            request.getOperationType(), request.getCorrelationId(),
                            request.getIdempotencyKey(), providerRef, responseData
                    );
                }
                case "PROCESSING" -> {
                    responseData.put("message", "Filing is actively being processed by GSTN");
                    responseData.put("terminal", false);
                    return GovIntegrationResult.success(
                            null, request.getOrganizationId(), GovProviderType.GST,
                            request.getOperationType(), request.getCorrelationId(),
                            request.getIdempotencyKey(), providerRef, responseData
                    );
                }
                case "REJECTED" -> {
                    responseData.put("message", "Return validation failed: Section 8 summary mismatch");
                    responseData.put("terminal", true);
                    return GovIntegrationResult.success(
                            null, request.getOrganizationId(), GovProviderType.GST,
                            request.getOperationType(), request.getCorrelationId(),
                            request.getIdempotencyKey(), providerRef, responseData
                    );
                }
                case "FAILED" -> {
                    responseData.put("message", "Processing failed on GST portal");
                    responseData.put("terminal", true);
                    return GovIntegrationResult.success(
                            null, request.getOrganizationId(), GovProviderType.GST,
                            request.getOperationType(), request.getCorrelationId(),
                            request.getIdempotencyKey(), providerRef, responseData
                    );
                }
                case "UNKNOWN" -> {
                    responseData.put("message", "Unrecognized status from GST portal");
                    responseData.put("terminal", false);
                    return GovIntegrationResult.success(
                            null, request.getOrganizationId(), GovProviderType.GST,
                            request.getOperationType(), request.getCorrelationId(),
                            request.getIdempotencyKey(), providerRef, responseData
                    );
                }
                default -> { // "FILED"
                    responseData.put("providerStatus", "FILED");
                    responseData.put("ackNumber", finalAck);
                    responseData.put("filingDate", java.time.LocalDate.now().toString());
                    responseData.put("message", "Return successfully filed and confirmed on GST portal");
                    responseData.put("terminal", true);
                    return GovIntegrationResult.success(
                            null, request.getOrganizationId(), GovProviderType.GST,
                            request.getOperationType(), request.getCorrelationId(),
                            request.getIdempotencyKey(), finalAck, responseData
                    );
                }
            }
        }

        if ("GST_RETURN_PREPARATION".equalsIgnoreCase(request.getOperationType())
                || "PREPARE_GST_RETURN".equalsIgnoreCase(request.getOperationType())) {
            String prepRef = "PREP-GST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String returnType = request.getRequestData() != null && request.getRequestData().containsKey("returnType")
                    ? String.valueOf(request.getRequestData().get("returnType")) : "GSTR1";
            String returnPeriod = request.getRequestData() != null && request.getRequestData().containsKey("returnPeriod")
                    ? String.valueOf(request.getRequestData().get("returnPeriod")) : "042026";
            String fingerprint = request.getRequestData() != null && request.getRequestData().containsKey("payloadFingerprint")
                    ? String.valueOf(request.getRequestData().get("payloadFingerprint")) : null;

            responseData.put("gstin", gstin);
            responseData.put("returnType", returnType);
            responseData.put("returnPeriod", returnPeriod);
            responseData.put("status", "PREPARED");
            responseData.put("providerValidationStatus", "PASSED");
            responseData.put("payloadFingerprint", fingerprint);
            responseData.put("prepReference", prepRef);

            return GovIntegrationResult.success(
                    null,
                    request.getOrganizationId(),
                    GovProviderType.GST,
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    prepRef,
                    responseData
            );
        }

        String simulatedAck = "ARN-GST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String stateCode = gstin.length() >= 2 ? gstin.substring(0, 2) : "27";
        String status = "ACTIVE";
        if (request.getRequestData() != null && request.getRequestData().containsKey("mockStatus")) {
            status = String.valueOf(request.getRequestData().get("mockStatus"));
        }

        responseData.put("gstin", gstin);
        responseData.put("ackNumber", simulatedAck);
        responseData.put("status", status);
        responseData.put("legalName", "Apex Enterprises Private Limited");
        responseData.put("tradeName", "Apex Solutions");
        responseData.put("registrationDate", "2017-07-01");
        responseData.put("registrationType", "REGULAR");
        responseData.put("stateCode", stateCode);
        responseData.put("centerJurisdiction", "COMMISSIONERATE MUMBAI WEST, DIVISION IV, RANGE II");
        responseData.put("stateJurisdiction", "MAHARASHTRA, WARD 101");
        responseData.put("constitutionOfBusiness", "Private Limited Company");
        responseData.put("taxpayerType", "Taxpayer");
        responseData.put("address", "Plot No 42, Bandra Kurla Complex, Mumbai, Maharashtra, 400051");
        responseData.put("lastUpdatedDate", "2026-01-15");

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
