package com.taxoryn.module.gov.dto;

import com.taxoryn.module.gov.model.GovProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Provider-neutral integration request payload for government operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovIntegrationRequest {

    /**
     * Tenant / Organization ID executing the request.
     */
    private UUID organizationId;

    /**
     * Target government provider subsystem (GST, INCOME_TAX, TRACES).
     */
    private GovProviderType providerType;

    /**
     * Logical operation identifier (e.g. "FETCH_STATUS", "SUBMIT_RETURN", "VERIFY_REGISTRATION").
     */
    private String operationType;

    /**
     * Optional business entity type reference (e.g. "GST_RETURN", "ITR_RETURN", "TDS_RETURN").
     */
    private String businessEntityType;

    /**
     * Optional primary key ID of the underlying business entity.
     */
    private UUID businessEntityId;

    /**
     * Unique correlation trace ID for distributed tracing across logs.
     */
    private String correlationId;

    /**
     * Deterministic idempotency key. If omitted, generated automatically from request fingerprint.
     */
    private String idempotencyKey;

    /**
     * Deterministic hash/fingerprint of the underlying compliance payload.
     */
    private String payloadFingerprint;

    /**
     * Structured request data passed to the adapter.
     */
    @Builder.Default
    private Map<String, Object> requestData = new HashMap<>();

    /**
     * Contextual execution metadata (e.g., client IP, user agent, timeout override).
     */
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();
}
