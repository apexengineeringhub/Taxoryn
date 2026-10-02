package com.taxoryn.module.service.dto;

import com.taxoryn.module.service.model.ServicePricingMode;
import java.math.BigDecimal;
import java.util.UUID;

public record PracticeServicePriceDto(UUID serviceId, String serviceCode, String serviceName,
        String description, String moduleCode, BigDecimal suggestedPrice, BigDecimal practicePrice,
        BigDecimal effectivePrice, String currency, String billingType,
        ServicePricingMode pricingMode, boolean enabled) {}
