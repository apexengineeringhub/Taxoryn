package com.taxoryn.module.gst.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lifecycle state of a GST / E-Way / E-Invoice service token or session reference.
 */
@Schema(description = "GST Service Token / Session Reference Lifecycle State")
public enum GstTokenStatus {

    @Schema(description = "Token has not been initiated")
    NOT_AUTHENTICATED,

    @Schema(description = "Interactive user authorization required before token issuance")
    AUTHENTICATION_REQUIRED,

    @Schema(description = "Authentication challenge pending provider verification")
    AUTHENTICATION_PENDING,

    @Schema(description = "Token reference is active and valid for gateway operations")
    ACTIVE,

    @Schema(description = "Token is within expiry grace window (e.g. < 30m remaining) and eligible for refresh")
    EXPIRING,

    @Schema(description = "Token has reached expiry time and cannot be used")
    EXPIRED,

    @Schema(description = "Token requires explicit re-authentication / refresh")
    REFRESH_REQUIRED,

    @Schema(description = "Authentication failed or token invalid")
    FAILED,

    @Schema(description = "Token was explicitly revoked")
    REVOKED;

    public boolean isActive() {
        return this == ACTIVE || this == EXPIRING;
    }

    public boolean isPending() {
        return this == AUTHENTICATION_REQUIRED || this == AUTHENTICATION_PENDING;
    }

    public boolean isTerminal() {
        return this == EXPIRED || this == FAILED || this == REVOKED;
    }
}
