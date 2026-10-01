package com.taxoryn.module.service.dto;

import com.taxoryn.module.service.model.ServicePricingMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdatePracticeServicePricingRequest(@NotNull ServicePricingMode pricingMode,
        @DecimalMin(value = "0.00", message = "Custom price cannot be negative")
        @Digits(integer = 13, fraction = 2, message = "Custom price must have at most two decimal places") BigDecimal customPrice,
        @NotNull Boolean enabled) {}
