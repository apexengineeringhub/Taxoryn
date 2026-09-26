package com.taxoryn.module.moduleconfig.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.TenantAccessDeniedException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.moduleconfig.dto.OrganizationFeatureDto;
import com.taxoryn.module.moduleconfig.dto.ProductFeatureDto;
import com.taxoryn.module.moduleconfig.entity.OrganizationFeatureEntity;
import com.taxoryn.module.moduleconfig.entity.ProductFeatureEntity;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.repository.OrganizationFeatureRepository;
import com.taxoryn.module.moduleconfig.repository.ProductFeatureRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.subscription.entity.SubscriptionEntity;
import com.taxoryn.module.subscription.repository.SubscriptionRepository;
import com.taxoryn.module.subscription.service.SubscriptionPlanEntitlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureConfigurationServiceImpl implements FeatureConfigurationService {

    private final ProductFeatureRepository productFeatureRepository;
    private final OrganizationFeatureRepository organizationFeatureRepository;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanEntitlementService subscriptionPlanEntitlementService;
    private final ModuleConfigurationService moduleConfigurationService;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public boolean isFeatureEnabled(String moduleCode, String featureCode) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        if (orgId == null) {
            return false;
        }
        return isFeatureEnabled(orgId, moduleCode, featureCode);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFeatureEnabled(UUID organizationId, String moduleCode, String featureCode) {
        if (organizationId == null || moduleCode == null || featureCode == null) {
            return false;
        }

        // 1. Parent Module Gate
        try {
            ProductModuleCode parsedCode = ProductModuleCode.valueOf(moduleCode.toUpperCase());
            if (!moduleConfigurationService.isModuleEnabled(organizationId, parsedCode)) {
                return false;
            }
        } catch (IllegalArgumentException ignored) {
        }

        // 2. Subscription Plan Entitlement Gate
        SubscriptionEntity subscription = subscriptionRepository.findByOrganizationId(organizationId).orElse(null);
        if (subscription != null) {
            if (!subscriptionPlanEntitlementService.isFeatureEntitled(subscription.getPlan(), moduleCode, featureCode)) {
                return false;
            }
        }

        // 3. Organization Feature Configuration Gate
        Optional<OrganizationFeatureEntity> orgConfig = organizationFeatureRepository
                .findByOrganizationIdAndModuleCodeAndFeatureCode(organizationId, moduleCode.toUpperCase(), featureCode.toUpperCase());

        if (orgConfig.isPresent()) {
            return orgConfig.get().isEnabled();
        }

        // 4. Fall back to Product Feature Master default
        return productFeatureRepository.findByModuleCodeAndCode(moduleCode.toUpperCase(), featureCode.toUpperCase())
                .map(ProductFeatureEntity::isEnabledByDefault)
                .orElse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyFeatureAccess(UUID organizationId, String moduleCode, String featureCode) {
        if (organizationId == null) {
            throw new UnauthorizedException("Organization context is required");
        }
        if (!isFeatureEnabled(organizationId, moduleCode, featureCode)) {
            log.warn("Access denied: Feature {}::{} is disabled or not entitled for organization {}",
                    moduleCode, featureCode, organizationId);
            throw new ForbiddenException("Feature " + featureCode + " under module " + moduleCode + " is disabled for this organization.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationFeatureDto> getOrganizationFeatures(UUID organizationId, String moduleCode) {
        validateTenantAccess(organizationId);

        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization", "id", organizationId);
        }

        List<ProductFeatureEntity> catalogFeatures = productFeatureRepository
                .findByModuleCodeOrderByDisplayOrderAsc(moduleCode.toUpperCase());

        List<OrganizationFeatureEntity> orgConfigs = organizationFeatureRepository
                .findByOrganizationIdAndModuleCode(organizationId, moduleCode.toUpperCase());

        Map<String, OrganizationFeatureEntity> configMap = orgConfigs.stream()
                .collect(Collectors.toMap(OrganizationFeatureEntity::getFeatureCode, c -> c));

        SubscriptionEntity subscription = subscriptionRepository.findByOrganizationId(organizationId).orElse(null);

        return catalogFeatures.stream()
                .map(cat -> mapToOrganizationFeatureDto(organizationId, cat, configMap.get(cat.getCode()), subscription))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationFeatureDto> getAllOrganizationFeatures(UUID organizationId) {
        validateTenantAccess(organizationId);

        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization", "id", organizationId);
        }

        List<ProductFeatureEntity> catalogFeatures = productFeatureRepository.findAllByOrderByDisplayOrderAsc();
        List<OrganizationFeatureEntity> orgConfigs = organizationFeatureRepository.findByOrganizationId(organizationId);

        Map<String, OrganizationFeatureEntity> configMap = orgConfigs.stream()
                .collect(Collectors.toMap(c -> c.getModuleCode() + ":" + c.getFeatureCode(), c -> c));

        SubscriptionEntity subscription = subscriptionRepository.findByOrganizationId(organizationId).orElse(null);

        return catalogFeatures.stream()
                .map(cat -> mapToOrganizationFeatureDto(organizationId, cat, configMap.get(cat.getModuleCode() + ":" + cat.getCode()), subscription))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrganizationFeatureDto updateFeatureStatus(UUID organizationId, String moduleCode, String featureCode, boolean enabled) {
        validateTenantAccess(organizationId);

        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization", "id", organizationId);
        }

        ProductFeatureEntity catalogFeature = productFeatureRepository
                .findByModuleCodeAndCode(moduleCode.toUpperCase(), featureCode.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("ProductFeature", "code", moduleCode + ":" + featureCode));

        SubscriptionEntity subscription = subscriptionRepository.findByOrganizationId(organizationId).orElse(null);

        if (enabled && subscription != null) {
            if (!subscriptionPlanEntitlementService.isFeatureEntitled(subscription.getPlan(), moduleCode, featureCode)) {
                throw new BusinessValidationException("Cannot enable feature " + featureCode + " because it is not included in your subscription plan (" + subscription.getPlan().name() + ").");
            }
        }

        Optional<OrganizationFeatureEntity> existingOpt = organizationFeatureRepository
                .findByOrganizationIdAndModuleCodeAndFeatureCode(organizationId, moduleCode.toUpperCase(), featureCode.toUpperCase());

        OrganizationFeatureEntity entity;
        boolean previousEnabled;

        if (existingOpt.isPresent()) {
            entity = existingOpt.get();
            previousEnabled = entity.isEnabled();
            entity.setEnabled(enabled);
        } else {
            previousEnabled = catalogFeature.isEnabledByDefault();
            entity = OrganizationFeatureEntity.builder()
                    .organizationId(organizationId)
                    .moduleCode(moduleCode.toUpperCase())
                    .featureCode(featureCode.toUpperCase())
                    .enabled(enabled)
                    .build();
        }

        OrganizationFeatureEntity saved = organizationFeatureRepository.save(entity);

        if (existingOpt.isEmpty() || previousEnabled != enabled) {
            auditService.logEvent(
                    organizationId,
                    SecurityUtils.getCurrentUserId(),
                    enabled ? "ORGANIZATION_FEATURE_ENABLED" : "ORGANIZATION_FEATURE_DISABLED",
                    "ORGANIZATION_FEATURE",
                    moduleCode + ":" + featureCode,
                    Map.of("enabled", previousEnabled),
                    Map.of("enabled", enabled)
            );
        }

        return mapToOrganizationFeatureDto(organizationId, catalogFeature, saved, subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductFeatureDto> getCatalogFeatures(String moduleCode) {
        return productFeatureRepository.findByModuleCodeOrderByDisplayOrderAsc(moduleCode.toUpperCase()).stream()
                .map(this::mapToProductFeatureDto)
                .collect(Collectors.toList());
    }

    private OrganizationFeatureDto mapToOrganizationFeatureDto(
            UUID organizationId,
            ProductFeatureEntity catalog,
            OrganizationFeatureEntity config,
            SubscriptionEntity subscription) {

        boolean isEnabled = config != null ? config.isEnabled() : catalog.isEnabledByDefault();
        boolean isExplicit = config != null;
        boolean isEntitled = subscription == null || subscriptionPlanEntitlementService.isFeatureEntitled(subscription.getPlan(), catalog.getModuleCode(), catalog.getCode());
        boolean effectiveAccess = isEnabled && isEntitled;

        return OrganizationFeatureDto.builder()
                .organizationId(organizationId)
                .moduleCode(catalog.getModuleCode())
                .featureCode(catalog.getCode())
                .featureName(catalog.getName())
                .featureDescription(catalog.getDescription())
                .enabled(isEnabled)
                .explicitlyConfigured(isExplicit)
                .entitled(isEntitled)
                .effectiveAccess(effectiveAccess)
                .updatedAt(config != null ? config.getUpdatedAt() : catalog.getUpdatedAt())
                .build();
    }

    private ProductFeatureDto mapToProductFeatureDto(ProductFeatureEntity entity) {
        return ProductFeatureDto.builder()
                .id(entity.getId())
                .moduleCode(entity.getModuleCode())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .enabledByDefault(entity.isEnabledByDefault())
                .displayOrder(entity.getDisplayOrder())
                .build();
    }

    private void validateTenantAccess(UUID organizationId) {
        if (organizationId == null) {
            throw new UnauthorizedException("Organization ID is required");
        }
        UUID currentTenant = SecurityUtils.getCurrentOrganizationId();
        if (currentTenant == null) {
            throw new UnauthorizedException("Authenticated tenant context is required");
        }
        if (!SecurityUtils.isTaxorynSuperAdmin() && !currentTenant.equals(organizationId)) {
            throw new TenantAccessDeniedException("Cross-tenant access violation: Target organization " +
                    organizationId + " does not match authenticated tenant " + currentTenant);
        }
    }
}
