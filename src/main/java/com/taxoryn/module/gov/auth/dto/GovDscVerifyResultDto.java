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
@Schema(description = "Result of DSC digital signature verification")
public class GovDscVerifyResultDto {

    @Schema(description = "Authentication session ID")
    private UUID sessionId;

    @Schema(description = "Government Connection ID")
    private UUID connectionId;

    @Schema(description = "DSC Purpose scope (ITR_DSC or TDS_DSC)")
    private GovAuthPurpose purpose;

    @Schema(description = "Session lifecycle status")
    private GovAuthStatus status;

    @Schema(description = "Authorization state")
    private GovAuthorizationState authorizationState;

    @Schema(description = "Whether signature verification succeeded")
    private boolean verified;

    @Schema(description = "Safe signature acknowledgment reference")
    private String signatureReference;

    @Schema(description = "Safe certificate reference")
    private String certificateReference;

    @Schema(description = "Certificate subject DN (safe metadata only)")
    private String certificateSubject;

    @Schema(description = "Certificate serial number reference")
    private String certificateSerial;

    @Schema(description = "Signing algorithm (e.g. SHA256withRSA)")
    private String signingAlgorithm;

    @Schema(description = "Verification timestamp")
    private Instant verifiedAt;

    @Schema(description = "Failure code if verification failed")
    private String failureCode;

    @Schema(description = "Safe failure diagnostic message")
    private String safeFailureMessage;
}
