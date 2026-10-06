package com.taxoryn.module.gov.auth.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Government Authentication Lifecycle Status")
public enum GovAuthStatus {
    AUTHENTICATION_STARTED,
    AUTHENTICATION_PENDING,
    AUTHENTICATED,
    AUTHENTICATION_FAILED,
    EXPIRED,
    REVOKED;

    public boolean isTerminal() {
        return this == AUTHENTICATION_FAILED || this == EXPIRED || this == REVOKED;
    }

    public boolean isActive() {
        return this == AUTHENTICATED;
    }

    public boolean isPending() {
        return this == AUTHENTICATION_STARTED || this == AUTHENTICATION_PENDING;
    }
}
