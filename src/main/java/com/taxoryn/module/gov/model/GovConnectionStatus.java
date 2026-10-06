package com.taxoryn.module.gov.model;

/**
 * Lifecycle states of an organization's connection to a government provider.
 */
public enum GovConnectionStatus {
    /**
     * Initial connection profile created, awaiting credential registration or activation.
     */
    CREATED,

    /**
     * Connection is active, credentials registered and operational.
     */
    ACTIVE,

    /**
     * Connection is temporarily or intentionally deactivated by practice administrator.
     */
    INACTIVE,

    /**
     * Authentication session expired, OTP required, or re-consent needed.
     */
    AUTH_REQUIRED,

    /**
     * Connection has failed authentication or provider handshake.
     */
    FAILED
}
