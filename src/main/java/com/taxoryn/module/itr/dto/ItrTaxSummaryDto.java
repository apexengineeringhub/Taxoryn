package com.taxoryn.module.itr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Structured tax liability, deduction, and tax payments summary for ITR return")
public class ItrTaxSummaryDto {

    @Schema(description = "Gross total income before Chapter VI-A deductions", example = "1250000.00")
    @Builder.Default
    private BigDecimal grossTotalIncome = BigDecimal.ZERO;

    @Schema(description = "Total Chapter VI-A deductions (80C, 80D, 80G, etc.)", example = "150000.00")
    @Builder.Default
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Schema(description = "Net taxable total income after deductions", example = "1100000.00")
    @Builder.Default
    private BigDecimal taxableIncome = BigDecimal.ZERO;

    @Schema(description = "Computed statutory tax payable before surcharge and cess", example = "142500.00")
    @Builder.Default
    private BigDecimal taxPayable = BigDecimal.ZERO;

    @Schema(description = "Statutory surcharge amount", example = "0.00")
    @Builder.Default
    private BigDecimal surcharge = BigDecimal.ZERO;

    @Schema(description = "Health & Education Cess (4%)", example = "5700.00")
    @Builder.Default
    private BigDecimal cess = BigDecimal.ZERO;

    @Schema(description = "Total aggregate tax and interest liability", example = "148200.00")
    @Builder.Default
    private BigDecimal totalTaxLiability = BigDecimal.ZERO;

    @Schema(description = "Total TDS and TCS credit claimed", example = "120000.00")
    @Builder.Default
    private BigDecimal tdsTcsCredit = BigDecimal.ZERO;

    @Schema(description = "Total Advance Tax paid", example = "20000.00")
    @Builder.Default
    private BigDecimal advanceTaxPaid = BigDecimal.ZERO;

    @Schema(description = "Self Assessment Tax paid (challan 280)", example = "8200.00")
    @Builder.Default
    private BigDecimal selfAssessmentTaxPaid = BigDecimal.ZERO;

    @Schema(description = "Total taxes paid (TDS/TCS + Advance Tax + Self Assessment Tax)", example = "148200.00")
    @Builder.Default
    private BigDecimal totalTaxesPaid = BigDecimal.ZERO;

    @Schema(description = "Refund claimed (if taxes paid exceed total liability)", example = "0.00")
    @Builder.Default
    private BigDecimal refundDue = BigDecimal.ZERO;

    @Schema(description = "Net balance tax payable (if liability exceeds taxes paid)", example = "0.00")
    @Builder.Default
    private BigDecimal balancePayable = BigDecimal.ZERO;
}
