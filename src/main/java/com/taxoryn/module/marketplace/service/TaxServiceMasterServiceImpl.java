package com.taxoryn.module.marketplace.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.marketplace.dto.*;
import com.taxoryn.module.marketplace.entity.*;
import com.taxoryn.module.marketplace.mapper.TaxServiceMapper;
import com.taxoryn.module.marketplace.repository.*;
import com.taxoryn.module.marketplace.util.TaxServiceNormalizationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxServiceMasterServiceImpl implements TaxServiceMasterService {

    private final TaxServiceCategoryRepository categoryRepository;
    private final TaxServiceRepository taxServiceRepository;
    private final TaxServiceAliasRepository aliasRepository;
    private final PracticeServiceRepository practiceServiceRepository;
    private final MarketplaceProfileRepository profileRepository;
    private final TaxServiceMapper mapper;
    private final AuditService auditService;

    // =========================================================================
    // Admin Category Management
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<TaxServiceCategoryDto> getAllCategories() {
        List<TaxServiceCategoryEntity> categories = categoryRepository.findAllByOrderBySortOrderAsc();
        return mapper.toCategoryDtoList(categories);
    }

    @Override
    @Transactional(readOnly = true)
    public TaxServiceCategoryDto getCategoryById(UUID id) {
        TaxServiceCategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax Service Category", "id", id));
        return mapper.toCategoryDto(category);
    }

    @Override
    @Transactional
    public TaxServiceCategoryDto createCategory(CreateTaxServiceCategoryRequest request) {
        String code = request.getCode().trim().toUpperCase(Locale.ROOT);
        if (categoryRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("Tax service category with code '" + code + "' already exists");
        }

        TaxServiceCategoryEntity category = TaxServiceCategoryEntity.builder()
                .code(code)
                .name(request.getName().trim())
                .description(request.getDescription())
                .icon(request.getIcon() != null ? request.getIcon().trim() : "Layers")
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        TaxServiceCategoryEntity saved = categoryRepository.save(category);
        auditService.logEvent("TAX_SERVICE_CATEGORY_CREATED", "TAX_SERVICE_CATEGORY", saved.getId().toString(), null,
                "Created tax service category: " + saved.getCode());

        return mapper.toCategoryDto(saved);
    }

    @Override
    @Transactional
    public TaxServiceCategoryDto updateCategory(UUID id, UpdateTaxServiceCategoryRequest request) {
        TaxServiceCategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax Service Category", "id", id));

        if (StringUtils.hasText(request.getName())) {
            category.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getIcon() != null) {
            category.setIcon(request.getIcon().trim());
        }
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        TaxServiceCategoryEntity saved = categoryRepository.save(category);
        auditService.logEvent("TAX_SERVICE_CATEGORY_UPDATED", "TAX_SERVICE_CATEGORY", saved.getId().toString(), null,
                "Updated tax service category: " + saved.getCode());

        return mapper.toCategoryDto(saved);
    }

    @Override
    @Transactional
    public TaxServiceCategoryDto toggleCategoryStatus(UUID id, boolean isActive) {
        TaxServiceCategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax Service Category", "id", id));

        category.setIsActive(isActive);
        TaxServiceCategoryEntity saved = categoryRepository.save(category);

        auditService.logEvent(isActive ? "TAX_SERVICE_CATEGORY_ACTIVATED" : "TAX_SERVICE_CATEGORY_DEACTIVATED",
                "TAX_SERVICE_CATEGORY", saved.getId().toString(), null,
                (isActive ? "Activated" : "Deactivated") + " tax service category: " + saved.getCode());

        return mapper.toCategoryDto(saved);
    }

    // =========================================================================
    // Admin Service Master Management
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TaxServiceDto> getTaxServices(UUID categoryId, Boolean isActive, String search, Pageable pageable) {
        Specification<TaxServiceEntity> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }
            if (StringUtils.hasText(search)) {
                String term = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), term),
                        cb.like(cb.lower(root.get("name")), term),
                        cb.like(cb.lower(root.get("description")), term)
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<TaxServiceEntity> page = taxServiceRepository.findAll(spec, pageable);
        return PagedResponse.of(page, entity -> {
            TaxServiceDto dto = mapper.toTaxServiceDto(entity);
            List<TaxServiceAliasEntity> aliases = aliasRepository.findByTaxServiceId(entity.getId());
            dto.setAliases(mapper.toAliasDtoList(aliases));
            return dto;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public TaxServiceDto getTaxServiceById(UUID id) {
        TaxServiceEntity service = taxServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax Service", "id", id));
        TaxServiceDto dto = mapper.toTaxServiceDto(service);
        List<TaxServiceAliasEntity> aliases = aliasRepository.findByTaxServiceId(id);
        dto.setAliases(mapper.toAliasDtoList(aliases));
        return dto;
    }

    @Override
    @Transactional
    public TaxServiceDto createTaxService(CreateTaxServiceRequest request) {
        TaxServiceCategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Tax Service Category", "id", request.getCategoryId()));

        String code = request.getCode().trim().toUpperCase(Locale.ROOT);
        if (taxServiceRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("Tax service with code '" + code + "' already exists");
        }

        TaxServiceEntity service = TaxServiceEntity.builder()
                .categoryId(category.getId())
                .code(code)
                .name(request.getName().trim())
                .description(request.getDescription())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        TaxServiceEntity saved = taxServiceRepository.save(service);

        // Save initial aliases if provided
        if (request.getAliases() != null && !request.getAliases().isEmpty()) {
            for (String aliasStr : request.getAliases()) {
                if (StringUtils.hasText(aliasStr)) {
                    TaxServiceAliasEntity alias = TaxServiceAliasEntity.builder()
                            .taxServiceId(saved.getId())
                            .alias(aliasStr.trim())
                            .normalizedAlias(TaxServiceNormalizationUtils.normalize(aliasStr))
                            .isActive(true)
                            .build();
                    aliasRepository.save(alias);
                }
            }
        }

        auditService.logEvent("TAX_SERVICE_CREATED", "TAX_SERVICE", saved.getId().toString(), null,
                "Created master tax service: " + saved.getCode());

        return getTaxServiceById(saved.getId());
    }

    @Override
    @Transactional
    public TaxServiceDto updateTaxService(UUID id, UpdateTaxServiceRequest request) {
        TaxServiceEntity service = taxServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax Service", "id", id));

        if (request.getCategoryId() != null) {
            categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tax Service Category", "id", request.getCategoryId()));
            service.setCategoryId(request.getCategoryId());
        }

        if (StringUtils.hasText(request.getName())) {
            service.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            service.setDescription(request.getDescription());
        }
        if (request.getSortOrder() != null) {
            service.setSortOrder(request.getSortOrder());
        }
        if (request.getIsActive() != null) {
            service.setIsActive(request.getIsActive());
        }

        TaxServiceEntity saved = taxServiceRepository.save(service);
        auditService.logEvent("TAX_SERVICE_UPDATED", "TAX_SERVICE", saved.getId().toString(), null,
                "Updated master tax service: " + saved.getCode());

        return getTaxServiceById(saved.getId());
    }

    @Override
    @Transactional
    public TaxServiceDto toggleTaxServiceStatus(UUID id, boolean isActive) {
        TaxServiceEntity service = taxServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax Service", "id", id));

        service.setIsActive(isActive);
        TaxServiceEntity saved = taxServiceRepository.save(service);

        auditService.logEvent(isActive ? "TAX_SERVICE_ACTIVATED" : "TAX_SERVICE_DEACTIVATED",
                "TAX_SERVICE", saved.getId().toString(), null,
                (isActive ? "Activated" : "Deactivated") + " master tax service: " + saved.getCode());

        return getTaxServiceById(saved.getId());
    }

    // =========================================================================
    // Admin Alias Management
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<TaxServiceAliasDto> getAliasesForService(UUID taxServiceId) {
        List<TaxServiceAliasEntity> aliases = aliasRepository.findByTaxServiceId(taxServiceId);
        return mapper.toAliasDtoList(aliases);
    }

    @Override
    @Transactional
    public TaxServiceAliasDto addAlias(UUID taxServiceId, CreateTaxServiceAliasRequest request) {
        TaxServiceEntity service = taxServiceRepository.findById(taxServiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax Service", "id", taxServiceId));

        TaxServiceAliasEntity alias = TaxServiceAliasEntity.builder()
                .taxServiceId(service.getId())
                .alias(request.getAlias().trim())
                .normalizedAlias(TaxServiceNormalizationUtils.normalize(request.getAlias()))
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        TaxServiceAliasEntity saved = aliasRepository.save(alias);
        return mapper.toAliasDto(saved);
    }

    @Override
    @Transactional
    public void deleteAlias(UUID taxServiceId, UUID aliasId) {
        TaxServiceAliasEntity alias = aliasRepository.findById(aliasId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax Service Alias", "id", aliasId));

        if (!alias.getTaxServiceId().equals(taxServiceId)) {
            throw new ResourceNotFoundException("Tax Service Alias", "id", aliasId);
        }

        aliasRepository.delete(alias);
    }

    // =========================================================================
    // Public Discovery & Normalization
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<PublicTaxServiceCategoryDto> getPublicCategoriesWithServices() {
        List<TaxServiceCategoryEntity> categories = categoryRepository.findByIsActiveTrueOrderBySortOrderAsc();
        List<TaxServiceEntity> activeServices = taxServiceRepository.findAllActiveWithCategory();

        Map<UUID, List<TaxServiceEntity>> servicesByCategory = activeServices.stream()
                .collect(Collectors.groupingBy(TaxServiceEntity::getCategoryId));

        List<PublicTaxServiceCategoryDto> result = new ArrayList<>();
        for (TaxServiceCategoryEntity cat : categories) {
            PublicTaxServiceCategoryDto catDto = mapper.toPublicCategoryDto(cat);
            List<TaxServiceEntity> childServices = servicesByCategory.getOrDefault(cat.getId(), Collections.emptyList());
            catDto.setServices(mapper.toPublicTaxServiceDtoList(childServices));
            result.add(catDto);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicTaxServiceDto> getPublicActiveServices() {
        List<TaxServiceEntity> services = taxServiceRepository.findAllActiveWithCategory();
        return mapper.toPublicTaxServiceDtoList(services);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PublicTaxServiceDto> resolveQueryToService(String query) {
        if (!StringUtils.hasText(query)) {
            return Optional.empty();
        }

        String raw = query.trim();

        // 1. Direct code lookup (e.g. INCOME_TAX_RETURN)
        Optional<TaxServiceEntity> byCode = taxServiceRepository.findByCodeIgnoreCase(raw.toUpperCase(Locale.ROOT));
        if (byCode.isPresent() && Boolean.TRUE.equals(byCode.get().getIsActive())) {
            return Optional.of(mapper.toPublicTaxServiceDto(byCode.get()));
        }

        // 2. Exact normalized alias match (e.g. "itr" -> INCOME_TAX_RETURN)
        String normalized = TaxServiceNormalizationUtils.normalize(raw);
        List<TaxServiceAliasEntity> matchingAliases = aliasRepository.findActiveMatchingAliases(normalized);
        if (!matchingAliases.isEmpty()) {
            return Optional.of(mapper.toPublicTaxServiceDto(matchingAliases.get(0).getTaxService()));
        }

        // 3. Partial alias search
        List<TaxServiceAliasEntity> partialAliases = aliasRepository.searchActiveAliases(normalized);
        if (!partialAliases.isEmpty()) {
            return Optional.of(mapper.toPublicTaxServiceDto(partialAliases.get(0).getTaxService()));
        }

        // 4. Name or code search
        List<TaxServiceEntity> matchingServices = taxServiceRepository.searchActiveByNameOrCode(raw);
        if (!matchingServices.isEmpty()) {
            return Optional.of(mapper.toPublicTaxServiceDto(matchingServices.get(0)));
        }

        return Optional.empty();
    }

    // =========================================================================
    // Practice Service Selection
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<PracticeServiceDto> getMyPracticeServices() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        MarketplaceProfileEntity profile = profileRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Profile", "organizationId", organizationId));

        List<PracticeServiceEntity> services = practiceServiceRepository.findByMarketplaceProfileIdAndIsActiveTrue(profile.getId());
        return mapper.toPracticeServiceDtoList(services);
    }

    @Override
    @Transactional
    public List<PracticeServiceDto> updateMyPracticeServices(UpdatePracticeServicesRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        MarketplaceProfileEntity profile = profileRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Profile", "organizationId", organizationId));

        List<UUID> requestedIds = request.getTaxServiceIds() != null ? request.getTaxServiceIds() : Collections.emptyList();

        // 1. Validate that all requested services exist and are ACTIVE
        List<TaxServiceEntity> activeServices = taxServiceRepository.findByIdInAndIsActiveTrue(requestedIds);
        if (activeServices.size() != requestedIds.size()) {
            throw new BadRequestException("One or more selected services are inactive or do not exist in the master catalogue");
        }

        // 2. Fetch existing practice associations
        List<PracticeServiceEntity> existing = practiceServiceRepository.findByMarketplaceProfileId(profile.getId());
        Set<UUID> targetIds = new HashSet<>(requestedIds);

        Map<UUID, TaxServiceEntity> activeServiceMap = activeServices.stream()
                .collect(java.util.stream.Collectors.toMap(TaxServiceEntity::getId, s -> s));

        // Deactivate unselected ones, reactivate or create selected ones
        for (PracticeServiceEntity ps : existing) {
            if (targetIds.contains(ps.getTaxServiceId())) {
                ps.setIsActive(true);
                ps.setTaxService(activeServiceMap.get(ps.getTaxServiceId()));
                targetIds.remove(ps.getTaxServiceId());
            } else {
                ps.setIsActive(false);
            }
            practiceServiceRepository.save(ps);
        }

        // Insert new associations
        for (UUID newServiceId : targetIds) {
            PracticeServiceEntity newPs = PracticeServiceEntity.builder()
                    .organizationId(organizationId)
                    .marketplaceProfileId(profile.getId())
                    .taxServiceId(newServiceId)
                    .taxService(activeServiceMap.get(newServiceId))
                    .isActive(true)
                    .build();
            practiceServiceRepository.save(newPs);
        }

        auditService.logEvent("PRACTICE_SERVICES_UPDATED", "PRACTICE_SERVICE", profile.getId().toString(), null,
                "Updated practice service catalog to " + requestedIds.size() + " active services");

        return getMyPracticeServices();
    }

    @Override
    @Transactional
    public PracticeServiceDto addServiceToPractice(UUID taxServiceId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        MarketplaceProfileEntity profile = profileRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Profile", "organizationId", organizationId));

        TaxServiceEntity service = taxServiceRepository.findById(taxServiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax Service", "id", taxServiceId));

        if (!Boolean.TRUE.equals(service.getIsActive())) {
            throw new BadRequestException("Cannot select inactive service: " + service.getCode());
        }

        PracticeServiceEntity ps = practiceServiceRepository.findByMarketplaceProfileIdAndTaxServiceId(profile.getId(), taxServiceId)
                .orElseGet(() -> PracticeServiceEntity.builder()
                        .organizationId(organizationId)
                        .marketplaceProfileId(profile.getId())
                        .taxServiceId(taxServiceId)
                        .build());

        ps.setIsActive(true);
        ps.setTaxService(service);
        PracticeServiceEntity saved = practiceServiceRepository.save(ps);

        auditService.logEvent("PRACTICE_SERVICE_ADDED", "PRACTICE_SERVICE", saved.getId().toString(), null,
                "Added service " + service.getCode() + " to practice profile");

        return mapper.toPracticeServiceDto(saved);
    }

    @Override
    @Transactional
    public void removeServiceFromPractice(UUID taxServiceId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        MarketplaceProfileEntity profile = profileRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Profile", "organizationId", organizationId));

        practiceServiceRepository.findByMarketplaceProfileIdAndTaxServiceId(profile.getId(), taxServiceId)
                .ifPresent(ps -> {
                    ps.setIsActive(false);
                    practiceServiceRepository.save(ps);
                    auditService.logEvent("PRACTICE_SERVICE_REMOVED", "PRACTICE_SERVICE", ps.getId().toString(), null,
                            "Removed service " + taxServiceId + " from practice profile");
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicTaxServiceDto> getPublicPracticeOfferedServices(UUID marketplaceProfileId) {
        List<PracticeServiceEntity> services = practiceServiceRepository.findByMarketplaceProfileIdAndIsActiveTrue(marketplaceProfileId);
        return services.stream()
                .map(ps -> mapper.toPublicTaxServiceDto(ps.getTaxService()))
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }
}
