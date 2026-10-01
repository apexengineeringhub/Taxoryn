package com.taxoryn.module.itr.tax;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ResidentialStatus;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class TaxCalculationBoundaryTest {
    private TaxCalculationService service;

    @BeforeEach void setUp() { service = new TaxCalculationService(new Ay2026TaxRuleProvider()); }

    private TaxCalculationRequest request(String income, String deductions, int age, TaxRegime regime) {
        return new TaxCalculationRequest("2026-27", TaxpayerType.INDIVIDUAL, age,
                ResidentialStatus.RESIDENT, regime, new BigDecimal(income), new BigDecimal(deductions));
    }
    private void tax(String income, String deductions, int age, TaxRegime regime, String expected) {
        assertEquals(0, service.calculateTax(request(income, deductions, age, regime)).totalTax().compareTo(new BigDecimal(expected)));
    }

    @Test void newRegimeSlabAndRebateBoundaries() {
        tax("0", "0", 35, TaxRegime.NEW, "0.00");
        tax("250000", "0", 35, TaxRegime.NEW, "0.00");
        tax("400000", "0", 35, TaxRegime.NEW, "0.00");
        tax("800000", "0", 35, TaxRegime.NEW, "0.00");
        tax("1000000", "0", 35, TaxRegime.NEW, "0.00");
        TaxCalculationResponse atLimit = service.calculateTax(request("1200000", "0", 35, TaxRegime.NEW));
        assertEquals(0, atLimit.slabTax().compareTo(new BigDecimal("60000.00")));
        assertEquals(0, atLimit.rebate().compareTo(new BigDecimal("60000.00")));
        assertEquals(0, atLimit.totalTax().compareTo(new BigDecimal("0.00")));
        TaxCalculationResponse aboveLimit = service.calculateTax(request("1200001", "0", 35, TaxRegime.NEW));
        assertTrue(aboveLimit.totalTax().signum() > 0);
        TaxCalculationResponse firstMillion = service.calculateTax(request("1000000", "0", 35, TaxRegime.NEW));
        assertEquals(0, firstMillion.slabTax().compareTo(new BigDecimal("40000.00")));
        tax("1500000", "0", 35, TaxRegime.NEW, "109200.00");
        tax("2000000", "0", 35, TaxRegime.NEW, "208000.00");
        tax("2400000", "0", 35, TaxRegime.NEW, "312000.00");
        TaxCalculationResponse aboveTopSlab = service.calculateTax(request("2400001", "0", 35, TaxRegime.NEW));
        assertTrue(aboveTopSlab.slabTax().compareTo(new BigDecimal("300000.00")) > 0);
    }

    @Test void oldRegimeAgeCategoriesAndDeductions() {
        tax("250000", "0", 35, TaxRegime.OLD, "0.00");
        tax("500000", "0", 35, TaxRegime.OLD, "0.00");
        tax("800000", "0", 35, TaxRegime.OLD, "75400.00");
        tax("500000", "0", 65, TaxRegime.OLD, "0.00");
        tax("500000", "0", 80, TaxRegime.OLD, "0.00");
        tax("1500000", "150000", 35, TaxRegime.OLD, "226200.00");
    }

    @Test void surchargeAndCessAreSeparate() {
        TaxCalculationResponse atThreshold = service.calculateTax(request("5000000", "0", 35, TaxRegime.NEW));
        assertEquals(0, atThreshold.surcharge().compareTo(BigDecimal.ZERO));
        TaxCalculationResponse result = service.calculateTax(request("5000001", "0", 35, TaxRegime.NEW));
        assertTrue(result.surcharge().signum() > 0);
        assertEquals(0, result.cess().compareTo(result.taxWithSurcharge().multiply(new BigDecimal("0.04")).setScale(2, java.math.RoundingMode.HALF_UP)));
    }

    @Test void comparisonReturnsBothRegimesAndSignedDifference() {
        TaxRegimeComparisonResponse result = service.compare(request("1500000", "150000", 35, TaxRegime.NEW));
        assertNotNull(result.oldRegime());
        assertNotNull(result.newRegime());
        assertEquals(0, result.taxDifference().compareTo(result.oldRegime().totalTax().subtract(result.newRegime().totalTax())));
    }

    @Test void rejectsNegativeValuesExcessDeductionsAndUnsupportedYear() {
        assertThrows(BadRequestException.class, () -> service.calculateTax(request("-1", "0", 35, TaxRegime.NEW)));
        assertThrows(BadRequestException.class, () -> service.calculateTax(request("100", "-1", 35, TaxRegime.NEW)));
        assertThrows(BadRequestException.class, () -> service.calculateTax(request("100", "101", 35, TaxRegime.NEW)));
        TaxCalculationRequest unsupported = new TaxCalculationRequest("2025-26", TaxpayerType.INDIVIDUAL, 35,
                ResidentialStatus.RESIDENT, TaxRegime.NEW, BigDecimal.ONE, BigDecimal.ZERO);
        assertThrows(BadRequestException.class, () -> service.calculateTax(unsupported));
    }
}
