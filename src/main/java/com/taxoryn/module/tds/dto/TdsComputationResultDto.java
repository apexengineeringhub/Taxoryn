package com.taxoryn.module.tds.dto;

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
@Schema(description = "Calculated TDS, Surcharge, Cess, Interest, and Late Fee result")
public class TdsComputationResultDto {

    @Schema(description = "Section Code", example = "194J")
    private String sectionCode;

    @Schema(description = "Section Title")
    private String sectionTitle;

    @Schema(description = "Gross Amount Paid / Credited")
    private BigDecimal grossAmount;

    @Schema(description = "Threshold Exemption Applicable flag")
    private boolean thresholdExemptionApplicable;

    @Schema(description = "Effective TDS Rate (%)", example = "10.00")
    private BigDecimal effectiveRate;

    @Schema(description = "Base TDS Amount")
    private BigDecimal baseTdsAmount;

    @Schema(description = "Surcharge Rate (%)")
    private BigDecimal surchargeRate;

    @Schema(description = "Surcharge Amount")
    private BigDecimal surchargeAmount;

    @Schema(description = "Health & Education Cess (4%)")
    private BigDecimal cessAmount;

    @Schema(description = "Total Tax Deductible / Deducted")
    private BigDecimal totalTaxDeducted;

    @Schema(description = "Net Amount Payable to Deductee")
    private BigDecimal netPayableToDeductee;

    @Schema(description = "Interest for Delay in Deduction (Sec 201(1A)(i) @ 1% per month)")
    private BigDecimal delayInDeductionInterest;

    @Schema(description = "Interest for Delay in Deposit (Sec 201(1A)(ii) @ 1.5% per month)")
    private BigDecimal delayInDepositInterest;

    @Schema(description = "Total Interest under Sec 201(1A)")
    private BigDecimal totalInterest;

    @Schema(description = "Days of Delay in Filing Return")
    private int delayDays;

    @Schema(description = "Late Filing Fee under Sec 234E (₹200/day capped at TDS)")
    private BigDecimal lateFee234E;

    @Schema(description = "Total Statutory Liability Payable")
    private BigDecimal totalPayableWithPenalties;

    @Schema(description = "Computation Explanation / Remarks")
    private String remarks;
}
