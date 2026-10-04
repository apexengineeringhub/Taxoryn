package com.taxoryn.module.gov.infrastructure.provider.tds;

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
 * Tax Deducted at Source (TDS/TCS) subsystem provider adapter implementing GovernmentProviderAdapter SPI.
 * Provides deterministic simulation of TRACES / Income Tax TDS gateway operations and handshakes without external network calls.
 */
@Slf4j
@Component
public class TdsProviderAdapter implements GovernmentProviderAdapter {

    public static final String ADAPTER_CODE = "TDS_MOCK_ADAPTER";

    @Override
    public GovProviderType getProviderType() {
        return GovProviderType.TDS;
    }

    @Override
    public String getAdapterCode() {
        return ADAPTER_CODE;
    }

    @Override
    public GovProviderHealth checkHealth() {
        return GovProviderHealth.up(GovProviderType.TDS, ADAPTER_CODE, "TDS TRACES mock gateway is operational");
    }

    @Override
    public GovHandshakeResult handshake(GovHandshakeRequest request) {
        log.info("[TDS_ADAPTER] Performing handshake for connection id={}", request != null ? request.getConnectionId() : null);

        String directive = "SUCCESS";
        if (request != null && request.getMetadata() != null && request.getMetadata().containsKey("mockOutcome")) {
            directive = String.valueOf(request.getMetadata().get("mockOutcome"));
        }

        return switch (directive.toUpperCase()) {
            case "AUTH_REQUIRED" -> GovHandshakeResult.authRequired(
                    GovProviderType.TDS, ADAPTER_CODE, "TRACES Handshake: Auth session expired or token invalid");
            case "PROVIDER_UNAVAILABLE", "UNAVAILABLE" -> GovHandshakeResult.unavailable(
                    GovProviderType.TDS, ADAPTER_CODE, "TRACES Handshake: TRACES Portal undergoing scheduled maintenance");
            case "TIMEOUT" -> GovHandshakeResult.error(
                    GovProviderType.TDS, ADAPTER_CODE, GovErrorCode.TIMEOUT, "TRACES Handshake: Gateway response timed out");
            case "RATE_LIMITED" -> GovHandshakeResult.error(
                    GovProviderType.TDS, ADAPTER_CODE, GovErrorCode.RATE_LIMITED, "TRACES Handshake: Request rate limit exceeded");
            case "ERROR" -> GovHandshakeResult.error(
                    GovProviderType.TDS, ADAPTER_CODE, GovErrorCode.UNKNOWN, "TRACES Handshake: Gateway error occurred");
            default -> GovHandshakeResult.healthy(
                    GovProviderType.TDS, ADAPTER_CODE, 20L, "TDS TRACES Gateway handshake successful");
        };
    }

    @Override
    public GovIntegrationResult execute(GovIntegrationRequest request) {
        log.info("[TDS_ADAPTER] Executing operation: operationType={}, correlationId={}",
                request.getOperationType(), request.getCorrelationId());

        String directive = resolveDirective(request);
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("adapterCode", ADAPTER_CODE);
        responseData.put("providerType", GovProviderType.TDS.name());
        responseData.put("timestamp", System.currentTimeMillis());

        if ("AUTH_REQUIRED".equalsIgnoreCase(directive)) {
            return GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    GovProviderType.TDS,
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.AUTH_REQUIRED,
                    "TDS TRACES portal session token expired or invalid",
                    false,
                    responseData
            );
        }

        if ("PROVIDER_UNAVAILABLE".equalsIgnoreCase(directive) || "UNAVAILABLE".equalsIgnoreCase(directive)) {
            return GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    GovProviderType.TDS,
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.PROVIDER_UNAVAILABLE,
                    "TDS TRACES portal unavailable (HTTP 503)",
                    true,
                    responseData
            );
        }

        if ("TIMEOUT".equalsIgnoreCase(directive)) {
            return GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    GovProviderType.TDS,
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.TIMEOUT,
                    "Gateway timed out waiting for TRACES response",
                    true,
                    responseData
            );
        }

        if ("RATE_LIMITED".equalsIgnoreCase(directive)) {
            return GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    GovProviderType.TDS,
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.RATE_LIMITED,
                    "TRACES API rate limit exceeded (HTTP 429)",
                    true,
                    responseData
            );
        }

        if ("VALIDATION_FAILED".equalsIgnoreCase(directive)
                || "SCHEMA_VALIDATION_FAILED".equalsIgnoreCase(directive)
                || "INVALID_TAN".equalsIgnoreCase(directive)) {
            return GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    GovProviderType.TDS,
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.VALIDATION_FAILED,
                    "TAN format or checksum rejected by TRACES portal",
                    false,
                    responseData
            );
        }

        if ("NOT_FOUND".equalsIgnoreCase(directive)) {
            return GovIntegrationResult.failure(
                    null,
                    request.getOrganizationId(),
                    GovProviderType.TDS,
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.NOT_FOUND,
                    "TAN record not found in TRACES deductor database",
                    false,
                    responseData
            );
        }

        String opType = request.getOperationType() != null ? request.getOperationType().toUpperCase() : "UNKNOWN";
        String providerReference = "TRACES-" + opType + "-ACK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        if ("VERIFY_TAN".equalsIgnoreCase(opType)) {
            String tan = extractTan(request);
            if ("XXXX99999Z".equalsIgnoreCase(tan) || tan.startsWith("ZZZZ")) {
                return GovIntegrationResult.failure(
                        null,
                        request.getOrganizationId(),
                        GovProviderType.TDS,
                        request.getOperationType(),
                        request.getCorrelationId(),
                        request.getIdempotencyKey(),
                        GovErrorCode.NOT_FOUND,
                        "TAN record " + tan + " not found in TRACES deductor database",
                        false,
                        responseData
                );
            }

            responseData.put("tan", tan);
            if (tan.startsWith("BLRN")) {
                responseData.put("deductorName", "Bangalore Tech Corp Limited");
                responseData.put("category", "COMPANY");
                responseData.put("status", "ACTIVE");
                responseData.put("tanStatus", "VALID");
                responseData.put("tracesStatus", "REGISTERED_ACTIVE");
                responseData.put("pan", "AABCB5678D");
                responseData.put("state", "KARNATAKA");
                responseData.put("pinCode", "560001");
                responseData.put("address", "100 MG Road, Bangalore, Karnataka 560001");
            } else if (tan.startsWith("DELA")) {
                responseData.put("deductorName", "Delhi Consulting Services Private Limited");
                responseData.put("category", "COMPANY");
                responseData.put("status", "ACTIVE");
                responseData.put("tanStatus", "VALID");
                responseData.put("tracesStatus", "REGISTERED_ACTIVE");
                responseData.put("pan", "AADCD9988E");
                responseData.put("state", "DELHI");
                responseData.put("pinCode", "110001");
                responseData.put("address", "Connaught Place, New Delhi 110001");
            } else {
                responseData.put("deductorName", "Acme Enterprises Private Limited");
                responseData.put("category", "COMPANY");
                responseData.put("status", "ACTIVE");
                responseData.put("tanStatus", "VALID");
                responseData.put("tracesStatus", "REGISTERED_ACTIVE");
                responseData.put("pan", "AAACA1234C");
                responseData.put("state", "MAHARASHTRA");
                responseData.put("pinCode", "400001");
                responseData.put("address", "Plot 42, Bandra Kurla Complex, Mumbai, Maharashtra 400001");
            }
        } else if ("CHALLAN_STATUS".equalsIgnoreCase(opType)) {
            responseData.put("status", "MATCHED");
            responseData.put("bsrCode", "0210001");
            responseData.put("challanDate", "2026-04-15");
            responseData.put("challanNo", "10023");
            responseData.put("cin", "02100011504202610023");
        } else if ("TDS_RETURN_PREPARATION".equalsIgnoreCase(opType) || "PREPARE_TDS_RETURN".equalsIgnoreCase(opType)) {
            String tan = extractTan(request);
            responseData.put("tan", tan);
            responseData.put("status", "PREPARED");
            responseData.put("fvuStatus", "VALIDATED");
            responseData.put("fvuVersion", "8.2");
            responseData.put("message", "TDS return prepared and validated successfully against TRACES FVU schema");
            if (request.getRequestData() != null) {
                if (request.getRequestData().containsKey("formType")) {
                    responseData.put("formType", request.getRequestData().get("formType"));
                }
                if (request.getRequestData().containsKey("quarter")) {
                    responseData.put("quarter", request.getRequestData().get("quarter"));
                }
                if (request.getRequestData().containsKey("financialYear")) {
                    responseData.put("financialYear", request.getRequestData().get("financialYear"));
                }
                if (request.getRequestData().containsKey("payloadFingerprint")) {
                    responseData.put("payloadFingerprint", request.getRequestData().get("payloadFingerprint"));
                }
            }
        } else {
            responseData.put("status", "SUCCESS");
            responseData.put("message", "TDS operation " + opType + " executed successfully");
        }

        return GovIntegrationResult.success(
                null,
                request.getOrganizationId(),
                GovProviderType.TDS,
                request.getOperationType(),
                request.getCorrelationId(),
                request.getIdempotencyKey(),
                providerReference,
                responseData
        );
    }

    private String resolveDirective(GovIntegrationRequest request) {
        if (request.getRequestData() != null && request.getRequestData().containsKey("mockOutcome")) {
            return String.valueOf(request.getRequestData().get("mockOutcome"));
        }
        if (request.getMetadata() != null && request.getMetadata().containsKey("mockOutcome")) {
            return String.valueOf(request.getMetadata().get("mockOutcome"));
        }
        return "SUCCESS";
    }

    private String extractTan(GovIntegrationRequest request) {
        if (request.getRequestData() != null && request.getRequestData().containsKey("tan")) {
            return String.valueOf(request.getRequestData().get("tan")).toUpperCase();
        }
        return "MUMB12345A";
    }
}
