package com.taxoryn.module.itr.tax;

import java.math.BigDecimal;

public record RebateRule(String assessmentYear, TaxRegime regime,
                         com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType taxpayerType,
                         BigDecimal maximumEligibleIncome, BigDecimal maximumRebate,
                         CalculationType calculationType) {
    public enum CalculationType { TAX_UP_TO_MAXIMUM_INCOME }
}
