package com.taxoryn.module.gov.reliability.model;

import com.taxoryn.module.gov.model.GovErrorCode;

/**
 * Normalized failure classification across government integration operations.
 * Segregates transient/recoverable errors from permanent or policy rejections.
 */
public enum GovFailureClassification {

    /**
     * Transient network or provider errors eligible for retry (timeout, 429, 502, 503, 504, connection reset).
     */
    TRANSIENT(true),

    /**
     * Permanent deterministic errors (400 Bad Request, 404 Not Found, 422 Unprocessable, unsupported operation).
     */
    PERMANENT(false),

    /**
     * Authentication or authorization failures (401 Unauthorized, token expired, invalid OTP, revoked consent).
     */
    AUTHENTICATION(false),

    /**
     * Payload or parameter validation errors (schema violation, invalid PAN/TAN/GSTIN syntax).
     */
    VALIDATION(false),

    /**
     * Business or domain rule violations (period already filed, duplicate challan, invalid FY).
     */
    BUSINESS(false),

    /**
     * Ambiguous or unclassified errors requiring manual or state reconciliation.
     */
    UNKNOWN(false);

    private final boolean retryable;

    GovFailureClassification(boolean retryable) {
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }

    /**
     * Maps a normalized GovErrorCode to its failure classification.
     */
    public static GovFailureClassification fromErrorCode(GovErrorCode errorCode) {
        if (errorCode == null) {
            return UNKNOWN;
        }
        return switch (errorCode) {
            case TIMEOUT, RATE_LIMITED, PROVIDER_UNAVAILABLE -> TRANSIENT;
            case AUTH_REQUIRED, FORBIDDEN -> AUTHENTICATION;
            case VALIDATION_FAILED -> VALIDATION;
            case DUPLICATE_SUBMISSION, NOT_FOUND -> BUSINESS;
            case UNKNOWN -> UNKNOWN;
        };
    }

    /**
     * Maps an HTTP status code to its failure classification.
     */
    public static GovFailureClassification fromHttpStatus(int httpStatusCode) {
        return switch (httpStatusCode) {
            case 408, 429, 502, 503, 504 -> TRANSIENT;
            case 401, 403 -> AUTHENTICATION;
            case 400 -> VALIDATION;
            case 404, 409, 422 -> BUSINESS;
            default -> httpStatusCode >= 500 ? TRANSIENT : PERMANENT;
        };
    }
}
