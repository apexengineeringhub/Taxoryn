package com.taxoryn.module.tds.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Aggregated financial and count totals for a statutory TDS/TCS quarterly statement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated financial figures and record counts for TDS return")
public class TdsReturnTotalsDto {

    @Schema(description = "Total amount paid or credited to deductees", example = "500000.00")
    @Builder.Default
    private BigDecimal totalAmountPaid = BigDecimal.ZERO;

    @Schema(description = "Total tax deducted at source across all deductees", example = "50000.00")
    @Builder.Default
    private BigDecimal totalTaxDeducted = BigDecimal.ZERO;

    @Schema(description = "Total tax deposited via ITNS 281 challans", example = "50000.00")
    @Builder.Default
    private BigDecimal totalTaxDeposited = BigDecimal.ZERO;

    @Schema(description = "Total interest paid under Section 201(1A)", example = "0.00")
    @Builder.Default
    private BigDecimal totalInterest = BigDecimal.ZERO;

    @Schema(description = "Total late filing fee paid under Section 234E", example = "0.00")
    @Builder.Default
    private BigDecimal totalLateFee = BigDecimal.ZERO;

    @Schema(description = "Total penalty paid under Section 271H", example = "0.00")
    @Builder.Default
    private BigDecimal totalPenalty = BigDecimal.ZERO;

    @Schema(description = "Total deposited challan amount (Tax + Interest + Fee + Others)", example = "50000.00")
    @Builder.Default
    private BigDecimal totalChallanAmount = BigDecimal.ZERO;

    @Schema(description = "Total number of deductee transaction records included", example = "15")
    @Builder.Default
    private Long totalDeducteeCount = 0L;

    @Schema(description = "Total number of ITNS 281 challans attached", example = "2")
    @Builder.Default
    private Long totalChallanCount = 0L;
}
