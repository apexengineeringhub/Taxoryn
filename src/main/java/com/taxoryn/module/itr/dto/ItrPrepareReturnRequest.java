package com.taxoryn.module.itr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to prepare and normalize an Income Tax Return (ITR)")
public class ItrPrepareReturnRequest {

    @NotBlank(message = "PAN is required")
    @Pattern(
            regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$",
            message = "Invalid PAN format. Expected standard 10-character alphanumeric format (e.g. ABCDE1234F)"
    )
    @Schema(description = "10-character statutory Permanent Account Number", example = "ABCDE1234F")
    private String pan;

    @NotBlank(message = "Assessment Year is required")
    @Schema(description = "Statutory Assessment Year (e.g. 2026-27)", example = "2026-27")
    private String assessmentYear;

    @Schema(description = "Applicable Financial Year (e.g. 2025-26)", example = "2025-26")
    private String financialYear;

    @NotBlank(message = "Return type is required")
    @Schema(description = "Statutory ITR form type (ITR-1, ITR-2, ITR-3, ITR-4, ITR-5, ITR-6, ITR-7)", example = "ITR-1")
    private String returnType;

    @Schema(description = "Optional existing ITR return record ID to bind and update")
    private UUID returnId;

    @Schema(description = "Optional ITR profile ID")
    private UUID profileId;

    @Schema(description = "Optional Client ID")
    private UUID clientId;

    @Schema(description = "Taxpayer constitution category (INDIVIDUAL, HUF, FIRM, COMPANY, TRUST)", example = "INDIVIDUAL")
    private String taxpayerType;

    @Schema(description = "Residential status for tax purposes (RESIDENT, NON_RESIDENT, RNOR)", example = "RESIDENT")
    private String residentialStatus;

    @Schema(description = "Structured tax and liability totals")
    private ItrTaxSummaryDto taxSummary;

    @Schema(description = "Income stream breakdowns (salary, houseProperty, businessProfession, capitalGains, otherSources)")
    private Map<String, Object> incomeDetails;

    @Schema(description = "Chapter VI-A deduction breakdowns (section80C, section80D, section80G, etc.)")
    private Map<String, Object> deductions;

    @Schema(description = "Form schedules (e.g. scheduleBFLA, scheduleCFL, schedule80G, scheduleCYLA)")
    private Map<String, Object> schedules;

    @Schema(description = "Bank accounts for statutory verification and refund credit (bankName, accountNumber, ifscCode, isPrimary)")
    private Map<String, Object> bankDetails;

    @Schema(description = "Optional simulation directives or preparation options")
    private Map<String, Object> options;
}
