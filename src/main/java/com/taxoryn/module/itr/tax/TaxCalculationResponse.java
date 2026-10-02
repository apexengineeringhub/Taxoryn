package com.taxoryn.module.itr.tax;

import java.math.BigDecimal;
import java.util.List;

public record TaxCalculationResponse(String assessmentYear, String lawVersion, TaxRegime regime,
        BigDecimal totalIncome, BigDecimal deductions, BigDecimal taxableIncome,
        List<SlabLine> slabBreakdown, BigDecimal slabTax, BigDecimal rebate,
        BigDecimal taxAfterRebate, BigDecimal surcharge, BigDecimal taxWithSurcharge,
        BigDecimal cess, BigDecimal totalTax) {
    public TaxCalculationResponse { slabBreakdown = List.copyOf(slabBreakdown); }
    public record SlabLine(BigDecimal from, BigDecimal to, BigDecimal rate, BigDecimal tax) {}
}
