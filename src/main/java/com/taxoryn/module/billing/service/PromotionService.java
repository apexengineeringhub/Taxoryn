package com.taxoryn.module.billing.service;

import com.taxoryn.module.billing.dto.CreatePromotionRequest;
import com.taxoryn.module.billing.dto.PriceResolutionResultDto;
import com.taxoryn.module.billing.dto.PromotionDto;
import com.taxoryn.module.billing.dto.UpdatePromotionRequest;
import com.taxoryn.module.billing.dto.UpdatePromotionStatusRequest;
import com.taxoryn.module.billing.entity.InvoiceItemEntity.BillingServiceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PromotionService {

    PromotionDto createPromotion(CreatePromotionRequest request);

    PromotionDto updatePromotion(UUID id, UpdatePromotionRequest request);

    PromotionDto updatePromotionStatus(UUID id, UpdatePromotionStatusRequest request);

    PromotionDto getPromotionById(UUID id);

    List<PromotionDto> getPromotions(Boolean activeOnly);

    void deletePromotion(UUID id);

    PriceResolutionResultDto resolvePrice(UUID organizationId, UUID clientId, BillingServiceType service, BigDecimal manualPrice, String promoCode, LocalDate date);

    void incrementPromotionUse(UUID promotionId);
}
