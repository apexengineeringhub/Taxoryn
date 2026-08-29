package com.taxoryn.module.gst.dto;

import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstReturnType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create / Schedule GST Return Filing Request Payload")
public class CreateGstReturnFilingRequest {

    @Schema(description = "GST Profile ID (optional if GSTIN is provided)", example = "d1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID gstProfileId;

    @Schema(description = "15-digit GSTIN (used to auto-resolve GST profile)", example = "27AAACZ1234D1Z8")
    private String gstin;

    @NotNull(message = "Return type is required")
    @Schema(description = "Return type (GSTR1, GSTR3B, GSTR9, CMP08, etc.)", example = "GSTR3B")
    private GstReturnType returnType;

    @NotBlank(message = "Return period is required")
    @Schema(description = "Return period (e.g. 2026-08)", example = "2026-08")
    private String returnPeriod;

    @NotBlank(message = "Financial year is required")
    @Schema(description = "Financial year (e.g. 2026-27)", example = "2026-27")
    private String financialYear;

    @Schema(description = "Statutory due date", example = "2026-09-20")
    private LocalDate dueDate;

    @Schema(description = "Initial filing status", defaultValue = "PENDING")
    @Builder.Default
    private GstFilingStatus filingStatus = GstFilingStatus.PENDING;

    @Schema(description = "Total taxable value")
    private BigDecimal totalTaxableValue;

    @Schema(description = "Total tax liability")
    private BigDecimal totalTaxLiability;

    @Schema(description = "Total ITC claimed")
    private BigDecimal totalItcClaimed;

    @Schema(description = "Tax paid in cash")
    private BigDecimal taxPaidCash;

    @Schema(description = "Tax paid via ITC")
    private BigDecimal taxPaidItc;

    @Schema(description = "Assigned practitioner employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "GST Portal Acknowledgement reference number (ARN)", example = "AA2707261234567")
    private String acknowledgementNumber;

    @Schema(description = "Optional linked compliance obligation ID")
    private UUID complianceId;

    @Schema(description = "Optional flag to auto-generate linked task in Task module", example = "true")
    private Boolean createTask;

    @Schema(description = "Optional linked document request ID")
    private UUID documentRequestId;

    @Schema(description = "Practitioner notes or remarks")
    private String notes;
}
