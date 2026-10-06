package com.taxoryn.module.itr.dto;

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
@Schema(description = "Summary and normalized payload of a prepared ITR return")
public class ItrPreparedReturnDto {

    @Schema(description = "Associated ITR Return ID (if linked)")
    private UUID returnId;

    @Schema(description = "Associated Client ID")
    private UUID clientId;

    @Schema(description = "Associated ITR Profile ID")
    private UUID profileId;

    @Schema(description = "10-character statutory PAN", example = "ABCDE1234F")
    private String pan;

    @Schema(description = "Masked PAN for secure client-facing display", example = "ABCDE****F")
    private String maskedPan;

    @Schema(description = "Legal registered name of taxpayer", example = "Apex Enterprise Solutions")
    private String taxpayerName;

    @Schema(description = "Statutory ITR form type code (ITR-1, ITR-2, ITR-3, ITR-4, ITR-5, ITR-6, ITR-7)", example = "ITR-1")
    private String returnType;

    @Schema(description = "Statutory Assessment Year", example = "2026-27")
    private String assessmentYear;

    @Schema(description = "Applicable Financial Year", example = "2025-26")
    private String financialYear;

    @Schema(description = "Current return status (PREPARED, VALIDATION_FAILED, READY_TO_FILE)", example = "PREPARED")
    private String status;

    @Schema(description = "Flag indicating return is verified, structurally compliant, and ready for submission", example = "true")
    private boolean readyForSubmission;

    @Schema(description = "Detailed structural and arithmetic validation results")
    private ItrReturnValidationResultDto validationResult;

    @Schema(description = "Normalized provider-neutral return payload with deterministic fingerprint")
    private ItrReturnPayloadDto payload;

    @Schema(description = "Deterministic SHA-256 payload fingerprint")
    private String payloadFingerprint;

    @Schema(description = "Provider simulation acknowledgement reference (if tracked)", example = "PREP-ITR-7B32F1")
    private String providerReferenceId;

    @Schema(description = "Timestamp when the return was prepared")
    private Instant preparedAt;

    @Schema(description = "Error code if preparation or validation failed")
    private String errorCode;

    @Schema(description = "Error message if preparation or validation failed")
    private String errorMessage;
}
