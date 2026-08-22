package com.taxoryn.module.tds.dto;

import com.taxoryn.module.tds.entity.TdsReturnEntity.FvuValidationStatus;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFilingStatus;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFormType;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsQuarter;
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
@Schema(description = "Request to create a new TDS Return Filing Record")
public class CreateTdsReturnRequest {

    @NotNull(message = "Client ID is required")
    @Schema(description = "Client ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID clientId;

    @NotNull(message = "TDS Profile ID is required")
    @Schema(description = "TDS Profile ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID tdsProfileId;

    @NotNull(message = "Form Type is required")
    @Schema(description = "TDS Return Form Type (FORM_24Q, FORM_26Q, FORM_27Q, FORM_27EQ, etc.)", requiredMode = Schema.RequiredMode.REQUIRED)
    private TdsFormType formType;

    @NotNull(message = "Quarter is required")
    @Schema(description = "Quarter (Q1, Q2, Q3, Q4)", requiredMode = Schema.RequiredMode.REQUIRED)
    private TdsQuarter quarter;

    @NotBlank(message = "Financial Year is required")
    @Schema(description = "Financial Year (e.g., 2026-27)", example = "2026-27", requiredMode = Schema.RequiredMode.REQUIRED)
    private String financialYear;

    @Schema(description = "Assessment Year (e.g., 2027-28)", example = "2027-28")
    private String assessmentYear;

    @Schema(description = "Statutory Due Date")
    private LocalDate dueDate;

    @Builder.Default
    @Schema(description = "Initial Filing Status", example = "PENDING")
    private TdsFilingStatus filingStatus = TdsFilingStatus.PENDING;

    @Schema(description = "Filing Date")
    private LocalDate filingDate;

    @Schema(description = "Provisional Receipt Number (Token / PRN)")
    private String tokenNumber;

    @Schema(description = "Receipt Reference")
    private String receiptNumber;

    @Builder.Default
    @Schema(description = "Total Amount Paid / Credited")
    private BigDecimal totalAmountPaid = BigDecimal.ZERO;

    @Builder.Default
    @Schema(description = "Total TDS Deducted")
    private BigDecimal totalTaxDeducted = BigDecimal.ZERO;

    @Builder.Default
    @Schema(description = "Total Tax Deposited via Challans")
    private BigDecimal totalTaxDeposited = BigDecimal.ZERO;

    @Builder.Default
    @Schema(description = "Interest under Section 201(1A)")
    private BigDecimal totalInterest = BigDecimal.ZERO;

    @Builder.Default
    @Schema(description = "Late Fee under Section 234E")
    private BigDecimal totalLateFee = BigDecimal.ZERO;

    @Builder.Default
    @Schema(description = "Penalty")
    private BigDecimal totalPenalty = BigDecimal.ZERO;

    @Schema(description = "Assigned Employee ID")
    private UUID assignedEmployeeId;

    @Builder.Default
    @Schema(description = "FVU Validation Status", example = "NOT_VALIDATED")
    private FvuValidationStatus fvuValidationStatus = FvuValidationStatus.NOT_VALIDATED;

    @Schema(description = "Notes")
    private String notes;
}
