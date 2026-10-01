package com.taxoryn.module.itr.tax;

import java.math.BigDecimal;

public record SurchargeRule(BigDecimal incomeFrom, BigDecimal incomeTo, BigDecimal rate,
                            TaxRegime regime, String assessmentYear) {}
