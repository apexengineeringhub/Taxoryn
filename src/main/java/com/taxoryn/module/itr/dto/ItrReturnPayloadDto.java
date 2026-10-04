package com.taxoryn.module.itr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Deterministic, normalized, provider-neutral ITR return submission payload")
public class ItrReturnPayloadDto {

    @Schema(description = "10-character statutory PAN", example = "ABCDE1234F")
    private String pan;

    @Schema(description = "Legal registered name of taxpayer", example = "Apex Enterprise Solutions")
    private String taxpayerName;

    @Schema(description = "Taxpayer entity category (INDIVIDUAL, HUF, COMPANY, FIRM, TRUST)", example = "INDIVIDUAL")
    private String taxpayerType;

    @Schema(description = "Residential status (RESIDENT, NON_RESIDENT, RNOR)", example = "RESIDENT")
    private String residentialStatus;

    @Schema(description = "Assessment Year (e.g. 2026-27)", example = "2026-27")
    private String assessmentYear;

    @Schema(description = "Financial Year (e.g. 2025-26)", example = "2025-26")
    private String financialYear;

    @Schema(description = "Statutory ITR form type (ITR-1, ITR-2, ITR-3, ITR-4, ITR-5, ITR-6, ITR-7)", example = "ITR-1")
    private String returnType;

    @Schema(description = "Computed tax and liability totals")
    private ItrTaxSummaryDto taxSummary;

    @Schema(description = "Income details categorized by heads of income")
    @Builder.Default
    private Map<String, Object> incomeDetails = new HashMap<>();

    @Schema(description = "Chapter VI-A deduction details")
    @Builder.Default
    private Map<String, Object> deductions = new HashMap<>();

    @Schema(description = "Statutory return schedules")
    @Builder.Default
    private Map<String, Object> schedules = new HashMap<>();

    @Schema(description = "Bank account details for refund processing")
    @Builder.Default
    private Map<String, Object> bankDetails = new HashMap<>();

    @Schema(description = "SHA-256 fingerprint ensuring deterministic payload identity across submissions", example = "b7a80d4f...91f")
    private String payloadFingerprint;

    @Schema(description = "Extensible metadata headers (schemaVersion, preparedBy, etc.)")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
