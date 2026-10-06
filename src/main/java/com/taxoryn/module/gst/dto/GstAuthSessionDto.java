package com.taxoryn.module.gst.dto;

import com.taxoryn.module.gov.auth.model.GovAuthMethod;
import com.taxoryn.module.gov.auth.model.GovAuthStatus;
import com.taxoryn.module.gst.model.GstAuthenticationPurpose;
import com.taxoryn.module.gst.model.GstTokenStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Safe GST / E-Way / E-Invoice authentication and token status response.
 * Note: Never contains raw secrets, bearer tokens, passwords, OTPs, or private keys.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Safe GST / E-Way / E-Invoice Authentication and Token Session Details")
public class GstAuthSessionDto {

    @Schema(description = "Underlying Government Authentication Session ID")
    private UUID sessionId;

    @Schema(description = "Government Connection ID")
    private UUID connectionId;

    @Schema(description = "Target GST Purpose (GST, EWAY_BILL, E_INVOICE)")
    private GstAuthenticationPurpose purpose;

    @Schema(description = "Authentication Method Used")
    private GovAuthMethod authMethod;

    @Schema(description = "Underlying Authentication Lifecycle Status")
    private GovAuthStatus status;

    @Schema(description = "GST Service Token / Session Reference Status")
    private GstTokenStatus tokenStatus;

    @Schema(description = "Safe non-secret token/session reference identifier")
    private String tokenReference;

    @Schema(description = "Whether interactive user action is required")
    private boolean requiresUserAction;

    @Schema(description = "Safe action prompt or login instructions")
    private String actionPrompt;

    @Schema(description = "Safe authorization redirect URL or challenge identifier")
    private String safeAuthorizationReference;

    @Schema(description = "Timestamp when token session was issued / authenticated")
    private Instant issuedAt;

    @Schema(description = "Session / token expiration timestamp")
    private Instant expiresAt;

    @Schema(description = "Last activity timestamp")
    private Instant lastActivityAt;

    @Schema(description = "Safe error or diagnostic failure code")
    private String failureCode;

    @Schema(description = "Safe failure diagnostic message")
    private String safeFailureMessage;

    @Schema(description = "Correlation ID for distributed tracing")
    private String correlationId;

    @Schema(description = "Safe non-sensitive session metadata")
    private Map<String, Object> metadata;
}
