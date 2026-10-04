package com.taxoryn.module.itr.dto;

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
@Schema(description = "ITR Return Submission Result")
public class ItrReturnSubmissionResultDto {

    @Schema(description = "ITR Return record ID (if associated)", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID returnId;

    @Schema(description = "Taxpayer PAN", example = "ABCDE1234F")
    private String pan;

    @Schema(description = "Masked PAN for audit/display", example = "ABCDE****F")
    private String maskedPan;

    @Schema(description = "Assessment Year", example = "2026-27")
    private String assessmentYear;

    @Schema(description = "Financial Year", example = "2025-26")
    private String financialYear;

    @Schema(description = "Return Type", example = "ITR1")
    private String returnType;

    @Schema(description = "Normalized Submission Status (e.g. SUBMITTED, FAILED, DUPLICATE_SUBMISSION)", example = "SUBMITTED")
    private String submissionStatus;

    @Schema(description = "Is submission considered successful at gateway stage")
    private boolean success;

    @Schema(description = "Provider Acknowledgement Reference Number (ARN / ITR-V Ack)", example = "ITD-ACK-7890123456")
    private String acknowledgementNumber;

    @Schema(description = "Provider Reference ID", example = "ITD-REF-12345678")
    private String providerReference;

    @Schema(description = "Gateway submission timestamp")
    private Instant submittedAt;

    @Schema(description = "Government Framework Operation ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID operationId;

    @Schema(description = "Payload SHA-256 fingerprint that was submitted")
    private String payloadFingerprint;

    @Schema(description = "Error Code if submission failed", example = "AUTH_REQUIRED")
    private String errorCode;

    @Schema(description = "Detailed error message if submission failed", example = "Income Tax e-filing session token expired or invalid")
    private String errorMessage;

    @Schema(description = "Safe provider response metadata")
    private Map<String, Object> metadata;
}
