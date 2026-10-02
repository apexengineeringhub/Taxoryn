package com.taxoryn.module.itr.tax;

import com.taxoryn.module.itr.entity.ItrProfileEntity.ResidentialStatus;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;

public interface TaxRuleProvider {
    TaxRuleSet resolve(String assessmentYear, TaxpayerType taxpayerType, AgeCategory ageCategory,
                       ResidentialStatus residentialStatus, TaxRegime regime);
}
