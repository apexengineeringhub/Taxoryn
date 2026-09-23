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
    private final ModuleConfigurationService moduleConfigurationService;
    private final AuditService auditService;
    private final PracticeSecurityScopeEvaluator securityScopeEvaluator;

    @Override
    @Transactional(readOnly = true)
    public List<ServiceCatalogItemDto> getServiceCatalog() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        Map<ProductModuleCode, OrganizationModuleDto> moduleMap = Map.of();
        if (organizationId != null) {
            try {
                List<OrganizationModuleDto> orgModules = moduleConfigurationService.getOrganizationModules(organizationId);
                moduleMap = orgModules.stream()
                        .collect(Collectors.toMap(OrganizationModuleDto::getModuleCode, m -> m, (a, b) -> a));
            } catch (Exception e) {
                log.warn("Could not load organization modules for catalog evaluation: {}", e.getMessage());
            }
        }

        List<ServiceCatalogItemDto> catalog = new ArrayList<>();
        for (ClientServiceType type : ClientServiceType.values()) {
            boolean moduleEnabled = true;
            boolean subscriptionEntitled = true;
            String accessStatus = "AVAILABLE";

            if (type.getAssociatedModule() != null) {
                OrganizationModuleDto module = moduleMap.get(type.getAssociatedModule());
                if (module != null) {
                    moduleEnabled = module.isEnabled();
                    subscriptionEntitled = module.isEntitled();
                    if (!module.isEnabled()) {
                        accessStatus = "MODULE_DISABLED";
                    } else if (!subscriptionEntitled) {
                        accessStatus = module.getSubscriptionStatus() != null &&
                                !List.of("ACTIVE", "TRIALING").contains(module.getSubscriptionStatus()) ?
                                "SUBSCRIPTION_REQUIRED" : "UPGRADE_REQUIRED";
                    } else {
                        accessStatus = "AVAILABLE";
                    }
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

        Map<UUID, String> employeeNameMap = loadEmployeeNames(organizationId);

        return entities.stream()
                .map(entity -> mapToDto(entity, client.getDisplayName(), employeeNameMap))
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

        Map<UUID, String> employeeNameMap = loadEmployeeNames(organizationId);
        return mapToDto(service, client.getDisplayName(), employeeNameMap);
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

        // 5. Validate Dates
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

        Map<UUID, String> employeeNameMap = loadEmployeeNames(organizationId);
        return mapToDto(saved, client.getDisplayName(), employeeNameMap);
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

        Map<UUID, String> employeeNameMap = loadEmployeeNames(organizationId);
        return mapToDto(saved, client.getDisplayName(), employeeNameMap);
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

        try {
            List<OrganizationModuleDto> orgModules = moduleConfigurationService.getOrganizationModules(organizationId);
            Optional<OrganizationModuleDto> moduleOpt = orgModules.stream()
                    .filter(m -> m.getModuleCode() == serviceType.getAssociatedModule())
                    .findFirst();

            if (moduleOpt.isPresent()) {
                OrganizationModuleDto module = moduleOpt.get();
                if (!module.isEnabled()) {
                    throw new BusinessValidationException("The module '" + serviceType.getAssociatedModule() + "' is disabled in your organization settings.");
                }
                if (!module.isEntitled()) {
                    throw new ForbiddenException("Your current subscription plan is not entitled to use the '" + serviceType.getAssociatedModule() + "' module.");
                }
            }
        } catch (BusinessValidationException | ForbiddenException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Could not verify module entitlement for service {}: {}", serviceType, e.getMessage());
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

    private ClientServiceDto mapToDto(ClientServiceEntity entity, String clientName, Map<UUID, String> employeeNameMap) {
        String employeeName = entity.getAssignedEmployeeId() != null ?
                employeeNameMap.getOrDefault(entity.getAssignedEmployeeId(), "Unassigned") : null;

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
                .billingFrequency(entity.getBillingFrequency())
                .notes(entity.getNotes())
                .routePath(entity.getServiceType() != null ? entity.getServiceType().getRoutePath() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
