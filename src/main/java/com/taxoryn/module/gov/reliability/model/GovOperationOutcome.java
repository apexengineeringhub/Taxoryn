package com.taxoryn.module.gov.reliability.model;

import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.model.GovOperationStatus;

/**
 * Standardized high-level operation outcomes for government integrations.
 */
public enum GovOperationOutcome {

    /**
     * Operation succeeded and was acknowledged by the government provider.
     */
    SUCCESS,

    /**
     * Provider accepted request for asynchronous processing (e.g. bulk batch filing).
     */
    ACCEPTED,

    /**
     * Request is actively being processed by the provider or gateway.
     */
    PROCESSING,

    /**
     * Transient error occurred; operation is eligible for retry.
     */
    RETRYABLE_FAILURE,

    /**
     * Provider quota / rate limit (HTTP 429) was encountered.
     */
    RATE_LIMITED,

    /**
     * Network or gateway timeout occurred during communication.
     */
    TIMEOUT,

    /**
     * Provider portal or gateway is temporarily unavailable (HTTP 502/503/504).
     */
    PROVIDER_UNAVAILABLE,

    /**
     * User authentication or consent delegation is required or expired.
     */
    AUTHENTICATION_REQUIRED,

    /**
     * Request payload or parameter validation failed.
     */
    VALIDATION_FAILURE,

    /**
     * Domain or tax rule rejected the request (e.g. already filed, invalid period).
     */
    BUSINESS_FAILURE,

    /**
     * Duplicate submission detected on provider gateway.
     */
    DUPLICATE,

    /**
     * Ambiguous or undetermined outcome requiring inquiry or reconciliation.
     */
    UNKNOWN;

    /**
     * Normalizes a GovIntegrationResult into a standardized GovOperationOutcome.
     */
    public static GovOperationOutcome fromResult(GovIntegrationResult result) {
        if (result == null) {
            return UNKNOWN;
        }
        if (result.isSuccess()) {
            return SUCCESS;
        }

        if (result.getStatus() == GovOperationStatus.IN_PROGRESS) {
            return PROCESSING;
        }

        GovErrorCode errorCode = result.getErrorCode();
        if (errorCode == null) {
            return result.isTransientError() ? RETRYABLE_FAILURE : UNKNOWN;
        }

        return switch (errorCode) {
            case RATE_LIMITED -> RATE_LIMITED;
            case TIMEOUT -> TIMEOUT;
            case PROVIDER_UNAVAILABLE -> PROVIDER_UNAVAILABLE;
            case AUTH_REQUIRED, FORBIDDEN -> AUTHENTICATION_REQUIRED;
            case VALIDATION_FAILED -> VALIDATION_FAILURE;
            case DUPLICATE_SUBMISSION -> DUPLICATE;
            case NOT_FOUND -> BUSINESS_FAILURE;
            case UNKNOWN -> result.isTransientError() ? RETRYABLE_FAILURE : UNKNOWN;
        };
    }
}
