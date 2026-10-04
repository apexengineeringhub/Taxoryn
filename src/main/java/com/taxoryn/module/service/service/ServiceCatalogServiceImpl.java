package com.taxoryn.module.service.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.service.ModuleConfigurationService;
import com.taxoryn.module.service.dto.CreateServiceRequest;
import com.taxoryn.module.service.dto.ServiceDto;
import com.taxoryn.module.service.dto.UpdateServiceRequest;
import com.taxoryn.module.service.entity.ServiceEntity;
import com.taxoryn.module.service.model.ServiceCategory;
import com.taxoryn.module.service.model.ServiceScope;
import com.taxoryn.module.service.model.ServiceStatus;
import com.taxoryn.module.service.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceCatalogServiceImpl implements ServiceCatalogService {

    private final ServiceRepository serviceRepository;
    private final ModuleConfigurationService moduleConfigurationService;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public List<ServiceDto> getServices() {
        return getServices(null, null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceDto> getServices(String search, ServiceCategory category, ServiceScope scope, Boolean activeOnly) {
        UUID organizationId = resolveOrganizationId();
        List<ServiceEntity> services = Boolean.TRUE.equals(activeOnly)
                ? serviceRepository.findActiveServicesForTenant(organizationId)
                : serviceRepository.findAllAccessibleServicesForTenant(organizationId);

        return services.stream()
                .filter(s -> {
                    if (search != null && !search.isBlank()) {
                        String q = search.trim().toLowerCase();
                        boolean matchName = s.getServiceName() != null && s.getServiceName().toLowerCase().contains(q);
                        boolean matchCode = s.getServiceCode() != null && s.getServiceCode().toLowerCase().contains(q);
                        boolean matchDesc = s.getDescription() != null && s.getDescription().toLowerCase().contains(q);
                        if (!matchName && !matchCode && !matchDesc) return false;
                    }
                    if (category != null && s.getCategory() != category) {
                        return false;
                    }
                    if (scope != null) {
                        ServiceScope effectiveScope = s.getScope() != null ? s.getScope() : (s.getOrganizationId() == null ? ServiceScope.TAXORYN : ServiceScope.PRACTICE);
                        if (effectiveScope != scope) return false;
                    }
                    return true;
                })
                .map(s -> mapToDto(s, organizationId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceDto getServiceById(UUID id) {
        UUID organizationId = resolveOrganizationId();
        ServiceEntity service = serviceRepository.findAccessibleServiceById(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", id));

        return mapToDto(service, organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceDto getServiceByCode(String serviceCode) {
        UUID organizationId = resolveOrganizationId();
        ServiceEntity service = serviceRepository.findAccessibleServiceByCode(serviceCode, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "serviceCode", serviceCode));

        return mapToDto(service, organizationId);
    }

    @Override
    @Transactional
    public ServiceDto createService(CreateServiceRequest request) {
        UUID organizationId = resolveOrganizationId();
        if (organizationId == null) {
            throw new BadRequestException("Organization context is required to create a custom practice service offering.");
        }
        String code = request.getServiceCode().trim().toUpperCase();

        if (serviceRepository.existsByOrganizationIdAndServiceCodeIgnoreCase(organizationId, code)) {
            throw new DuplicateResourceException("Service", "serviceCode", code);
        }

        BigDecimal defaultPrice = request.getDefaultPrice() != null ? request.getDefaultPrice() : BigDecimal.ZERO;
        String billingUnit = StringUtils.hasText(request.getBillingUnit()) ? request.getBillingUnit().trim() : "PER_APPLICATION";
        BigDecimal taxRate = request.getTaxRate() != null ? request.getTaxRate() : new BigDecimal("18.00");

        ServiceEntity service = ServiceEntity.builder()
                .organizationId(organizationId)
                .scope(ServiceScope.PRACTICE)
                .serviceCode(code)
                .serviceName(request.getServiceName().trim())
                .description(request.getDescription())
                .category(request.getCategory())
                .defaultPrice(defaultPrice)
                .billingUnit(billingUnit)
                .taxRate(taxRate)
                .status(ServiceStatus.ACTIVE)
                .configurable(request.isConfigurable())
                .moduleCode(request.getModuleCode())
                .build();

        service = serviceRepository.save(service);
        log.info("Created custom practice service: id={}, code={}, tenant={}", service.getId(), code, organizationId);

        auditService.logEvent("SERVICE_CREATED", "SERVICE", service.getId().toString(), null, service);

        return mapToDto(service, organizationId);
    }

    @Override
    @Transactional
    public ServiceDto updateService(UUID id, UpdateServiceRequest request) {
        UUID organizationId = resolveOrganizationId();
        ServiceEntity service = serviceRepository.findAccessibleServiceById(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", id));

        // System global services cannot be modified directly by practice users
        if (service.getOrganizationId() == null && !SecurityUtils.isTaxorynSuperAdmin()) {
            throw new BusinessValidationException("Cannot modify platform default Taxoryn service. You may configure it under practice service pricing.");
        }

        if (StringUtils.hasText(request.getServiceName())) {
            service.setServiceName(request.getServiceName().trim());
        }
        if (request.getDescription() != null) {
            service.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            service.setCategory(request.getCategory());
        }
        if (request.getDefaultPrice() != null) {
            service.setDefaultPrice(request.getDefaultPrice());
        }
        if (StringUtils.hasText(request.getBillingUnit())) {
            service.setBillingUnit(request.getBillingUnit().trim());
        }
        if (request.getTaxRate() != null) {
            service.setTaxRate(request.getTaxRate());
        }
        if (request.getStatus() != null) {
            service.setStatus(request.getStatus());
        }
        if (request.getModuleCode() != null) {
            service.setModuleCode(request.getModuleCode());
        }
        if (request.getConfigurable() != null) {
            service.setConfigurable(request.getConfigurable());
        }

        service = serviceRepository.save(service);
        log.info("Updated service: id={}, code={}", service.getId(), service.getServiceCode());

        auditService.logEvent("SERVICE_UPDATED", "SERVICE", service.getId().toString(), null, service);

        return mapToDto(service, organizationId);
    }

    @Override
    @Transactional
    public ServiceDto toggleServiceStatus(UUID id, boolean active) {
        UUID organizationId = resolveOrganizationId();
        ServiceEntity service = serviceRepository.findAccessibleServiceById(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", id));

        if (service.getOrganizationId() == null && !SecurityUtils.isTaxorynSuperAdmin()) {
            throw new BusinessValidationException("Platform standard services status must be configured via practice service pricing.");
        }

        ServiceStatus newStatus = active ? ServiceStatus.ACTIVE : ServiceStatus.INACTIVE;
        service.setStatus(newStatus);
        service = serviceRepository.save(service);

        String event = active ? "SERVICE_ACTIVATED" : "SERVICE_DEACTIVATED";
        auditService.logEvent(event, "SERVICE", service.getId().toString(), null, service);

        return mapToDto(service, organizationId);
    }

    @Override
    @Transactional
    public void deleteService(UUID id) {
        UUID organizationId = resolveOrganizationId();
        ServiceEntity service = serviceRepository.findAccessibleServiceById(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", id));

        if (service.getOrganizationId() == null) {
            throw new BusinessValidationException("Cannot delete platform default Taxoryn service.");
        }

        if (organizationId == null || !organizationId.equals(service.getOrganizationId())) {
            throw new BusinessValidationException("Access denied: You cannot delete another practice's service offering.");
        }

        serviceRepository.delete(service);
        auditService.logEvent("SERVICE_DELETED", "SERVICE", id.toString(), null, null);
    }

    @Override
    public boolean isServiceAvailableForPractice(UUID organizationId, ServiceEntity service) {
        if (service == null || service.getStatus() != ServiceStatus.ACTIVE) {
            return false;
        }

        // Practice custom service is always available to that practice if ACTIVE
        if (service.getOrganizationId() != null) {
            return true;
        }

        if (!StringUtils.hasText(service.getModuleCode())) {
            return true;
        }

        try {
            ProductModuleCode moduleCode = ProductModuleCode.valueOf(service.getModuleCode().toUpperCase());
            return organizationId == null || moduleConfigurationService.isModuleEnabled(organizationId, moduleCode);
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private ServiceDto mapToDto(ServiceEntity entity, UUID organizationId) {
        boolean available = isServiceAvailableForPractice(organizationId, entity);
        String reason = available ? "Service is available and enabled." : "Service requires subscription upgrade or module activation.";
        ServiceScope scope = entity.getScope() != null ? entity.getScope() : (entity.getOrganizationId() == null ? ServiceScope.TAXORYN : ServiceScope.PRACTICE);

        return ServiceDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .scope(scope)
                .serviceCode(entity.getServiceCode())
                .serviceName(entity.getServiceName())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .defaultPrice(entity.getDefaultPrice())
                .billingUnit(entity.getBillingUnit() != null ? entity.getBillingUnit() : "PER_RETURN")
                .taxRate(entity.getTaxRate() != null ? entity.getTaxRate() : new BigDecimal("18.00"))
                .status(entity.getStatus())
                .configurable(entity.isConfigurable())
                .moduleCode(entity.getModuleCode())
                .isGlobal(entity.getOrganizationId() == null)
                .isAvailable(available)
                .availabilityReason(reason)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .version(entity.getVersion())
                .build();
    }

    private UUID resolveOrganizationId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        return SecurityUtils.getCurrentOrganizationId();
    }
}
