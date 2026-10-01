package com.taxoryn.module.itr.tax;

import java.math.BigDecimal;

public record TaxRegimeComparisonResponse(TaxCalculationResponse oldRegime,
        TaxCalculationResponse newRegime, BigDecimal taxDifference) {}
