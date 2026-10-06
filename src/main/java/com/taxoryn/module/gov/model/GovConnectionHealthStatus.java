package com.taxoryn.module.gov.model;

/**
 * Health states of a government connection/provider handshake.
 */
public enum GovConnectionHealthStatus {
    /**
     * Initial state prior to any health probe or handshake execution.
     */
    UNKNOWN,

    /**
     * Provider gateway is fully reachable and connection is healthy.
     */
    HEALTHY,

    /**
     * Provider reachable but authentication / OTP / re-consent is required.
     */
    AUTH_REQUIRED,

    /**
     * Provider gateway is down, unreachable, or undergoing scheduled maintenance.
     */
    UNAVAILABLE,

    /**
     * High latency, intermittent rate-limiting, or degraded gateway performance.
     */
    DEGRADED,

    /**
     * Handshake failed due to configuration error, timeout, or unexpected exception.
     */
    ERROR
}
