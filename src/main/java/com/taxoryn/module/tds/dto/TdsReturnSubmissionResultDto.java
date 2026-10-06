package com.taxoryn.module.tds.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Result of submitting a prepared TDS return statement to TRACES / Income Tax Gateway.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response payload containing the result of TDS return submission")
public class TdsReturnSubmissionResultDto {

    @Schema(description = "TDS Return ID", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
    private UUID returnId;

    @Schema(description = "10-character Tax Deduction Account Number (TAN)", example = "MUMB12345A")
    private String tan;

    @Schema(description = "Masked TAN for secure logging and display", example = "MUMB*****A")
    private String maskedTan;

    @Schema(description = "Statutory TDS return form type (e.g. FORM_24Q, FORM_26Q, FORM_27Q, FORM_27EQ)", example = "FORM_26Q")
    private String formType;

    @Schema(description = "Statutory quarter (Q1, Q2, Q3, Q4)", example = "Q1")
    private String quarter;

    @Schema(description = "Financial Year (YYYY-YY)", example = "2025-26")
    private String financialYear;

    @Schema(description = "Assessment Year (YYYY-YY)", example = "2026-27")
    private String assessmentYear;

    @Schema(description = "Submission lifecycle status (e.g. SUBMITTED, DUPLICATE_SUBMISSION, FAILED)", example = "SUBMITTED")
    private String submissionStatus;

    @Schema(description = "Whether the return submission request succeeded with an acknowledgment", example = "true")
    private boolean success;

    @Schema(description = "Official acknowledgment / Provisional Receipt Number (PRN / Token)", example = "TRACES-ACK-2026-000123")
    private String acknowledgementNumber;

    @Schema(description = "Provider transaction reference", example = "TRACES-SUB-20260401-ABCDEF")
    private String providerReference;

    @Schema(description = "Government integration operation ID", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb72")
    private UUID operationId;

    @Schema(description = "SHA-256 fingerprint hash of the submitted canonical payload")
    private String payloadFingerprint;

    @Schema(description = "Timestamp when the submission was recorded")
    private Instant submittedAt;

    @Schema(description = "Descriptive outcome message", example = "TDS statement submitted successfully to TRACES gateway")
    private String message;

    @Schema(description = "Standardized error code if submission failed", example = "PROVIDER_UNAVAILABLE")
    private String errorCode;

    @Schema(description = "Error description if submission failed")
    private String errorMessage;

    @Schema(description = "Additional metadata from the gateway response")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
