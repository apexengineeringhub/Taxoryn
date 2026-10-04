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
 * Result of statutory TDS return preparation, normalization, and fingerprinting.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing prepared TDS return statement, validation results, and canonical fingerprint")
public class TdsPreparedReturnDto {

    @Schema(description = "TDS Return ID", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
    private UUID returnId;

    @Schema(description = "Associated Client ID", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6e")
    private UUID clientId;

    @Schema(description = "Associated TDS Profile ID", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6f")
    private UUID profileId;

    @Schema(description = "10-character Tax Deduction Account Number (TAN)", example = "MUMB12345A")
    private String tan;

    @Schema(description = "Masked TAN for secure logging and display", example = "MUMB*****A")
    private String maskedTan;

    @Schema(description = "Deductor legal name", example = "Acme Enterprises Private Limited")
    private String deductorName;

    @Schema(description = "Statutory TDS return form type", example = "FORM_26Q")
    private String formType;

    @Schema(description = "Financial Year (YYYY-YY)", example = "2025-26")
    private String financialYear;

    @Schema(description = "Quarter (Q1, Q2, Q3, Q4)", example = "Q1")
    private String quarter;

    @Schema(description = "Assessment Year (YYYY-YY)", example = "2026-27")
    private String assessmentYear;

    @Schema(description = "Return preparation status (e.g. READY_TO_FILE, VALIDATION_FAILED, ERROR)", example = "READY_TO_FILE")
    private String status;

    @Schema(description = "Whether the return is valid and ready for submission to TRACES / Income Tax portal", example = "true")
    private boolean readyForSubmission;

    @Schema(description = "Validation outcome with statutory error/warning details")
    private TdsReturnValidationResultDto validationResult;

    @Schema(description = "Canonical normalized TDS payload")
    private TdsReturnPayloadDto payload;

    @Schema(description = "Deterministic SHA-256 fingerprint hash of the return payload")
    private String payloadFingerprint;

    @Schema(description = "Provider reference acknowledgment identifier")
    private String providerReferenceId;

    @Schema(description = "Audit operation tracking ID")
    private UUID operationId;

    @Schema(description = "Preparation completion timestamp")
    private Instant preparedAt;

    @Schema(description = "Error code if preparation failed")
    private String errorCode;

    @Schema(description = "Error message if preparation failed")
    private String errorMessage;

    @Schema(description = "Additional return metadata")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
