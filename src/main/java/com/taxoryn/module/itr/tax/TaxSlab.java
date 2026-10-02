package com.taxoryn.module.itr.tax;

import java.math.BigDecimal;

public record TaxSlab(BigDecimal lowerLimit, BigDecimal upperLimit, BigDecimal rate,
                      BigDecimal fixedTaxBeforeSlab) {
    public TaxSlab {
        if (lowerLimit == null || rate == null || fixedTaxBeforeSlab == null || lowerLimit.signum() < 0
                || rate.signum() < 0 || fixedTaxBeforeSlab.signum() < 0
                || (upperLimit != null && upperLimit.compareTo(lowerLimit) <= 0)) {
            throw new IllegalArgumentException("Invalid tax slab");
        }
    }
}
