package com.taxoryn.module.gov.auth.dto;

import com.taxoryn.module.gov.auth.model.GovAuthMethod;
import com.taxoryn.module.gov.auth.model.GovAuthStatus;
import com.taxoryn.module.gov.model.GovProviderType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Safe Government Authentication Session Details")
public class GovAuthSessionDto {

    @Schema(description = "Authentication Session ID")
    private UUID sessionId;

    @Schema(description = "Target Government Connection ID")
    private UUID connectionId;

    @Schema(description = "Provider Subsystem Type (GST, INCOME_TAX, TDS)")
    private GovProviderType providerType;

    @Schema(description = "Authentication Method (OAUTH2, OTP, EVC, DSC)")
    private GovAuthMethod authMethod;

    @Schema(description = "Current Authentication Lifecycle Status")
    private GovAuthStatus status;

    @Schema(description = "Whether interactive user action is required (e.g., OTP entry, OAuth login redirect)")
    private boolean requiresUserAction;

    @Schema(description = "Safe human-readable instructions or portal redirect prompt")
    private String actionPrompt;

    @Schema(description = "Session Expiration Timestamp")
    private Instant expiresAt;

    @Schema(description = "Timestamp when authenticated successfully")
    private Instant authenticatedAt;

    @Schema(description = "Last Activity Timestamp")
    private Instant lastActivityAt;

    @Schema(description = "Safe Error / Failure Code")
    private String failureCode;

    @Schema(description = "Safe Diagnostic Failure Message")
    private String safeFailureMessage;

    @Schema(description = "Correlation ID for distributed tracing")
    private String correlationId;

    @Schema(description = "Session Creation Timestamp")
    private Instant createdAt;

    @Schema(description = "Session Last Updated Timestamp")
    private Instant updatedAt;

    @Schema(description = "Non-sensitive Session Metadata")
    private Map<String, Object> metadata;
}
