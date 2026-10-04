package com.taxoryn.module.service.dto;

import com.taxoryn.module.service.model.ServicePricingMode;
import com.taxoryn.module.service.model.ServiceScope;
import java.math.BigDecimal;
import java.util.UUID;

public record PracticeServicePriceDto(
        UUID serviceId,
        String serviceCode,
        String serviceName,
        String description,
        String moduleCode,
        BigDecimal suggestedPrice,
        BigDecimal practicePrice,
        BigDecimal effectivePrice,
        String currency,
        String billingType,
        ServicePricingMode pricingMode,
        boolean enabled,
        ServiceScope scope,
        BigDecimal taxRate
) {
    public PracticeServicePriceDto(
            UUID serviceId, String serviceCode, String serviceName,
            String description, String moduleCode, BigDecimal suggestedPrice, BigDecimal practicePrice,
            BigDecimal effectivePrice, String currency, String billingType,
            ServicePricingMode pricingMode, boolean enabled
    ) {
        this(serviceId, serviceCode, serviceName, description, moduleCode, suggestedPrice, practicePrice,
                effectivePrice, currency, billingType, pricingMode, enabled, ServiceScope.TAXORYN, new BigDecimal("18.00"));
    }
}
