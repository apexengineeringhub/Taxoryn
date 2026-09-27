package com.taxoryn.module.moduleconfig.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.TenantAccessDeniedException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.moduleconfig.dto.EffectiveConfigurationResponse;
import com.taxoryn.module.moduleconfig.dto.OrganizationModuleDto;
import com.taxoryn.module.moduleconfig.dto.ProductModuleDto;
import com.taxoryn.module.moduleconfig.entity.OrganizationFeatureEntity;
import com.taxoryn.module.moduleconfig.entity.OrganizationModuleEntity;
import com.taxoryn.module.moduleconfig.entity.ProductFeatureEntity;
import com.taxoryn.module.moduleconfig.entity.ProductModuleEntity;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.repository.OrganizationFeatureRepository;
import com.taxoryn.module.moduleconfig.repository.OrganizationModuleRepository;
import com.taxoryn.module.moduleconfig.repository.ProductFeatureRepository;
import com.taxoryn.module.moduleconfig.repository.ProductModuleRepository;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.subscription.entity.SubscriptionEntity;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionStatus;
import com.taxoryn.module.subscription.repository.SubscriptionRepository;
import com.taxoryn.module.subscription.service.SubscriptionPlanEntitlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModuleConfigurationServiceImpl implements ModuleConfigurationService {

    private final ProductModuleRepository productModuleRepository;
    private final OrganizationModuleRepository organizationModuleRepository;
    private final ProductFeatureRepository productFeatureRepository;
    private final OrganizationFeatureRepository organizationFeatureRepository;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanEntitlementService subscriptionPlanEntitlementService;
    private final LocationRepository locationRepository;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public boolean isModuleEnabled(ProductModuleCode moduleCode) {
        UUID currentOrgId = SecurityUtils.getCurrentOrganizationId();
        if (currentOrgId == null) {
            return false;
        }
        return isModuleEnabled(currentOrgId, moduleCode);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isModuleEnabled(UUID organizationId, ProductModuleCode moduleCode) {
        if (organizationId == null || moduleCode == null) {
            return false;
        }

        ProductModuleCode effectiveCode = moduleCode;
        if (moduleCode == ProductModuleCode.GST_COMPLIANCE) {
            effectiveCode = ProductModuleCode.GST;
        } else if (moduleCode == ProductModuleCode.ITR_COMPLIANCE) {
            effectiveCode = ProductModuleCode.ITR;
        }

        // 1. Check Subscription Plan Entitlement Gate
        SubscriptionEntity subscription = subscriptionRepository.findByOrganizationId(organizationId).orElse(null);
        if (subscription != null) {
            if (!subscriptionPlanEntitlementService.isModuleEntitled(subscription.getPlan(), effectiveCode.name())) {
                return false;
            }
        }

        // 2. Check if an explicit organization configuration exists
        Optional<OrganizationModuleEntity> configOpt = organizationModuleRepository
                .findByOrganizationIdAndModuleCode(organizationId, effectiveCode);

        if (configOpt.isPresent()) {
            return configOpt.get().isEnabled();
        }

        // 3. Fall back to catalog default
        return productModuleRepository.findByCode(moduleCode)
                .map(ProductModuleEntity::isEnabledByDefault)
                .orElse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<ProductModuleCode> getEnabledModules(UUID organizationId) {
        validateTenantAccess(organizationId);

        List<OrganizationModuleDto> modules = getOrganizationModules(organizationId);
        Set<ProductModuleCode> enabledSet = EnumSet.noneOf(ProductModuleCode.class);

        for (OrganizationModuleDto mod : modules) {
            if (mod.isEnabled() && mod.isEntitled()) {
                enabledSet.add(mod.getModuleCode());
            }
        }

        return enabledSet;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationModuleDto> getOrganizationModules(UUID organizationId) {
        validateTenantAccess(organizationId);

        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization", "id", organizationId);
        }

        List<ProductModuleEntity> catalog = productModuleRepository.findAllByOrderByDisplayOrderAsc();
        List<OrganizationModuleEntity> orgConfigs = organizationModuleRepository.findByOrganizationId(organizationId);
        SubscriptionEntity subscription = subscriptionRepository.findByOrganizationId(organizationId).orElse(null);

        Map<ProductModuleCode, OrganizationModuleEntity> configMap = orgConfigs.stream()
                .collect(Collectors.toMap(OrganizationModuleEntity::getModuleCode, c -> c));

        return catalog.stream()
                .map(cat -> mapToOrganizationDto(organizationId, cat, configMap.get(cat.getCode()), subscription))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationModuleDto getOrganizationModule(UUID organizationId, ProductModuleCode moduleCode) {
        validateTenantAccess(organizationId);

        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization", "id", organizationId);
        }

        ProductModuleEntity catalogModule = productModuleRepository.findByCode(moduleCode)
                .orElseThrow(() -> new ResourceNotFoundException("ProductModule", "code", moduleCode));

        Optional<OrganizationModuleEntity> configOpt = organizationModuleRepository
                .findByOrganizationIdAndModuleCode(organizationId, moduleCode);

        SubscriptionEntity subscription = subscriptionRepository.findByOrganizationId(organizationId).orElse(null);

        return mapToOrganizationDto(organizationId, catalogModule, configOpt.orElse(null), subscription);
    }

    @Override
    @Transactional
    public OrganizationModuleDto updateModuleStatus(UUID organizationId, ProductModuleCode moduleCode, boolean enabled) {
        validateTenantAccess(organizationId);

        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization", "id", organizationId);
        }

        ProductModuleEntity catalogModule = productModuleRepository.findByCode(moduleCode)
                .orElseThrow(() -> new ResourceNotFoundException("ProductModule", "code", moduleCode));

        SubscriptionEntity subscription = subscriptionRepository.findByOrganizationId(organizationId).orElse(null);

        // Enforce: Organization cannot enable a module not included in its plan entitlement
        if (enabled && subscription != null) {
            if (!subscriptionPlanEntitlementService.isModuleEntitled(subscription.getPlan(), moduleCode.name())) {
                throw new BusinessValidationException("Cannot enable module " + moduleCode.name() +
                        " because it is not included in current subscription plan (" + subscription.getPlan().name() + "). Please upgrade your subscription first.");
            }
        }

        Optional<OrganizationModuleEntity> existingOpt = organizationModuleRepository
                .findByOrganizationIdAndModuleCode(organizationId, moduleCode);

        boolean previousEnabled;
        OrganizationModuleEntity entity;

        if (existingOpt.isPresent()) {
            entity = existingOpt.get();
            previousEnabled = entity.isEnabled();
            entity.setEnabled(enabled);
        } else {
            previousEnabled = catalogModule.isEnabledByDefault();
            entity = OrganizationModuleEntity.builder()
                    .moduleCode(moduleCode)
                    .enabled(enabled)
                    .build();
            entity.setOrganizationId(organizationId);
        }

        OrganizationModuleEntity saved = organizationModuleRepository.save(entity);

        if (existingOpt.isEmpty() || previousEnabled != enabled) {
            UUID userId = SecurityUtils.getCurrentUser().map(com.taxoryn.core.security.SecurityUser::getUserId).orElse(null);
            auditService.logEvent(
                    organizationId,
                    userId,
                    enabled ? "ORGANIZATION_MODULE_ENABLED" : "ORGANIZATION_MODULE_DISABLED",
                    "ORGANIZATION_MODULE",
                    moduleCode.name(),
                    Map.of("moduleCode", moduleCode.name(), "enabled", previousEnabled),
                    Map.of("moduleCode", moduleCode.name(), "enabled", enabled)
            );
            log.info("Organization module configuration updated: orgId={}, module={}, enabled={}, previous={}",
                    organizationId, moduleCode, enabled, previousEnabled);
        }

        return mapToOrganizationDto(organizationId, catalogModule, saved, subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductModuleDto> getAllProductModules() {
        return productModuleRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::mapToProductModuleDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EffectiveConfigurationResponse getEffectiveConfiguration(UUID organizationId) {
        validateTenantAccess(organizationId);

        SubscriptionEntity subscription = subscriptionRepository.findByOrganizationId(organizationId).orElse(null);
        String planCode = subscription != null && subscription.getPlan() != null ? subscription.getPlan().name() : "STARTER";
        String statusStr = subscription != null && subscription.getStatus() != null ? subscription.getStatus().name() : "ACTIVE";

        boolean multiLoc = subscriptionPlanEntitlementService.isMultiLocationEnabled(planCode);
        int maxLocs = subscriptionPlanEntitlementService.getMaxLocations(planCode);
        long activeLocCount = locationRepository.countByOrganizationIdAndIsActiveTrue(organizationId);

        List<ProductModuleEntity> modules = productModuleRepository.findAllByOrderByDisplayOrderAsc();
        List<OrganizationModuleEntity> orgModuleConfigs = organizationModuleRepository.findByOrganizationId(organizationId);
        Map<ProductModuleCode, Boolean> orgModuleMap = orgModuleConfigs.stream()
                .collect(Collectors.toMap(OrganizationModuleEntity::getModuleCode, OrganizationModuleEntity::isEnabled));

        Map<String, Boolean> effectiveModules = new LinkedHashMap<>();
        for (ProductModuleEntity mod : modules) {
            boolean entitled = subscriptionPlanEntitlementService.isModuleEntitled(planCode, mod.getCode().name());
            boolean configured = orgModuleMap.getOrDefault(mod.getCode(), mod.isEnabledByDefault());
            boolean effective = entitled && configured;
            effectiveModules.put(mod.getCode().name(), effective);
        }

        List<ProductFeatureEntity> features = productFeatureRepository.findAllByOrderByDisplayOrderAsc();
        List<OrganizationFeatureEntity> orgFeatureConfigs = organizationFeatureRepository.findByOrganizationId(organizationId);
        Map<String, Boolean> orgFeatureMap = orgFeatureConfigs.stream()
                .collect(Collectors.toMap(f -> f.getModuleCode() + ":" + f.getFeatureCode(), OrganizationFeatureEntity::isEnabled));

        Map<String, Map<String, Boolean>> effectiveFeatures = new LinkedHashMap<>();
        for (ProductFeatureEntity feat : features) {
            String modCode = feat.getModuleCode();
            boolean moduleEffective = effectiveModules.getOrDefault(modCode, false);

            boolean featEntitled = subscriptionPlanEntitlementService.isFeatureEntitled(planCode, modCode, feat.getCode());
            boolean featConfigured = orgFeatureMap.getOrDefault(modCode + ":" + feat.getCode(), feat.isEnabledByDefault());
            boolean featEffective = moduleEffective && featEntitled && featConfigured;

            effectiveFeatures.computeIfAbsent(modCode, k -> new LinkedHashMap<>()).put(feat.getCode(), featEffective);
        }

        List<String> navigationItems = new ArrayList<>();
        navigationItems.add("DASHBOARD");
        navigationItems.add("CLIENTS");

        if (Boolean.TRUE.equals(effectiveModules.get("GST"))) navigationItems.add("GST");
        if (Boolean.TRUE.equals(effectiveModules.get("ITR"))) navigationItems.add("ITR");
        if (Boolean.TRUE.equals(effectiveModules.get("TDS"))) navigationItems.add("TDS");
        if (Boolean.TRUE.equals(effectiveModules.get("TAX_NOTICES"))) navigationItems.add("TAX_NOTICES");
        if (Boolean.TRUE.equals(effectiveModules.get("BILLING"))) navigationItems.add("BILLING");
        if (Boolean.TRUE.equals(effectiveModules.get("DOCUMENT"))) navigationItems.add("DOCUMENTS");
        if (Boolean.TRUE.equals(effectiveModules.get("COMMUNICATION"))) navigationItems.add("COMMUNICATION");
        if (Boolean.TRUE.equals(effectiveModules.get("REPORTS"))) navigationItems.add("REPORTS");

        if (multiLoc || activeLocCount > 1) {
            navigationItems.add("LOCATIONS");
        }
        navigationItems.add("SETTINGS");

        return EffectiveConfigurationResponse.builder()
                .organizationId(organizationId)
                .subscriptionPlan(planCode)
                .subscriptionStatus(statusStr)
                .multiLocationEnabled(multiLoc)
                .maxLocations(maxLocs)
                .activeLocationCount((int) activeLocCount)
                .modules(effectiveModules)
                .features(effectiveFeatures)
                .navigationItems(navigationItems)
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

    private OrganizationModuleDto mapToOrganizationDto(
            UUID organizationId,
            ProductModuleEntity catalog,
            OrganizationModuleEntity config,
            SubscriptionEntity subscription) {

        boolean isEnabled = config != null ? config.isEnabled() : catalog.isEnabledByDefault();
        boolean isExplicit = config != null;

        String subStatusStr = subscription != null && subscription.getStatus() != null
                ? subscription.getStatus().name()
                : SubscriptionStatus.ACTIVE.name();

        boolean isStatusValid = subscription == null
                || subscription.getStatus() == SubscriptionStatus.ACTIVE
                || subscription.getStatus() == SubscriptionStatus.TRIALING;

        boolean isEntitled = subscription == null || subscriptionPlanEntitlementService.isModuleEntitled(subscription.getPlan(), catalog.getCode().name());

        boolean effectiveAccess = isEnabled && isStatusValid && isEntitled;

        String accessStatus;
        String reason;

        if (!isEntitled) {
            accessStatus = "UPGRADE_REQUIRED";
            reason = "Current subscription plan does not include " + catalog.getCode().name() + ". Please upgrade your subscription.";
        } else if (!isEnabled) {
            accessStatus = "MODULE_DISABLED";
            reason = "Product module " + catalog.getCode().name() + " is administratively disabled for this organization.";
        } else if (!isStatusValid) {
            accessStatus = "SUBSCRIPTION_REQUIRED";
            reason = "Active subscription required to access " + catalog.getCode().name() + ". Current status: " + subStatusStr + ".";
        } else {
            accessStatus = "AVAILABLE";
            reason = "Module is active and available.";
        }

        return OrganizationModuleDto.builder()
                .organizationId(organizationId)
                .moduleCode(catalog.getCode())
                .moduleName(catalog.getName())
                .moduleDescription(catalog.getDescription())
                .category(catalog.getCategory())
                .enabled(isEnabled)
                .explicitlyConfigured(isExplicit)
                .entitled(isEntitled)
                .subscriptionStatus(subStatusStr)
                .effectiveAccess(effectiveAccess)
                .accessStatus(accessStatus)
                .reason(reason)
                .updatedAt(config != null ? config.getUpdatedAt() : catalog.getUpdatedAt())
                .build();
    }

    private ProductModuleDto mapToProductModuleDto(ProductModuleEntity entity) {
        return ProductModuleDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .status(entity.getStatus())
                .enabledByDefault(entity.isEnabledByDefault())
                .displayOrder(entity.getDisplayOrder())
                .build();
    }
}
