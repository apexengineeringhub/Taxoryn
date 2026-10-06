package com.taxoryn.module.consent.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Taxpayer Consent and Delegation Lifecycle Status")
public enum ConsentStatus {

    @Schema(description = "Consent draft created, not yet finalized")
    DRAFT,

    @Schema(description = "Consent created and awaiting taxpayer / client authorization")
    PENDING,

    @Schema(description = "Consent authorized and actively usable")
    ACTIVE,

    @Schema(description = "Consent validity period has expired")
    EXPIRED,

    @Schema(description = "Consent was explicitly revoked by taxpayer or administrator")
    REVOKED,

    @Schema(description = "Consent request was rejected by taxpayer")
    REJECTED;

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isTerminal() {
        return this == EXPIRED || this == REVOKED || this == REJECTED;
    }

    public boolean isPending() {
        return this == DRAFT || this == PENDING;
    }
}
