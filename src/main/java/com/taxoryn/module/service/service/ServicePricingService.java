package com.taxoryn.module.service.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.service.dto.PracticeServicePriceDto;
import com.taxoryn.module.service.dto.UpdatePracticeServicePricingRequest;
import com.taxoryn.module.service.entity.PracticeServicePricingEntity;
import com.taxoryn.module.service.model.ServicePricingMode;
import com.taxoryn.module.service.repository.PracticeServicePricingRepository;
import com.taxoryn.module.marketplace.entity.TaxServiceEntity;
import com.taxoryn.module.marketplace.repository.TaxServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServicePricingService {
    private final TaxServiceRepository serviceRepository;
    private final PracticeServicePricingRepository pricingRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<PracticeServicePriceDto> getPracticePricing() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        Map<UUID, PracticeServicePricingEntity> configs = pricingRepository.findAllByOrganizationId(organizationId).stream()
                .collect(Collectors.toMap(PracticeServicePricingEntity::getTaxServiceId, Function.identity()));
        return serviceRepository.findAllActiveWithCategory().stream()
                .filter(service -> service.getSuggestedPrice() != null)
                .map(service -> toDto(service, configs.get(service.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PracticeServicePriceDto getEffectiveServicePrice(UUID organizationId, String serviceCode) {
        if (organizationId == null || serviceCode == null || serviceCode.isBlank()) throw new BadRequestException("Organization and service code are required");
        TaxServiceEntity service = serviceRepository.findByCodeIgnoreCase(serviceCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Service", "serviceCode", serviceCode));
        if (!Boolean.TRUE.equals(service.getIsActive()) || service.getSuggestedPrice() == null)
            throw new BadRequestException("Service is inactive or has no suggested price");
        PracticeServicePricingEntity config = pricingRepository.findByOrganizationIdAndTaxServiceId(organizationId, service.getId()).orElse(null);
        PracticeServicePriceDto result = toDto(service, config);
        if (!result.enabled()) throw new BadRequestException("Service is disabled for this practice");
        return result;
    }

    @Transactional
    public PracticeServicePriceDto updatePracticePricing(String serviceCode, UpdatePracticeServicePricingRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        TaxServiceEntity service = serviceRepository.findByCodeIgnoreCase(serviceCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Service", "serviceCode", serviceCode));
        if (service.getSuggestedPrice() == null || !Boolean.TRUE.equals(service.getIsActive()))
            throw new BadRequestException("Service is inactive or not configured for pricing");
        if (request.pricingMode() == ServicePricingMode.CUSTOM && request.customPrice() == null)
            throw new BadRequestException("Custom price is required when CUSTOM pricing mode is selected");
        if (request.pricingMode() == ServicePricingMode.DEFAULT && request.customPrice() != null)
            throw new BadRequestException("Custom price must be omitted when DEFAULT pricing mode is selected");

        PracticeServicePricingEntity config = pricingRepository.findByOrganizationIdAndTaxServiceId(organizationId, service.getId())
                .orElseGet(() -> {
                    PracticeServicePricingEntity created = PracticeServicePricingEntity.builder().taxServiceId(service.getId()).build();
                    created.setOrganizationId(organizationId);
                    return created;
                });
        ServicePricingMode oldMode = config.getId() == null ? ServicePricingMode.DEFAULT : config.getPricingMode();
        BigDecimal oldPrice = config.getId() == null || config.getPricingMode() == ServicePricingMode.DEFAULT
                ? service.getSuggestedPrice() : config.getCustomPrice();
        config.setPricingMode(request.pricingMode());
        config.setCustomPrice(request.customPrice());
        config.setEnabled(request.enabled());
        config = pricingRepository.save(config);
        PracticeServicePriceDto updated = toDto(service, config);
        auditService.logEvent(organizationId, SecurityUtils.getCurrentUserId(), "PRACTICE_SERVICE_PRICE_UPDATED",
                "PRACTICE_SERVICE_PRICING", config.getId().toString(),
                Map.of("taxServiceId", service.getId(), "serviceCode", service.getCode(), "pricingMode", oldMode, "price", oldPrice),
                Map.of("taxServiceId", service.getId(), "serviceCode", service.getCode(), "pricingMode", request.pricingMode(), "price", updated.practicePrice(), "enabled", request.enabled()));
        return updated;
    }

    private PracticeServicePriceDto toDto(TaxServiceEntity service, PracticeServicePricingEntity config) {
        ServicePricingMode mode = config == null ? ServicePricingMode.DEFAULT : config.getPricingMode();
        boolean enabled = config == null || config.isEnabled();
        BigDecimal practicePrice = mode == ServicePricingMode.CUSTOM && config != null ? config.getCustomPrice() : service.getSuggestedPrice();
        return new PracticeServicePriceDto(service.getId(), service.getCode(), service.getName(),
                service.getDescription(), moduleCode(service), service.getSuggestedPrice(), practicePrice,
                enabled ? practicePrice : null, service.getCurrency(), service.getBillingType(), mode, enabled);
    }

    private String moduleCode(TaxServiceEntity service) {
        String category = service.getCategory().getCode();
        return switch (category) {
            case "INCOME_TAX" -> "ITR";
            case "GST" -> "GST";
            case "TDS" -> "TDS";
            case "TAX_NOTICES" -> "TAX_NOTICES";
            default -> "ADVISORY";
        };
    }
}
