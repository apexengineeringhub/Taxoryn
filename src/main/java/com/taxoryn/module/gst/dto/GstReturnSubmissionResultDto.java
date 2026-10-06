package com.taxoryn.module.gst.dto;

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
@Schema(description = "GST Return Submission Result")
public class GstReturnSubmissionResultDto {

    @Schema(description = "Filing record ID (if associated)", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID filingId;

    @Schema(description = "GSTIN of the taxpayer", example = "27AAACA1234A1ZV")
    private String gstin;

    @Schema(description = "Return Type", example = "GSTR1")
    private String returnType;

    @Schema(description = "Return Period", example = "042026")
    private String returnPeriod;

    @Schema(description = "Financial Year", example = "2026-27")
    private String financialYear;

    @Schema(description = "Normalized Submission Status (e.g. SUCCESS, FILED, SUBMITTED, FAILED, DUPLICATE, IN_PROGRESS)", example = "SUCCESS")
    private String submissionStatus;

    @Schema(description = "Is submission considered successful")
    private boolean success;

    @Schema(description = "Provider Acknowledgement Reference Number (ARN)", example = "MOCK-GST-ACK-E3B0C442")
    private String acknowledgementNumber;

    @Schema(description = "Provider Reference ID", example = "REF-GST-SUB-98765432")
    private String providerReference;

    @Schema(description = "Submission timestamp")
    private Instant submittedAt;

    @Schema(description = "Government Framework Operation ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID operationId;

    @Schema(description = "Payload SHA-256 fingerprint that was submitted")
    private String payloadFingerprint;

    @Schema(description = "Error Code if submission failed", example = "AUTH_REQUIRED")
    private String errorCode;

    @Schema(description = "Detailed error message if submission failed", example = "Session expired")
    private String errorMessage;

    @Schema(description = "Safe provider metadata")
    private Map<String, Object> metadata;
}
