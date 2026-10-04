package com.taxoryn.module.gov.auth.dto;

import com.taxoryn.module.gov.auth.model.GovAuthPurpose;
import com.taxoryn.module.gov.auth.model.GovAuthStatus;
import com.taxoryn.module.gov.auth.model.GovAuthorizationState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Government DSC signing session response (safe reference only, no private keys)")
public class GovDscSigningSessionDto {

    @Schema(description = "Authentication session ID")
    private UUID sessionId;

    @Schema(description = "Government Connection ID")
    private UUID connectionId;

    @Schema(description = "DSC Purpose scope (ITR_DSC or TDS_DSC)")
    private GovAuthPurpose purpose;

    @Schema(description = "Session lifecycle status")
    private GovAuthStatus status;

    @Schema(description = "Detailed authorization workflow state")
    private GovAuthorizationState authorizationState;

    @Schema(description = "Safe challenge / signing request reference ID")
    private String signingChallengeReference;

    @Schema(description = "Action prompt for digital certificate signing")
    private String actionPrompt;

    @Schema(description = "Digest of payload being signed")
    private String documentDigest;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Session expiration timestamp")
    private Instant expiresAt;

    @Schema(description = "Correlation ID")
    private String correlationId;

    @Schema(description = "Safe diagnostic message")
    private String safeMessage;

    @Schema(description = "Failure diagnostic code if failed")
    private String failureCode;
}
