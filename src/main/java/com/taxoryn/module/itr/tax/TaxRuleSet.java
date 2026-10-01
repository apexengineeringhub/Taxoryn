package com.taxoryn.module.itr.tax;

import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
import java.math.BigDecimal;
import java.util.List;

public record TaxRuleSet(String assessmentYear, String lawVersion, TaxpayerType taxpayerType,
                         TaxRegime regime, AgeCategory ageCategory, List<TaxSlab> slabs,
                         RebateRule rebateRule, List<SurchargeRule> surchargeRules,
                         BigDecimal cessRate) {
    public TaxRuleSet { slabs = List.copyOf(slabs); surchargeRules = List.copyOf(surchargeRules); }
}
