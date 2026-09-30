package com.taxoryn.module.service.service;

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
import com.taxoryn.module.service.model.ServiceStatus;
import com.taxoryn.module.service.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
        UUID organizationId = resolveOrganizationId();
        List<ServiceEntity> services = serviceRepository.findActiveServicesForTenant(organizationId);

        return services.stream()
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
        String code = request.getServiceCode().trim().toUpperCase();

        if (serviceRepository.existsByOrganizationIdAndServiceCode(organizationId, code)) {
            throw new DuplicateResourceException("Service", "serviceCode", code);
        }

        ServiceEntity service = ServiceEntity.builder()
                .organizationId(organizationId)
                .serviceCode(code)
                .serviceName(request.getServiceName().trim())
                .description(request.getDescription())
                .category(request.getCategory())
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

        // System global services cannot be modified directly by tenants
        if (service.getOrganizationId() == null && !SecurityUtils.isTaxorynSuperAdmin()) {
            throw new BusinessValidationException("Cannot modify platform default system service. You may configure it at practice module settings.");
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
    public boolean isServiceAvailableForPractice(UUID organizationId, ServiceEntity service) {
        if (service == null || service.getStatus() != ServiceStatus.ACTIVE) {
            return false;
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

        return ServiceDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .serviceCode(entity.getServiceCode())
                .serviceName(entity.getServiceName())
                .description(entity.getDescription())
                .category(entity.getCategory())
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
