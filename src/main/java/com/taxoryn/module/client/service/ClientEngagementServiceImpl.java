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
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientServiceEntity;
import com.taxoryn.module.client.entity.ClientServiceStatus;
import com.taxoryn.module.client.entity.ClientServiceType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.repository.ClientServiceRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.service.ModuleConfigurationService;
import com.taxoryn.module.service.dto.ServiceDto;
import com.taxoryn.module.service.model.ServiceStatus;
import com.taxoryn.module.service.service.ServiceCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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
    @Lazy private final ServiceCatalogService serviceCatalogService;

    private static final Map<ClientServiceStatus, Set<ClientServiceStatus>> ALLOWED_TRANSITIONS = Map.of(
            ClientServiceStatus.PENDING, Set.of(ClientServiceStatus.ACTIVE, ClientServiceStatus.INACTIVE, ClientServiceStatus.ENDED, ClientServiceStatus.COMPLETED),
            ClientServiceStatus.ACTIVE, Set.of(ClientServiceStatus.INACTIVE, ClientServiceStatus.SUSPENDED, ClientServiceStatus.ENDED, ClientServiceStatus.COMPLETED),
            ClientServiceStatus.INACTIVE, Set.of(ClientServiceStatus.ACTIVE, ClientServiceStatus.ENDED, ClientServiceStatus.COMPLETED),
            ClientServiceStatus.SUSPENDED, Set.of(ClientServiceStatus.ACTIVE, ClientServiceStatus.INACTIVE, ClientServiceStatus.ENDED, ClientServiceStatus.COMPLETED),
            ClientServiceStatus.ENDED, Set.of(ClientServiceStatus.ACTIVE, ClientServiceStatus.PENDING),
            ClientServiceStatus.COMPLETED, Set.of(ClientServiceStatus.ACTIVE, ClientServiceStatus.PENDING)
    );

    @Override
    public boolean isTransitionAllowed(ClientServiceStatus from, ClientServiceStatus to) {
        if (from == null || to == null) {
            return false;
        }
        if (from == to) {
            return true;
        }
        return ALLOWED_TRANSITIONS.getOrDefault(from, Collections.emptySet()).contains(to);
    }

    @Override
    public Set<ClientServiceStatus> getAllowedTransitions(ClientServiceStatus currentStatus) {
        if (currentStatus == null) {
            return Collections.emptySet();
        }
        return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());
    }

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

        // 1. Client lifecycle validation
        if (client.getStatus() == ClientStatus.ARCHIVED || client.getStatus() == ClientStatus.SUSPENDED || client.getStatus() == ClientStatus.INACTIVE) {
            throw new BusinessValidationException("Cannot assign services to client with status " + client.getStatus());
        }

        UUID serviceOfferingId = request.getServiceOfferingId();
        ClientServiceType serviceType = request.getServiceType();
        BigDecimal agreedPrice = request.getAgreedPrice();

        // 2. Resolve Service Offering if provided
        if (serviceOfferingId != null) {
            ServiceDto offering = serviceCatalogService.findServiceById(serviceOfferingId)
                    .orElseThrow(() -> new ResourceNotFoundException("ServiceOffering", "id", serviceOfferingId));

            if (offering.getStatus() != ServiceStatus.ACTIVE) {
                throw new BusinessValidationException("Cannot assign inactive service offering: " + offering.getServiceName());
            }
            if (!offering.isAvailable()) {
                throw new BusinessValidationException("Service offering '" + offering.getServiceName() + "' is not available for this organization: " + offering.getAvailabilityReason());
            }
            if (StringUtils.hasText(offering.getModuleCode())) {
                try {
                    ProductModuleCode moduleCode = ProductModuleCode.valueOf(offering.getModuleCode().toUpperCase());
                    if (moduleCode != null && !moduleConfigurationService.isModuleEnabled(organizationId, moduleCode)) {
                        throw new BusinessValidationException("The module '" + offering.getModuleCode() + "' is disabled in your organization settings or not included in your subscription plan.");
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (serviceType == null) {
                serviceType = resolveClientServiceType(offering.getServiceCode());
            }

            if (agreedPrice == null) {
                agreedPrice = offering.getDefaultPrice();
            }

            // Prevent duplicate active relationship by offering ID
            if (clientServiceRepository.existsByOrganizationIdAndClientIdAndServiceOfferingIdAndStatus(
                    organizationId, clientId, serviceOfferingId, ClientServiceStatus.ACTIVE)) {
                throw new BusinessValidationException("An active engagement for service offering '" + offering.getServiceName() + "' already exists for this client");
            }
        }

        // 3. Fallback to Service Type direct validation
        if (serviceType == null) {
            throw new BusinessValidationException("Service offering ID or valid service type is required");
        }

        validateServiceEntitlement(organizationId, serviceType);

        if (clientServiceRepository.existsByOrganizationIdAndClientIdAndServiceTypeAndStatus(
                organizationId, clientId, serviceType, ClientServiceStatus.ACTIVE)) {
            throw new BusinessValidationException("An active engagement for service '" + serviceType.getDisplayName() + "' already exists for this client");
        }

        // Try to link serviceOfferingId if not already set
        if (serviceOfferingId == null && serviceCatalogService != null) {
            serviceCatalogService.findServiceByCode(serviceType.name()).ifPresent(offering -> {
                // optional backlink
            });
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

        Instant now = Instant.now();
        String reason = StringUtils.hasText(request.getReason()) ? request.getReason().trim() : "Initial service assignment";

        ClientServiceEntity entity = ClientServiceEntity.builder()
                .clientId(clientId)
                .serviceOfferingId(serviceOfferingId)
                .serviceType(serviceType)
                .status(ClientServiceStatus.ACTIVE)
                .agreedPrice(agreedPrice)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .assignedEmployeeId(request.getAssignedEmployeeId())
                .responsibleUserId(request.getResponsibleUserId())
                .locationId(request.getLocationId())
                .billingFrequency(request.getBillingFrequency() != null ? request.getBillingFrequency() : "MONTHLY")
                .notes(request.getNotes())
                .statusChangedAt(now)
                .statusChangedBy(userId)
                .statusChangeReason(reason)
                .build();
        entity.setOrganizationId(organizationId);

        ClientServiceEntity saved = clientServiceRepository.save(entity);

        auditService.logEvent(
                organizationId,
                userId,
                "SERVICE_ASSIGNED",
                "CLIENT_SERVICE",
                saved.getId().toString(),
                null,
                "Assigned " + serviceType.name() + " service to client " + client.getDisplayName()
        );

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

        ClientServiceStatus currentStatus = service.getStatus();
        ClientServiceStatus newStatus = request.getStatus();

        // 1. Status Transition Validation
        if (newStatus != null && newStatus != currentStatus) {
            if (!isTransitionAllowed(currentStatus, newStatus)) {
                throw new BusinessValidationException("Invalid client service status transition from " + currentStatus + " to " + newStatus);
            }

            // If activating, check entitlement and ensure no duplicate active service
            if (newStatus == ClientServiceStatus.ACTIVE) {
                validateServiceEntitlement(organizationId, service.getServiceType());
                if (service.getServiceOfferingId() != null) {
                    if (clientServiceRepository.existsByOrganizationIdAndClientIdAndServiceOfferingIdAndStatus(
                            organizationId, clientId, service.getServiceOfferingId(), ClientServiceStatus.ACTIVE)) {
                        throw new BusinessValidationException("Another active engagement for this service offering already exists for this client");
                    }
                } else {
                    if (clientServiceRepository.existsByOrganizationIdAndClientIdAndServiceTypeAndStatus(
                            organizationId, clientId, service.getServiceType(), ClientServiceStatus.ACTIVE)) {
                        throw new BusinessValidationException("Another active engagement for service '" + service.getServiceType().getDisplayName() + "' already exists for this client");
                    }
                }
            }

            service.setStatus(newStatus);
            service.setStatusChangedAt(Instant.now());
            service.setStatusChangedBy(userId);
            if (StringUtils.hasText(request.getReason())) {
                service.setStatusChangeReason(request.getReason().trim());
            }

            logStatusTransitionAuditEvent(organizationId, userId, service, currentStatus, newStatus, client.getDisplayName());
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

        if (request.getAgreedPrice() != null) {
            service.setAgreedPrice(request.getAgreedPrice());
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
        return updateClientServiceStatus(clientId, serviceId, status, null);
    }

    @Override
    @Transactional
    public ClientServiceDto updateClientServiceStatus(UUID clientId, UUID serviceId, ClientServiceStatus status, String reason) {
        UpdateClientServiceRequest req = UpdateClientServiceRequest.builder()
                .status(status)
                .reason(reason)
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

        if (!isTransitionAllowed(service.getStatus(), ClientServiceStatus.INACTIVE)) {
            throw new BusinessValidationException("Cannot deactivate client service from current status " + service.getStatus());
        }

        // Soft deactivation
        ClientServiceStatus oldStatus = service.getStatus();
        service.setStatus(ClientServiceStatus.INACTIVE);
        service.setStatusChangedAt(Instant.now());
        service.setStatusChangedBy(userId);
        service.setStatusChangeReason("Service engagement deactivated by user");
        clientServiceRepository.save(service);

        logStatusTransitionAuditEvent(organizationId, userId, service, oldStatus, ClientServiceStatus.INACTIVE, client.getDisplayName());
    }

    private void logStatusTransitionAuditEvent(UUID organizationId, UUID userId, ClientServiceEntity service,
                                               ClientServiceStatus oldStatus, ClientServiceStatus newStatus, String clientName) {
        String action;
        switch (newStatus) {
            case ACTIVE -> action = "SERVICE_ACTIVATED";
            case INACTIVE -> action = "SERVICE_DEACTIVATED";
            case ENDED, COMPLETED -> action = "SERVICE_ENDED";
            case SUSPENDED -> action = "SERVICE_SUSPENDED";
            default -> action = "CLIENT_SERVICE_STATUS_UPDATED";
        }

        auditService.logEvent(
                organizationId,
                userId,
                action,
                "CLIENT_SERVICE",
                service.getId().toString(),
                null,
                "Transitioned " + service.getServiceType().name() + " service for client " + clientName + " from " + oldStatus + " to " + newStatus
        );
    }

    private ClientServiceType resolveClientServiceType(String serviceCode) {
        if (serviceCode == null) {
            return ClientServiceType.OTHER;
        }
        try {
            return ClientServiceType.valueOf(serviceCode);
        } catch (IllegalArgumentException e) {
            return ClientServiceType.OTHER;
        }
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
                    .map(u -> StringUtils.hasText(u.getFullName()) ? u.getFullName() : u.getEmail())
                    .orElse(null);
        }

        String locationName = null;
        if (entity.getLocationId() != null) {
            locationName = locationRepository.findByIdAndOrganizationId(entity.getLocationId(), organizationId)
                    .map(com.taxoryn.module.organization.entity.LocationEntity::getName)
                    .orElse(null);
        }

        String serviceName = entity.getServiceType() != null ? entity.getServiceType().getDisplayName() : null;
        String category = entity.getServiceType() != null ? entity.getServiceType().getCategory() : null;
        String serviceCode = entity.getServiceType() != null ? entity.getServiceType().name() : null;

        if (entity.getServiceOfferingId() != null && serviceCatalogService != null) {
            serviceCatalogService.findServiceById(entity.getServiceOfferingId()).ifPresent(offering -> {
                // optional enrichment
            });
        }

        return ClientServiceDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .clientId(entity.getClientId())
                .clientName(clientName)
                .serviceOfferingId(entity.getServiceOfferingId())
                .serviceType(entity.getServiceType())
                .serviceCode(serviceCode)
                .serviceName(serviceName)
                .category(category)
                .status(entity.getStatus())
                .agreedPrice(entity.getAgreedPrice())
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
                .statusChangedAt(entity.getStatusChangedAt())
                .statusChangedBy(entity.getStatusChangedBy())
                .statusChangeReason(entity.getStatusChangeReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
