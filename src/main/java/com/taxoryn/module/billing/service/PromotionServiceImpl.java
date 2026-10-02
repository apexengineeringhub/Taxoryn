package com.taxoryn.module.billing.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.billing.dto.CreatePromotionRequest;
import com.taxoryn.module.billing.dto.PriceResolutionResultDto;
import com.taxoryn.module.billing.dto.PromotionDto;
import com.taxoryn.module.billing.dto.UpdatePromotionRequest;
import com.taxoryn.module.billing.dto.UpdatePromotionStatusRequest;
import com.taxoryn.module.billing.entity.BillingProfileEntity;
import com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus;
import com.taxoryn.module.billing.entity.InvoiceItemEntity.BillingServiceType;
import com.taxoryn.module.billing.entity.PromotionEntity;
import com.taxoryn.module.billing.model.PricingType;
import com.taxoryn.module.billing.model.PromotionDiscountType;
import com.taxoryn.module.billing.model.PromotionType;
import com.taxoryn.module.billing.repository.BillingProfileRepository;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.billing.repository.PromotionRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.service.service.ServicePricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final BillingProfileRepository billingProfileRepository;
    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final AuditService auditService;
    private final PracticeSecurityScopeEvaluator securityScopeEvaluator;
    private final ServicePricingService servicePricingService;

    private static final Map<BillingServiceType, BigDecimal> DEFAULT_STANDARD_PRICES = Map.of(
            BillingServiceType.GST_FILING, new BigDecimal("2500.00"),
            BillingServiceType.ITR_FILING, new BigDecimal("3500.00"),
            BillingServiceType.TDS, new BigDecimal("2000.00"),
            BillingServiceType.ACCOUNTING, new BigDecimal("5000.00"),
            BillingServiceType.CONSULTING, new BigDecimal("3000.00"),
            BillingServiceType.AUDIT, new BigDecimal("15000.00"),
            BillingServiceType.ROC_COMPLIANCE, new BigDecimal("4000.00"),
            BillingServiceType.OTHER, new BigDecimal("2000.00")
    );

    @Override
    @Transactional
    public PromotionDto createPromotion(CreatePromotionRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        validateAdminOrBillingAccess();

        if (StringUtils.hasText(request.getCode())) {
            String code = request.getCode().trim().toUpperCase();
            if (promotionRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, code)) {
                throw new DuplicateResourceException("Promotion", "code", code);
            }
        }

        if (request.getDiscountType() == PromotionDiscountType.PERCENTAGE) {
            if (request.getDiscountValue().compareTo(new BigDecimal("100.00")) > 0) {
                throw new BadRequestException("Percentage discount cannot exceed 100%");
            }
        }

        if (request.getValidFrom() != null && request.getValidUntil() != null) {
            if (request.getValidUntil().isBefore(request.getValidFrom())) {
                throw new BadRequestException("Promotion validUntil date cannot be before validFrom date");
            }
        }

        if (request.getTargetClientId() != null) {
            if (clientRepository.findByIdAndOrganizationId(request.getTargetClientId(), organizationId).isEmpty()) {
                throw new ResourceNotFoundException("Client", "id", request.getTargetClientId());
            }
        }

        PromotionEntity entity = PromotionEntity.builder()
                .code(StringUtils.hasText(request.getCode()) ? request.getCode().trim().toUpperCase() : null)
                .name(request.getName().trim())
                .description(request.getDescription())
                .promotionType(request.getPromotionType())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .targetService(request.getTargetService())
                .targetClientId(request.getTargetClientId())
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .active(request.getActive() != null ? request.getActive() : true)
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .maxUses(request.getMaxUses())
                .currentUses(0)
                .build();
        entity.setOrganizationId(organizationId);

        PromotionEntity saved = promotionRepository.save(entity);
        log.info("Created promotion: id={}, name={}, type={} in tenant={}", saved.getId(), saved.getName(), saved.getPromotionType(), organizationId);

        PromotionDto dto = mapToDto(saved);
        auditService.logEvent("PROMOTION_CREATED", "PROMOTION", saved.getId().toString(), null, dto);
        return dto;
    }

    @Override
    @Transactional
    public PromotionDto updatePromotion(UUID id, UpdatePromotionRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        validateAdminOrBillingAccess();

        PromotionEntity entity = promotionRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", id));

        PromotionDto oldSnapshot = mapToDto(entity);

        if (StringUtils.hasText(request.getCode())) {
            String code = request.getCode().trim().toUpperCase();
            if (!code.equalsIgnoreCase(entity.getCode()) && promotionRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, code)) {
                throw new DuplicateResourceException("Promotion", "code", code);
            }
            entity.setCode(code);
        }

        if (request.getName() != null) {
            entity.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getPromotionType() != null) {
            entity.setPromotionType(request.getPromotionType());
        }
        if (request.getDiscountType() != null) {
            entity.setDiscountType(request.getDiscountType());
        }
        if (request.getDiscountValue() != null) {
            PromotionDiscountType currentType = entity.getDiscountType();
            if (currentType == PromotionDiscountType.PERCENTAGE && request.getDiscountValue().compareTo(new BigDecimal("100.00")) > 0) {
                throw new BadRequestException("Percentage discount cannot exceed 100%");
            }
            entity.setDiscountValue(request.getDiscountValue());
        }
        if (request.getTargetService() != null) {
            entity.setTargetService(request.getTargetService());
        }
        if (request.getTargetClientId() != null) {
            if (clientRepository.findByIdAndOrganizationId(request.getTargetClientId(), organizationId).isEmpty()) {
                throw new ResourceNotFoundException("Client", "id", request.getTargetClientId());
            }
            entity.setTargetClientId(request.getTargetClientId());
        }
        if (request.getValidFrom() != null) {
            entity.setValidFrom(request.getValidFrom());
        }
        if (request.getValidUntil() != null) {
            entity.setValidUntil(request.getValidUntil());
        }
        if (entity.getValidFrom() != null && entity.getValidUntil() != null && entity.getValidUntil().isBefore(entity.getValidFrom())) {
            throw new BadRequestException("Promotion validUntil date cannot be before validFrom date");
        }
        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }
        if (request.getPriority() != null) {
            entity.setPriority(request.getPriority());
        }
        if (request.getMaxUses() != null) {
            entity.setMaxUses(request.getMaxUses());
        }

        PromotionEntity saved = promotionRepository.save(entity);
        log.info("Updated promotion: id={} in tenant={}", saved.getId(), organizationId);

        PromotionDto dto = mapToDto(saved);
        auditService.logEvent("PROMOTION_UPDATED", "PROMOTION", saved.getId().toString(), oldSnapshot, dto);
        return dto;
    }

    @Override
    @Transactional
    public PromotionDto updatePromotionStatus(UUID id, UpdatePromotionStatusRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        validateAdminOrBillingAccess();

        PromotionEntity entity = promotionRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", id));

        entity.setActive(request.getActive());
        PromotionEntity saved = promotionRepository.save(entity);

        String event = Boolean.TRUE.equals(request.getActive()) ? "PROMOTION_ACTIVATED" : "PROMOTION_DEACTIVATED";
        log.info("Promotion {} status changed to active={} in tenant={}", id, request.getActive(), organizationId);

        PromotionDto dto = mapToDto(saved);
        auditService.logEvent(event, "PROMOTION", saved.getId().toString(), null, dto);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionDto getPromotionById(UUID id) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PromotionEntity entity = promotionRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", id));
        return mapToDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionDto> getPromotions(Boolean activeOnly) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        List<PromotionEntity> list;
        if (Boolean.TRUE.equals(activeOnly)) {
            list = promotionRepository.findAllByOrganizationIdAndActiveTrueOrderByPriorityDesc(organizationId);
        } else {
            list = promotionRepository.findAllByOrganizationId(organizationId);
        }
        return list.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deletePromotion(UUID id) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        validateAdminOrBillingAccess();

        PromotionEntity entity = promotionRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", id));

        promotionRepository.delete(entity);
        auditService.logEvent("PROMOTION_DELETED", "PROMOTION", id.toString(), null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PriceResolutionResultDto resolvePrice(
            UUID organizationId,
            UUID clientId,
            BillingServiceType service,
            BigDecimal manualPrice,
            String promoCode,
            LocalDate date
    ) {
        return resolvePriceInternal(organizationId, clientId, service, null, manualPrice, promoCode, date);
    }

    @Override
    @Transactional(readOnly = true)
    public PriceResolutionResultDto resolvePriceForCatalogService(UUID organizationId, UUID clientId,
            BillingServiceType service, String serviceCode, BigDecimal manualPrice, String promoCode, LocalDate date) {
        BigDecimal catalogPrice = servicePricingService.getEffectiveServicePrice(organizationId, serviceCode).effectivePrice();
        return resolvePriceInternal(organizationId, clientId, service, catalogPrice, manualPrice, promoCode, date);
    }

    private PriceResolutionResultDto resolvePriceInternal(UUID organizationId, UUID clientId,
            BillingServiceType service, BigDecimal catalogPrice, BigDecimal manualPrice, String promoCode, LocalDate date) {
        LocalDate evaluationDate = (date != null) ? date : LocalDate.now();
        BigDecimal standardBasePrice = catalogPrice != null ? catalogPrice : DEFAULT_STANDARD_PRICES.getOrDefault(service, new BigDecimal("2500.00"));

        // 1. Manual / Explicit custom rate provided
        if (manualPrice != null && manualPrice.compareTo(BigDecimal.ZERO) >= 0) {
            return PriceResolutionResultDto.builder()
                    .unitPrice(manualPrice)
                    .standardUnitPrice(standardBasePrice)
                    .pricingType(PricingType.CUSTOM)
                    .discountAmount(BigDecimal.ZERO)
                    .build();
        }

        // 2. Customer-Specific Pricing (from BillingProfileEntity)
        if (clientId != null) {
            Optional<BillingProfileEntity> profileOpt = billingProfileRepository.findFirstByOrganizationIdAndClientIdAndEngagementIdIsNullAndActiveTrue(organizationId, clientId);
            if (profileOpt.isPresent() && profileOpt.get().getDefaultRate() != null && profileOpt.get().getDefaultRate().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal customRate = profileOpt.get().getDefaultRate();
                return PriceResolutionResultDto.builder()
                        .unitPrice(customRate)
                        .standardUnitPrice(standardBasePrice)
                        .pricingType(PricingType.CUSTOMER_SPECIFIC)
                        .discountAmount(BigDecimal.ZERO)
                        .build();
            }
        }

        // 3. Promotional Pricing
        PromotionEntity matchedPromotion = null;

        if (StringUtils.hasText(promoCode)) {
            String code = promoCode.trim().toUpperCase();
            Optional<PromotionEntity> promoOpt = promotionRepository.findByOrganizationIdAndCodeIgnoreCase(organizationId, code);
            if (promoOpt.isPresent()) {
                PromotionEntity promo = promoOpt.get();
                if (promo.isCurrentlyValid(evaluationDate) && isPromotionTargetMatch(promo, organizationId, clientId, service)) {
                    matchedPromotion = promo;
                } else {
                    throw new BadRequestException("Promotion code '" + promoCode + "' is not valid or not eligible for this client/service");
                }
            } else {
                throw new BadRequestException("Promotion code '" + promoCode + "' not found");
            }
        } else {
            // Automatic resolution: find highest priority active promotion matching client and service
            List<PromotionEntity> activePromotions = promotionRepository.findAllByOrganizationIdAndActiveTrueOrderByPriorityDesc(organizationId);
            for (PromotionEntity promo : activePromotions) {
                if (promo.isCurrentlyValid(evaluationDate) && isPromotionTargetMatch(promo, organizationId, clientId, service)) {
                    matchedPromotion = promo;
                    break;
                }
            }
        }

        if (matchedPromotion != null) {
            BigDecimal finalUnitPrice;
            BigDecimal discountAmount;

            switch (matchedPromotion.getDiscountType()) {
                case PERCENTAGE -> {
                    discountAmount = standardBasePrice.multiply(matchedPromotion.getDiscountValue())
                            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    finalUnitPrice = standardBasePrice.subtract(discountAmount);
                    if (finalUnitPrice.compareTo(BigDecimal.ZERO) < 0) {
                        finalUnitPrice = BigDecimal.ZERO;
                    }
                }
                case FIXED_AMOUNT -> {
                    discountAmount = matchedPromotion.getDiscountValue().min(standardBasePrice);
                    finalUnitPrice = standardBasePrice.subtract(discountAmount);
                    if (finalUnitPrice.compareTo(BigDecimal.ZERO) < 0) {
                        finalUnitPrice = BigDecimal.ZERO;
                    }
                }
                case FIXED_PRICE -> {
                    finalUnitPrice = matchedPromotion.getDiscountValue();
                    discountAmount = standardBasePrice.subtract(finalUnitPrice);
                    if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
                        discountAmount = BigDecimal.ZERO;
                    }
                }
                default -> throw new IllegalStateException("Unexpected discount type: " + matchedPromotion.getDiscountType());
            }

            return PriceResolutionResultDto.builder()
                    .unitPrice(finalUnitPrice)
                    .standardUnitPrice(standardBasePrice)
                    .pricingType(PricingType.PROMOTIONAL)
                    .promotionId(matchedPromotion.getId())
                    .promotionName(matchedPromotion.getName())
                    .discountType(matchedPromotion.getDiscountType())
                    .discountValue(matchedPromotion.getDiscountValue())
                    .discountAmount(discountAmount)
                    .build();
        }

        // 4. Standard Price
        return PriceResolutionResultDto.builder()
                .unitPrice(standardBasePrice)
                .standardUnitPrice(standardBasePrice)
                .pricingType(PricingType.STANDARD)
                .discountAmount(BigDecimal.ZERO)
                .build();
    }

    @Override
    @Transactional
    public void incrementPromotionUse(UUID promotionId) {
        if (promotionId != null) {
            promotionRepository.findById(promotionId).ifPresent(promo -> {
                promo.setCurrentUses(promo.getCurrentUses() + 1);
                promotionRepository.save(promo);
            });
        }
    }

    private boolean isPromotionTargetMatch(PromotionEntity promo, UUID organizationId, UUID clientId, BillingServiceType service) {
        switch (promo.getPromotionType()) {
            case CUSTOMER_SPECIFIC -> {
                return promo.getTargetClientId() != null && promo.getTargetClientId().equals(clientId)
                        && (promo.getTargetService() == null || promo.getTargetService() == service);
            }
            case SERVICE_SPECIFIC -> {
                return promo.getTargetService() != null && promo.getTargetService() == service
                        && (promo.getTargetClientId() == null || promo.getTargetClientId().equals(clientId));
            }
            case NEW_CLIENT -> {
                boolean isNew = (clientId != null) && (invoiceRepository.countByOrganizationIdAndClientIdAndStatusNot(organizationId, clientId, InvoiceStatus.CANCELLED) == 0);
                return isNew && (promo.getTargetService() == null || promo.getTargetService() == service);
            }
            case GENERAL, SEASONAL, REFERRAL -> {
                boolean matchesService = (promo.getTargetService() == null || promo.getTargetService() == service);
                boolean matchesClient = (promo.getTargetClientId() == null || promo.getTargetClientId().equals(clientId));
                return matchesService && matchesClient;
            }
            default -> {
                return false;
            }
        }
    }

    private void validateAdminOrBillingAccess() {
        if (SecurityUtils.isClientPortalUser()) {
            throw new com.taxoryn.core.exception.ForbiddenException("Access denied: Client portal users cannot manage promotions");
        }
        if (securityScopeEvaluator != null) {
            PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
            if (scope != null && !scope.isFirmAdmin() && !securityScopeEvaluator.hasBillingAccess(scope)) {
                throw new com.taxoryn.core.exception.ForbiddenException("Access denied: Billing management permission required");
            }
        }
    }

    private PromotionDto mapToDto(PromotionEntity entity) {
        return PromotionDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .promotionType(entity.getPromotionType())
                .discountType(entity.getDiscountType())
                .discountValue(entity.getDiscountValue())
                .targetService(entity.getTargetService())
                .targetClientId(entity.getTargetClientId())
                .validFrom(entity.getValidFrom())
                .validUntil(entity.getValidUntil())
                .active(entity.getActive())
                .priority(entity.getPriority())
                .maxUses(entity.getMaxUses())
                .currentUses(entity.getCurrentUses())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}
