package com.taxoryn.module.gov.reconciliation.model;

/**
 * Normalized authoritative status returned by external government providers during reconciliation.
 */
public enum GovAuthoritativeStatus {
    /**
     * Provider acknowledges operation is accepted and currently undergoing validation/processing.
     */
    PROCESSING,

    /**
     * Provider confirms operation has completed successfully (e.g. Return Filed / Acknowledged).
     * This is the ONLY provider state that permits transitioning a submission to SUCCEEDED.
     */
    FILED,

    /**
     * Provider has reviewed and rejected the submission (e.g. validation error or schema mismatch).
     */
    REJECTED,

    /**
     * Provider internal execution or gateway failed terminally.
     */
    FAILED,

    /**
     * Provider response is ambiguous, unavailable, or unverified. Preserves recoverable state.
     */
    UNKNOWN,

    /**
     * Provider requires explicit taxpayer intervention (e.g. re-authentication, EVC verification, OTP).
     */
    REQUIRES_ACTION
}
