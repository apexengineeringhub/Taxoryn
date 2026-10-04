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
@Schema(description = "Result of EVC challenge verification")
public class GovEvcVerifyResultDto {

    @Schema(description = "Authentication session ID")
    private UUID sessionId;

    @Schema(description = "Government Connection ID")
    private UUID connectionId;

    @Schema(description = "EVC Purpose scope (ITR_EVC or TDS_EVC)")
    private GovAuthPurpose purpose;

    @Schema(description = "Session lifecycle status")
    private GovAuthStatus status;

    @Schema(description = "Authorization state")
    private GovAuthorizationState authorizationState;

    @Schema(description = "Whether verification succeeded")
    private boolean verified;

    @Schema(description = "Attempts remaining for this challenge")
    private int attemptsRemaining;

    @Schema(description = "Safe verification acknowledgment reference")
    private String verificationReference;

    @Schema(description = "Verification timestamp")
    private Instant verifiedAt;

    @Schema(description = "Failure code if verification failed")
    private String failureCode;

    @Schema(description = "Safe failure diagnostic message")
    private String safeFailureMessage;
}
