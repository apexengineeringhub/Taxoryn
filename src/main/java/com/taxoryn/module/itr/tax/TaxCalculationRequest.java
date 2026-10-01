package com.taxoryn.module.itr.tax;

import com.taxoryn.module.itr.entity.ItrProfileEntity.ResidentialStatus;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record TaxCalculationRequest(
        @NotBlank String assessmentYear,
        @NotNull TaxpayerType taxpayerType,
        @NotNull @Min(0) Integer age,
        @NotNull ResidentialStatus residentialStatus,
        TaxRegime regime,
        @NotNull @DecimalMin("0.00") BigDecimal totalIncome,
        @NotNull @DecimalMin("0.00") BigDecimal deductions) {}
