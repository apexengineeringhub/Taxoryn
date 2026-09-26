package com.taxoryn.module.client.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.dto.ClientServiceDto;
import com.taxoryn.module.client.dto.CreateClientServiceRequest;
import com.taxoryn.module.client.dto.ServiceCatalogItemDto;
import com.taxoryn.module.client.dto.UpdateClientServiceRequest;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientServiceEntity;
import com.taxoryn.module.client.entity.ClientServiceStatus;
import com.taxoryn.module.client.entity.ClientServiceType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.repository.ClientServiceRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.moduleconfig.dto.OrganizationModuleDto;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.service.ModuleConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientEngagementServiceImpl implements ClientEngagementService {

    private final ClientRepository clientRepository;
    private final ClientServiceRepository clientServiceRepository;
    private final EmployeeRepository employeeRepository;
    private final com.taxoryn.module.user.repository.UserRepository userRepository;
    private final com.taxoryn.module.organization.repository.LocationRepository locationRepository;
    private final ModuleConfigurationService moduleConfigurationService;
    private final AuditService auditService;
    private final PracticeSecurityScopeEvaluator securityScopeEvaluator;

    @Override
    @Transactional(readOnly = true)
    public List<ServiceCatalogItemDto> getServiceCatalog() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        List<ServiceCatalogItemDto> catalog = new ArrayList<>();
        for (ClientServiceType type : ClientServiceType.values()) {
            boolean moduleEnabled = true;
            boolean subscriptionEntitled = true;
            String accessStatus = "AVAILABLE";

            if (type.getAssociatedModule() != null && organizationId != null) {
                moduleEnabled = moduleConfigurationService.isModuleEnabled(organizationId, type.getAssociatedModule());
                if (!moduleEnabled) {
                    accessStatus = "MODULE_DISABLED";
                }
            }

            catalog.add(ServiceCatalogItemDto.builder()
                    .serviceType(type)
                    .code(type.name())
                    .displayName(type.getDisplayName())
                    .description(type.getDescription())
                    .category(type.getCategory())
                    .associatedModule(type.getAssociatedModule())
                    .associatedCapability(type.getAssociatedCapability())
                    .routePath(type.getRoutePath())
                    .active(true)
                    .moduleEnabled(moduleEnabled)
                    .subscriptionEntitled(subscriptionEntitled)
                    .accessStatus(accessStatus)
                    .build());
        }

        return catalog;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientServiceDto> getClientServices(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null) {
            throw new UnauthorizedException("Authenticated organization context is required");
        }

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateClientAccess(client);

        List<ClientServiceEntity> entities = clientServiceRepository
                .findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(organizationId, clientId);

        return entities.stream()
                .map(entity -> mapToDto(entity, client.getDisplayName(), organizationId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClientServiceDto getClientServiceById(UUID clientId, UUID serviceId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null) {
            throw new UnauthorizedException("Authenticated organization context is required");
        }

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateClientAccess(client);

        ClientServiceEntity service = clientServiceRepository.findByIdAndOrganizationIdAndClientId(serviceId, organizationId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientService", "id", serviceId));

        return mapToDto(service, client.getDisplayName(), organizationId);
    }

    @Override
    @Transactional
    public ClientServiceDto createClientService(UUID clientId, CreateClientServiceRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null) {
            throw new UnauthorizedException("Authenticated organization context is required");
        }
        UUID userId = SecurityUtils.getCurrentUserId();

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateClientAccess(client);

        // 1. Validate Service Type
        ClientServiceType serviceType = request.getServiceType();
        if (serviceType == null) {
            throw new BusinessValidationException("Service type is required");
        }

        // 2. Validate Entitlement
        validateServiceEntitlement(organizationId, serviceType);

        // 3. Prevent duplicate active service
        if (clientServiceRepository.existsByOrganizationIdAndClientIdAndServiceTypeAndStatus(
                organizationId, clientId, serviceType, ClientServiceStatus.ACTIVE)) {
            throw new BusinessValidationException("An active engagement for service '" + serviceType.getDisplayName() + "' already exists for this client");
        }

        // 4. Validate Assigned Employee if provided
        if (request.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getAssignedEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned Employee", "id", request.getAssignedEmployeeId()));
        }

        // 5. Validate Responsible User if provided
        if (request.getResponsibleUserId() != null) {
            userRepository.findByIdAndOrganizationId(request.getResponsibleUserId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Responsible User", "id", request.getResponsibleUserId()));
        }

        // 6. Validate Location if provided
        if (request.getLocationId() != null) {
            locationRepository.findByIdAndOrganizationId(request.getLocationId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Location", "id", request.getLocationId()));
        }

        // 7. Validate Dates
        if (request.getStartDate() != null && request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessValidationException("Service end date cannot be before start date");
        }

        ClientServiceEntity entity = ClientServiceEntity.builder()
                .clientId(clientId)
                .serviceType(serviceType)
                .status(ClientServiceStatus.ACTIVE)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .assignedEmployeeId(request.getAssignedEmployeeId())
                .responsibleUserId(request.getResponsibleUserId())
                .locationId(request.getLocationId())
                .billingFrequency(request.getBillingFrequency() != null ? request.getBillingFrequency() : "MONTHLY")
                .notes(request.getNotes())
                .build();
        entity.setOrganizationId(organizationId);

        ClientServiceEntity saved = clientServiceRepository.save(entity);

        auditService.logEvent(
                organizationId,
                userId,
                "CLIENT_SERVICE_CREATED",
                "CLIENT_SERVICE",
                saved.getId().toString(),
                null,
                "Created " + serviceType.name() + " service engagement for client " + client.getDisplayName()
        );

        return mapToDto(saved, client.getDisplayName(), organizationId);
    }

    @Override
    @Transactional
    public ClientServiceDto updateClientService(UUID clientId, UUID serviceId, UpdateClientServiceRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null) {
            throw new UnauthorizedException("Authenticated organization context is required");
        }
        UUID userId = SecurityUtils.getCurrentUserId();

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateClientAccess(client);

        ClientServiceEntity service = clientServiceRepository.findByIdAndOrganizationIdAndClientId(serviceId, organizationId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientService", "id", serviceId));

        // 1. If activating, check entitlement and ensure no duplicate active service of same type
        if (request.getStatus() == ClientServiceStatus.ACTIVE && service.getStatus() != ClientServiceStatus.ACTIVE) {
            validateServiceEntitlement(organizationId, service.getServiceType());
            if (clientServiceRepository.existsByOrganizationIdAndClientIdAndServiceTypeAndStatus(
                    organizationId, clientId, service.getServiceType(), ClientServiceStatus.ACTIVE)) {
                throw new BusinessValidationException("Another active engagement for service '" + service.getServiceType().getDisplayName() + "' already exists for this client");
            }
        }

        // 2. Validate Assigned Employee
        if (request.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getAssignedEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned Employee", "id", request.getAssignedEmployeeId()));
            service.setAssignedEmployeeId(request.getAssignedEmployeeId());
        }

        // 3. Validate Responsible User
        if (request.getResponsibleUserId() != null) {
            userRepository.findByIdAndOrganizationId(request.getResponsibleUserId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Responsible User", "id", request.getResponsibleUserId()));
            service.setResponsibleUserId(request.getResponsibleUserId());
        }

        // 4. Validate Location
        if (request.getLocationId() != null) {
            locationRepository.findByIdAndOrganizationId(request.getLocationId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Location", "id", request.getLocationId()));
            service.setLocationId(request.getLocationId());
        }

        if (request.getStatus() != null) {
            service.setStatus(request.getStatus());
        }
        if (request.getStartDate() != null) {
            service.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            service.setEndDate(request.getEndDate());
        }
        if (request.getBillingFrequency() != null) {
            service.setBillingFrequency(request.getBillingFrequency());
        }
        if (request.getNotes() != null) {
            service.setNotes(request.getNotes());
        }

        // Validate Dates
        if (service.getStartDate() != null && service.getEndDate() != null && service.getEndDate().isBefore(service.getStartDate())) {
            throw new BusinessValidationException("Service end date cannot be before start date");
        }

        ClientServiceEntity saved = clientServiceRepository.save(service);

        auditService.logEvent(
                organizationId,
                userId,
                "CLIENT_SERVICE_UPDATED",
                "CLIENT_SERVICE",
                saved.getId().toString(),
                null,
                "Updated " + service.getServiceType().name() + " service engagement for client " + client.getDisplayName()
        );

        return mapToDto(saved, client.getDisplayName(), organizationId);
    }

    @Override
    @Transactional
    public ClientServiceDto updateClientServiceStatus(UUID clientId, UUID serviceId, ClientServiceStatus status) {
        UpdateClientServiceRequest req = UpdateClientServiceRequest.builder()
                .status(status)
                .build();
        return updateClientService(clientId, serviceId, req);
    }

    @Override
    @Transactional
    public void deleteClientService(UUID clientId, UUID serviceId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null) {
            throw new UnauthorizedException("Authenticated organization context is required");
        }
        UUID userId = SecurityUtils.getCurrentUserId();

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateClientAccess(client);

        ClientServiceEntity service = clientServiceRepository.findByIdAndOrganizationIdAndClientId(serviceId, organizationId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientService", "id", serviceId));

        // Soft deactivation
        service.setStatus(ClientServiceStatus.INACTIVE);
        clientServiceRepository.save(service);

        auditService.logEvent(
                organizationId,
                userId,
                "CLIENT_SERVICE_DEACTIVATED",
                "CLIENT_SERVICE",
                service.getId().toString(),
                null,
                "Deactivated " + service.getServiceType().name() + " service for client " + client.getDisplayName()
        );
    }

    private void validateServiceEntitlement(UUID organizationId, ClientServiceType serviceType) {
        if (serviceType.getAssociatedModule() == null) {
            return;
        }

        ProductModuleCode moduleCode = serviceType.getAssociatedModule();
        if (!moduleConfigurationService.isModuleEnabled(organizationId, moduleCode)) {
            throw new BusinessValidationException("The module '" + moduleCode.name() + "' is disabled in your organization settings or not included in your subscription plan.");
        }
    }

    private void validateClientAccess(ClientEntity client) {
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        if (!scope.isFirmAdmin()) {
            Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);
            boolean isAssigned = (client.getAssignedEmployeeId() != null && scope.getAccessibleAssigneeIds() != null && scope.getAccessibleAssigneeIds().contains(client.getAssignedEmployeeId()));
            if (!isAssigned && (accessibleClientIds == null || !accessibleClientIds.contains(client.getId()))) {
                throw new AccessDeniedException("Access denied: You do not have permission to access clients outside your assigned department or portfolio.");
            }
        }
    }

    private Map<UUID, String> loadEmployeeNames(UUID organizationId) {
        return employeeRepository.findAllByOrganizationId(organizationId).stream()
                .collect(Collectors.toMap(
                        EmployeeEntity::getId,
                        EmployeeEntity::getFullName,
                        (a, b) -> a
                ));
    }

    private ClientServiceDto mapToDto(ClientServiceEntity entity, String clientName, UUID organizationId) {
        String employeeName = null;
        if (entity.getAssignedEmployeeId() != null) {
            employeeName = employeeRepository.findByIdAndOrganizationId(entity.getAssignedEmployeeId(), organizationId)
                    .map(com.taxoryn.module.employee.entity.EmployeeEntity::getFullName)
                    .orElse(null);
        }

        String responsibleUserName = null;
        if (entity.getResponsibleUserId() != null) {
            responsibleUserName = userRepository.findByIdAndOrganizationId(entity.getResponsibleUserId(), organizationId)
                    .map(u -> org.springframework.util.StringUtils.hasText(u.getFullName()) ? u.getFullName() : u.getEmail())
                    .orElse(null);
        }

        String locationName = null;
        if (entity.getLocationId() != null) {
            locationName = locationRepository.findByIdAndOrganizationId(entity.getLocationId(), organizationId)
                    .map(com.taxoryn.module.organization.entity.LocationEntity::getName)
                    .orElse(null);
        }

        return ClientServiceDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .clientId(entity.getClientId())
                .clientName(clientName)
                .serviceType(entity.getServiceType())
                .serviceName(entity.getServiceType() != null ? entity.getServiceType().getDisplayName() : null)
                .category(entity.getServiceType() != null ? entity.getServiceType().getCategory() : null)
                .status(entity.getStatus())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .assignedEmployeeId(entity.getAssignedEmployeeId())
                .assignedEmployeeName(employeeName)
                .responsibleUserId(entity.getResponsibleUserId())
                .responsibleUserName(responsibleUserName)
                .locationId(entity.getLocationId())
                .locationName(locationName)
                .billingFrequency(entity.getBillingFrequency())
                .notes(entity.getNotes())
                .routePath(entity.getServiceType() != null ? entity.getServiceType().getRoutePath() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
