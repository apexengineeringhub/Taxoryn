package com.taxoryn.module.tds.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Request to submit a prepared statutory TDS return statement to TRACES / Income Tax Gateway.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for submitting a prepared TDS return statement")
public class TdsSubmitReturnRequest {

    @Schema(description = "ID of the prepared TDS return entity", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
    private UUID returnId;

    @Schema(description = "Optional ID of specific TDS government connection to use", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb70")
    private UUID connectionId;

    @Schema(description = "10-character Tax Deduction Account Number (TAN)", example = "MUMB12345A")
    private String tan;

    @Schema(description = "Statutory TDS return form type (e.g. FORM_24Q, FORM_26Q, FORM_27Q, FORM_27EQ)", example = "FORM_26Q")
    private String formType;

    @Schema(description = "Statutory quarter (Q1, Q2, Q3, Q4)", example = "Q1")
    private String quarter;

    @Schema(description = "Financial Year (YYYY-YY)", example = "2025-26")
    private String financialYear;

    @Schema(description = "Assessment Year (YYYY-YY)", example = "2026-27")
    private String assessmentYear;

    @Schema(description = "SHA-256 fingerprint of the prepared canonical payload", example = "a1b2c3d4e5f6...")
    private String payloadFingerprint;

    @Schema(description = "Expected SHA-256 fingerprint for integrity validation", example = "a1b2c3d4e5f6...")
    private String expectedFingerprint;

    @Schema(description = "Optional canonical payload representation")
    private TdsReturnPayloadDto payload;

    @Schema(description = "Optional execution directives and provider options (e.g. mockOutcome)")
    @Builder.Default
    private Map<String, Object> options = new HashMap<>();
}
