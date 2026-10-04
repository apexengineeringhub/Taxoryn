package com.taxoryn.module.tds.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request payload to validate, normalize, and prepare a statutory TDS return.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for preparing a statutory TDS quarterly statement")
public class TdsPrepareReturnRequest {

    @Schema(description = "Optional ID of existing draft TDS return entity to link and update", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
    private UUID returnId;

    @Schema(description = "Optional ID of client profile", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6e")
    private UUID clientId;

    @Schema(description = "Optional ID of TDS profile (TAN master)", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6f")
    private UUID profileId;

    @Schema(description = "Optional ID of specific TDS government connection", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb70")
    private UUID connectionId;

    @NotBlank(message = "TAN is mandatory for TDS return preparation")
    @Schema(description = "10-character Tax Deduction Account Number (TAN)", example = "MUMB12345A", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tan;

    @NotBlank(message = "Form type is mandatory (FORM_24Q, FORM_26Q, FORM_27Q, FORM_27EQ)")
    @Schema(description = "Statutory TDS return form type", example = "FORM_26Q", requiredMode = Schema.RequiredMode.REQUIRED)
    private String formType;

    @NotBlank(message = "Financial year is mandatory (e.g. 2025-26)")
    @Schema(description = "Financial Year (format YYYY-YY)", example = "2025-26", requiredMode = Schema.RequiredMode.REQUIRED)
    private String financialYear;

    @NotBlank(message = "Quarter is mandatory (Q1, Q2, Q3, Q4)")
    @Schema(description = "Statutory return quarter", example = "Q1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String quarter;

    @Schema(description = "Assessment Year (auto-derived if omitted)", example = "2026-27")
    private String assessmentYear;

    @Schema(description = "Deductor category / constitution (COMPANY, INDIVIDUAL_HUF, FIRM, etc.)", example = "COMPANY")
    private String deductorType;

    @Schema(description = "Aggregated return financial totals")
    private TdsReturnTotalsDto totals;

    @Schema(description = "Challan deposit records attached to this return")
    @Builder.Default
    private List<Map<String, Object>> challans = new ArrayList<>();

    @Schema(description = "Deductee transaction line items attached to this return")
    @Builder.Default
    private List<Map<String, Object>> deductees = new ArrayList<>();

    @Schema(description = "Optional provider directives and metadata")
    @Builder.Default
    private Map<String, Object> options = new HashMap<>();
}
