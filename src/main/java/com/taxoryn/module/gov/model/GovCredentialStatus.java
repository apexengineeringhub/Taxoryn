package com.taxoryn.module.gov.model;

/**
 * Validation and lifecycle status of a registered government credential reference.
 */
public enum GovCredentialStatus {
    /**
     * Credential is active, valid, and available for integration authentication.
     */
    VALID,

    /**
     * Credential token/secret has expired based on timestamp or provider response.
     */
    EXPIRED,

    /**
     * Credential was revoked by practice administrator or invalidated.
     */
    REVOKED,

    /**
     * Credential failed authentication checks.
     */
    INVALID
}
