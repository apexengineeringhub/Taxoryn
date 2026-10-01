package com.taxoryn.module.itr.tax;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ResidentialStatus;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxCalculationService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int MONEY_SCALE = 2;
    private final TaxRuleProvider ruleProvider;

    public TaxCalculationResponse calculateTax(TaxCalculationRequest request) {
        validate(request, true);
        return calculate(request.assessmentYear(), request.taxpayerType(), AgeCategory.of(request.age()),
                request.residentialStatus(), request.regime(), request.totalIncome(), request.deductions());
    }

    public TaxRegimeComparisonResponse compare(TaxCalculationRequest request) {
        validate(request, false);
        TaxCalculationResponse oldResult = calculate(request.assessmentYear(), request.taxpayerType(),
                AgeCategory.of(request.age()), request.residentialStatus(), TaxRegime.OLD, request.totalIncome(), request.deductions());
        TaxCalculationResponse newResult = calculate(request.assessmentYear(), request.taxpayerType(),
                AgeCategory.of(request.age()), request.residentialStatus(), TaxRegime.NEW, request.totalIncome(), request.deductions());
        return new TaxRegimeComparisonResponse(oldResult, newResult, money(oldResult.totalTax().subtract(newResult.totalTax())));
    }

    private TaxCalculationResponse calculate(String year, TaxpayerType type, AgeCategory age,
            ResidentialStatus status, TaxRegime regime, BigDecimal income, BigDecimal deductions) {
        TaxRuleSet rules = ruleProvider.resolve(year, type, age, status, regime);
        BigDecimal taxable = money(income.subtract(deductions));
        List<TaxCalculationResponse.SlabLine> breakdown = new ArrayList<>();
        BigDecimal slabTax = ZERO;
        for (TaxSlab slab : rules.slabs()) {
            if (taxable.compareTo(slab.lowerLimit()) <= 0) break;
            BigDecimal upper = slab.upperLimit() == null || taxable.compareTo(slab.upperLimit()) < 0 ? taxable : slab.upperLimit();
            BigDecimal base = upper.subtract(slab.lowerLimit());
            BigDecimal tax = money(base.multiply(slab.rate()));
            breakdown.add(new TaxCalculationResponse.SlabLine(slab.lowerLimit(), upper, slab.rate().multiply(new BigDecimal("100")), tax));
            slabTax = money(slab.fixedTaxBeforeSlab().add(tax));
        }
        BigDecimal rebate = ZERO;
        RebateRule rebateRule = rules.rebateRule();
        if (rebateRule != null && taxable.compareTo(rebateRule.maximumEligibleIncome()) <= 0) {
            rebate = slabTax.min(rebateRule.maximumRebate());
        }
        BigDecimal afterRebate = money(slabTax.subtract(rebate).max(ZERO));
        BigDecimal surchargeRate = rules.surchargeRules().stream()
                .filter(rule -> taxable.compareTo(rule.incomeFrom()) > 0 && (rule.incomeTo() == null || taxable.compareTo(rule.incomeTo()) <= 0))
                .map(SurchargeRule::rate).findFirst().orElse(ZERO);
        BigDecimal surcharge = money(afterRebate.multiply(surchargeRate));
        BigDecimal withSurcharge = money(afterRebate.add(surcharge));
        BigDecimal cess = money(withSurcharge.multiply(rules.cessRate()));
        BigDecimal total = money(withSurcharge.add(cess));
        return new TaxCalculationResponse(year, rules.lawVersion(), regime, money(income), money(deductions), taxable,
                breakdown, slabTax, money(rebate), afterRebate, surcharge, withSurcharge, cess, total);
    }

    private static void validate(TaxCalculationRequest request, boolean requireRegime) {
        if (request == null) throw new BadRequestException("Calculation request is required");
        if (request.totalIncome() == null || request.deductions() == null || request.totalIncome().signum() < 0 || request.deductions().signum() < 0)
            throw new BadRequestException("Income and deductions must be zero or greater");
        if (request.deductions().compareTo(request.totalIncome()) > 0)
            throw new BadRequestException("Deductions cannot exceed total income");
        if (request.age() == null || request.age() < 0) throw new BadRequestException("Age must be zero or greater");
        if (request.taxpayerType() == null || request.residentialStatus() == null || request.assessmentYear() == null || request.assessmentYear().isBlank())
            throw new BadRequestException("Assessment year and taxpayer details are required");
        if (requireRegime && request.regime() == null) throw new BadRequestException("Tax regime is required");
    }

    private static BigDecimal money(BigDecimal amount) { return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP); }
}
