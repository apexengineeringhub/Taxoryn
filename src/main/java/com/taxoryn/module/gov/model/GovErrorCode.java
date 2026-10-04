package com.taxoryn.module.gov.model;

/**
 * Normalized error categories across all government integration providers.
 */
public enum GovErrorCode {
    /**
     * Authentication required or expired session / OTP / token.
     */
    AUTH_REQUIRED(false),

    /**
     * Forbidden: PAN/TAN mismatch, insufficient practitioner authorization.
     */
    FORBIDDEN(false),

    /**
     * Payload validation error: Schema failure, calculation mismatch, missing mandatory field.
     */
    VALIDATION_FAILED(false),

    /**
     * Provider rate limit exceeded (HTTP 429).
     */
    RATE_LIMITED(true),

    /**
     * Provider system temporarily down or maintenance mode (HTTP 502/503/504).
     */
    PROVIDER_UNAVAILABLE(true),

    /**
     * Connection or read timeout during external communication.
     */
    TIMEOUT(true),

    /**
     * Unclassified or unexpected provider exception.
     */
    UNKNOWN(false);

    private final boolean transientError;

    GovErrorCode(boolean transientError) {
        this.transientError = transientError;
    }

    /**
     * Indicates whether operations failing with this error code can be safely retried.
     */
    public boolean isTransient() {
        return transientError;
    }
}
