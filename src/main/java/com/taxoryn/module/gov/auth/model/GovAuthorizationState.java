package com.taxoryn.module.gov.auth.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Provider-neutral state of the interactive government authorization flow.
 * Distinguishes the interaction stage (e.g. user action required) from overall connection authentication status.
 */
@Schema(description = "Government Authorization Flow State")
public enum GovAuthorizationState {

    @Schema(description = "Flow has not been initiated")
    NOT_STARTED,

    @Schema(description = "Authorization flow started, awaiting provider initialization")
    AUTHORIZATION_REQUIRED,

    @Schema(description = "Interactive user action required (e.g., OAuth portal login, OTP/EVC entry, DSC signing)")
    USER_ACTION_REQUIRED,

    @Schema(description = "Authorization challenge submitted, provider verification in progress")
    AUTHORIZATION_IN_PROGRESS,

    @Schema(description = "Authorization completed successfully and valid session established")
    AUTHORIZATION_COMPLETED,

    @Schema(description = "Authorization failed or rejected by provider/user")
    AUTHORIZATION_FAILED,

    @Schema(description = "Authorization session has expired")
    EXPIRED,

    @Schema(description = "Authorization flow or session was explicitly cancelled / revoked")
    CANCELLED;

    public boolean isTerminal() {
        return this == AUTHORIZATION_COMPLETED
                || this == AUTHORIZATION_FAILED
                || this == EXPIRED
                || this == CANCELLED;
    }

    public boolean isPending() {
        return this == NOT_STARTED
                || this == AUTHORIZATION_REQUIRED
                || this == USER_ACTION_REQUIRED
                || this == AUTHORIZATION_IN_PROGRESS;
    }

    public boolean isUserActionRequired() {
        return this == USER_ACTION_REQUIRED;
    }
}
