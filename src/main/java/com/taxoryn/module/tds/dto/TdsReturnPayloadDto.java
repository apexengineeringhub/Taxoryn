package com.taxoryn.module.tds.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Canonical normalized payload representation for a statutory TDS return submission.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Provider-neutral canonical TDS return payload")
public class TdsReturnPayloadDto {

    @Schema(description = "10-character Tax Deduction and Collection Account Number (TAN)", example = "MUMB12345A")
    private String tan;

    @Schema(description = "Name of the deductor / employer / collector", example = "Acme Enterprises Private Limited")
    private String deductorName;

    @Schema(description = "Category of deductor (COMPANY, INDIVIDUAL_HUF, FIRM, etc.)", example = "COMPANY")
    private String deductorType;

    @Schema(description = "Permanent Account Number (PAN) of deductor", example = "AAACA1234C")
    private String pan;

    @Schema(description = "Statutory TDS return form type (FORM_24Q, FORM_26Q, FORM_27Q, FORM_27EQ)", example = "FORM_26Q")
    private String formType;

    @Schema(description = "Financial Year (YYYY-YY)", example = "2025-26")
    private String financialYear;

    @Schema(description = "Quarter (Q1, Q2, Q3, Q4)", example = "Q1")
    private String quarter;

    @Schema(description = "Assessment Year (YYYY-YY)", example = "2026-27")
    private String assessmentYear;

    @Schema(description = "Aggregated return financial totals and counts")
    private TdsReturnTotalsDto totals;

    @Schema(description = "List of normalized ITNS 281 challan deposit records")
    @Builder.Default
    private List<Map<String, Object>> challans = new ArrayList<>();

    @Schema(description = "List of normalized deductee payment / deduction records")
    @Builder.Default
    private List<Map<String, Object>> deductees = new ArrayList<>();

    @Schema(description = "Deterministic SHA-256 fingerprint hash of the canonical return payload")
    private String payloadFingerprint;

    @Schema(description = "Metadata directives and preparation audit headers")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
