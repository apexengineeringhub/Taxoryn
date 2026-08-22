package com.taxoryn.module.tds.dto;

import com.taxoryn.module.tds.entity.TdsReturnEntity.FvuValidationStatus;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFilingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request to update an existing TDS Return Filing Record")
public class UpdateTdsReturnRequest {

    @Schema(description = "Statutory Due Date")
    private LocalDate dueDate;

    @Schema(description = "Filing Status")
    private TdsFilingStatus filingStatus;

    @Schema(description = "Filing Date")
    private LocalDate filingDate;

    @Schema(description = "Provisional Receipt Number (Token / PRN)")
    private String tokenNumber;

    @Schema(description = "Portal Receipt Reference")
    private String receiptNumber;

    @Schema(description = "Total Amount Paid / Credited")
    private BigDecimal totalAmountPaid;

    @Schema(description = "Total TDS Deducted")
    private BigDecimal totalTaxDeducted;

    @Schema(description = "Total Tax Deposited via Challans")
    private BigDecimal totalTaxDeposited;

    @Schema(description = "Interest under Sec 201(1A)")
    private BigDecimal totalInterest;

    @Schema(description = "Late fee under Sec 234E")
    private BigDecimal totalLateFee;

    @Schema(description = "Penalty")
    private BigDecimal totalPenalty;

    @Schema(description = "Assigned Employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "FVU Validation Status")
    private FvuValidationStatus fvuValidationStatus;

    @Schema(description = "Practitioner Notes")
    private String notes;
}
