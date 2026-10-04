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
@Schema(description = "Government EVC challenge session response (safe reference only, no raw OTP/EVC)")
public class GovEvcChallengeDto {

    @Schema(description = "Authentication session ID")
    private UUID sessionId;

    @Schema(description = "Government Connection ID")
    private UUID connectionId;

    @Schema(description = "EVC Purpose scope (ITR_EVC or TDS_EVC)")
    private GovAuthPurpose purpose;

    @Schema(description = "Session lifecycle status")
    private GovAuthStatus status;

    @Schema(description = "Detailed authorization workflow state")
    private GovAuthorizationState authorizationState;

    @Schema(description = "Safe challenge reference ID")
    private String challengeReference;

    @Schema(description = "Action prompt for the user")
    private String actionPrompt;

    @Schema(description = "Verification attempts remaining before challenge invalidation")
    @Builder.Default
    private int attemptsRemaining = 3;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Challenge expiration timestamp")
    private Instant expiresAt;

    @Schema(description = "Correlation ID")
    private String correlationId;

    @Schema(description = "Safe diagnostic message")
    private String safeMessage;

    @Schema(description = "Failure diagnostic code if failed")
    private String failureCode;
}
