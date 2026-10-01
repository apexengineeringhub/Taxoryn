package com.taxoryn.module.itr.tax;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ResidentialStatus;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class Ay2026TaxRuleProvider implements TaxRuleProvider {
    private static final String AY = "2026-27";
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static BigDecimal n(String value) { return new BigDecimal(value); }
    private static TaxSlab slab(String lower, String upper, String rate, String fixed) {
        return new TaxSlab(n(lower), upper == null ? null : n(upper), n(rate), n(fixed));
    }

    @Override
    public TaxRuleSet resolve(String assessmentYear, TaxpayerType taxpayerType, AgeCategory age,
                              ResidentialStatus residentialStatus, TaxRegime regime) {
        if (!AY.equals(assessmentYear)) throw new BadRequestException("Unsupported assessment year: " + assessmentYear);
        if (taxpayerType != TaxpayerType.INDIVIDUAL) throw new BadRequestException("Tax calculation currently supports individuals only");
        if (regime == null || residentialStatus == null || age == null) throw new BadRequestException("Taxpayer details are required");

        List<TaxSlab> slabs = new ArrayList<>();
        if (regime == TaxRegime.NEW) {
            slabs.add(slab("0", "400000", "0", "0"));
            slabs.add(slab("400000", "800000", "0.05", "0"));
            slabs.add(slab("800000", "1200000", "0.10", "20000"));
            slabs.add(slab("1200000", "1600000", "0.15", "60000"));
            slabs.add(slab("1600000", "2000000", "0.20", "120000"));
            slabs.add(slab("2000000", "2400000", "0.25", "200000"));
            slabs.add(slab("2400000", null, "0.30", "300000"));
        } else {
            BigDecimal exemption = age == AgeCategory.SUPER_SENIOR_CITIZEN ? n("500000")
                    : age == AgeCategory.SENIOR_CITIZEN ? n("300000") : n("250000");
            slabs.add(new TaxSlab(ZERO, exemption, ZERO, ZERO));
            if (exemption.compareTo(n("500000")) < 0) {
                slabs.add(slab(exemption.toPlainString(), "500000", "0.05", "0"));
                slabs.add(slab("500000", "1000000", "0.20", age == AgeCategory.SENIOR_CITIZEN ? "10000" : "12500"));
                slabs.add(slab("1000000", null, "0.30", age == AgeCategory.SENIOR_CITIZEN ? "110000" : "112500"));
            } else {
                slabs.add(slab("500000", "1000000", "0.20", "0"));
                slabs.add(slab("1000000", null, "0.30", "100000"));
            }
        }
        RebateRule rebate = null;
        if (residentialStatus != ResidentialStatus.NON_RESIDENT) {
            if (regime == TaxRegime.NEW) rebate = new RebateRule(AY, regime, taxpayerType, n("1200000"), n("60000"), RebateRule.CalculationType.TAX_UP_TO_MAXIMUM_INCOME);
            else rebate = new RebateRule(AY, regime, taxpayerType, n("500000"), n("12500"), RebateRule.CalculationType.TAX_UP_TO_MAXIMUM_INCOME);
        }
        List<SurchargeRule> surcharge = List.of(
                new SurchargeRule(n("5000000"), n("10000000"), n("0.10"), regime, AY),
                new SurchargeRule(n("10000000"), n("20000000"), n("0.15"), regime, AY),
                new SurchargeRule(n("20000000"), n("50000000"), n("0.25"), regime, AY),
                new SurchargeRule(n("50000000"), null, n(regime == TaxRegime.NEW ? "0.25" : "0.37"), regime, AY));
        return new TaxRuleSet(AY, "INCOME_TAX_ACT_1961", taxpayerType, regime, age, slabs, rebate, surcharge, n("0.04"));
    }
}
