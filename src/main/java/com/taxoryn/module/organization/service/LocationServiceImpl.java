package com.taxoryn.module.organization.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.SubscriptionLimitExceededException;
import com.taxoryn.core.exception.TenantAccessDeniedException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.organization.dto.CreateLocationRequest;
import com.taxoryn.module.organization.dto.LocationDto;
import com.taxoryn.module.organization.dto.UpdateLocationRequest;
import com.taxoryn.module.organization.entity.EmployeeLocationEntity;
import com.taxoryn.module.organization.entity.EmployeeLocationEntity.EmployeeLocationId;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.repository.EmployeeLocationRepository;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.subscription.entity.SubscriptionEntity;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;
import com.taxoryn.module.subscription.repository.SubscriptionRepository;
import com.taxoryn.module.subscription.service.SubscriptionPlanEntitlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final EmployeeLocationRepository employeeLocationRepository;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanEntitlementService subscriptionPlanEntitlementService;
    private final AuditService auditService;
    private final com.taxoryn.core.security.PracticeSecurityScopeEvaluator securityScopeEvaluator;

    @Override
    @Transactional
    public LocationDto createLocation(UUID organizationId, CreateLocationRequest request) {
        validateTenantAccess(organizationId);

        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization", "id", organizationId);
        }

        // 1. Subscription Plan Limit Enforcement
        SubscriptionEntity subscription = subscriptionRepository.findByOrganizationId(organizationId).orElse(null);
        SubscriptionPlan plan = subscription != null ? subscription.getPlan() : SubscriptionPlan.STARTER;

        long currentActiveLocations = locationRepository.countByOrganizationIdAndIsActiveTrue(organizationId);

        if (currentActiveLocations >= 1) {
            boolean multiLocationAllowed = subscriptionPlanEntitlementService.isMultiLocationEnabled(plan);
            if (!multiLocationAllowed) {
                log.warn("Multi-location creation blocked: Org {} is on plan {} with multi-location disabled", organizationId, plan);
                throw new SubscriptionLimitExceededException("Multi-location management is not enabled for your subscription plan (" +
                        plan.name() + "). Please upgrade to Professional, Business, or Enterprise tier.");
            }

            int maxLocations = subscriptionPlanEntitlementService.getMaxLocations(plan);
            if (currentActiveLocations >= maxLocations) {
                log.warn("Location limit exceeded: Org {} has {} active locations, limit is {}",
                        organizationId, currentActiveLocations, maxLocations);
                throw new SubscriptionLimitExceededException("Location limit reached (" + maxLocations +
                        " max locations for " + plan.name() + " plan). Please upgrade your subscription.");
            }
        }

        // If this is marked as head office, unset existing head office
        if (request.isHeadOffice()) {
            unsetHeadOffice(organizationId);
        }

        LocationEntity location = LocationEntity.builder()
                .name(request.getName())
                .code(request.getCode())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .phone(request.getPhone())
                .email(request.getEmail())
                .isHeadOffice(request.isHeadOffice() || currentActiveLocations == 0) // First location is default head office
                .isActive(true)
                .build();
        location.setOrganizationId(organizationId);

        LocationEntity saved = locationRepository.save(location);

        // Assign employees if provided
        if (request.getAssignedEmployeeIds() != null && !request.getAssignedEmployeeIds().isEmpty()) {
            for (UUID empId : request.getAssignedEmployeeIds()) {
                employeeLocationRepository.save(new EmployeeLocationEntity(new EmployeeLocationId(empId, saved.getId())));
            }
        }

        auditService.logEvent(
                organizationId,
                SecurityUtils.getCurrentUserId(),
                "LOCATION_CREATED",
                "LOCATION",
                saved.getId().toString(),
                null,
                Map.of("name", saved.getName(), "city", saved.getCity(), "isHeadOffice", saved.isHeadOffice())
        );

        log.info("Practice location created: orgId={}, locationId={}, name={}", organizationId, saved.getId(), saved.getName());
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public LocationDto initializeDefaultHeadOffice(UUID organizationId, String organizationName, String addressLine1, String city, String state, String pincode) {
        if (locationRepository.countByOrganizationIdAndIsActiveTrue(organizationId) > 0) {
            return locationRepository.findAllByOrganizationIdAndIsActiveTrue(organizationId).stream()
                    .filter(LocationEntity::isHeadOffice)
                    .findFirst()
                    .map(this::mapToDto)
                    .orElseGet(() -> mapToDto(locationRepository.findAllByOrganizationIdAndIsActiveTrue(organizationId).get(0)));
        }

        LocationEntity location = LocationEntity.builder()
                .name(organizationName != null && !organizationName.isBlank() ? organizationName + " (Head Office)" : "Head Office")
                .code("HO-01")
                .addressLine1(addressLine1 != null ? addressLine1 : "Primary Office Address")
                .city(city != null && !city.isBlank() ? city : "Mumbai")
                .state(state != null && !state.isBlank() ? state : "Maharashtra")
                .pincode(pincode != null ? pincode : "400001")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        location.setOrganizationId(organizationId);

        LocationEntity saved = locationRepository.save(location);
        log.info("Initialized default Head Office location for organization: orgId={}, locationId={}", organizationId, saved.getId());
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationDto> getLocations(UUID organizationId) {
        validateTenantAccess(organizationId);
        return locationRepository.findAllByOrganizationId(organizationId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LocationDto getLocationById(UUID organizationId, UUID locationId) {
        validateTenantAccess(organizationId);
        LocationEntity location = locationRepository.findByIdAndOrganizationId(locationId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", locationId));

        if (securityScopeEvaluator != null) {
            com.taxoryn.core.security.PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
            if (scope != null && !scope.isFirmAdmin()) {
                if (!scope.canAccessLocation(locationId)) {
                    throw new TenantAccessDeniedException("Access denied: Location " + locationId + " is not in your allowed location scope");
                }
            }
        }

        return mapToDto(location);
    }

    @Override
    @Transactional
    public LocationDto updateLocation(UUID organizationId, UUID locationId, UpdateLocationRequest request) {
        validateTenantAccess(organizationId);

        LocationEntity location = locationRepository.findByIdAndOrganizationId(locationId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", locationId));

        if (request.getName() != null) location.setName(request.getName());
        if (request.getCode() != null) location.setCode(request.getCode());
        if (request.getAddressLine1() != null) location.setAddressLine1(request.getAddressLine1());
        if (request.getAddressLine2() != null) location.setAddressLine2(request.getAddressLine2());
        if (request.getCity() != null) location.setCity(request.getCity());
        if (request.getState() != null) location.setState(request.getState());
        if (request.getPincode() != null) location.setPincode(request.getPincode());
        if (request.getPhone() != null) location.setPhone(request.getPhone());
        if (request.getEmail() != null) location.setEmail(request.getEmail());

        if (Boolean.TRUE.equals(request.getIsHeadOffice()) && !location.isHeadOffice()) {
            unsetHeadOffice(organizationId);
            location.setHeadOffice(true);
        } else if (request.getIsHeadOffice() != null) {
            location.setHeadOffice(request.getIsHeadOffice());
        }

        if (request.getIsActive() != null) {
            location.setActive(request.getIsActive());
        }

        LocationEntity saved = locationRepository.save(location);

        if (request.getAssignedEmployeeIds() != null) {
            employeeLocationRepository.deleteByLocationId(saved.getId());
            for (UUID empId : request.getAssignedEmployeeIds()) {
                employeeLocationRepository.save(new EmployeeLocationEntity(new EmployeeLocationId(empId, saved.getId())));
            }
        }

        auditService.logEvent(
                organizationId,
                SecurityUtils.getCurrentUserId(),
                "LOCATION_UPDATED",
                "LOCATION",
                saved.getId().toString(),
                null,
                Map.of("name", saved.getName(), "city", saved.getCity(), "isActive", saved.isActive())
        );

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void deleteLocation(UUID organizationId, UUID locationId) {
        validateTenantAccess(organizationId);

        LocationEntity location = locationRepository.findByIdAndOrganizationId(locationId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", locationId));

        location.setActive(false);
        locationRepository.save(location);

        auditService.logEvent(
                organizationId,
                SecurityUtils.getCurrentUserId(),
                "LOCATION_DEACTIVATED",
                "LOCATION",
                locationId.toString(),
                null,
                Map.of("name", location.getName())
        );
    }

    @Override
    @Transactional
    public LocationDto assignEmployees(UUID organizationId, UUID locationId, List<UUID> employeeIds) {
        validateTenantAccess(organizationId);

        LocationEntity location = locationRepository.findByIdAndOrganizationId(locationId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", locationId));

        employeeLocationRepository.deleteByLocationId(locationId);
        if (employeeIds != null) {
            for (UUID empId : employeeIds) {
                employeeLocationRepository.save(new EmployeeLocationEntity(new EmployeeLocationId(empId, locationId)));
            }
        }

        return mapToDto(location);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> getLocationsForEmployee(UUID organizationId, UUID employeeId) {
        validateTenantAccess(organizationId);
        return employeeLocationRepository.findLocationIdsByEmployeeId(employeeId);
    }

    private void unsetHeadOffice(UUID organizationId) {
        List<LocationEntity> activeLocations = locationRepository.findAllByOrganizationIdAndIsActiveTrue(organizationId);
        for (LocationEntity loc : activeLocations) {
            if (loc.isHeadOffice()) {
                loc.setHeadOffice(false);
                locationRepository.save(loc);
            }
        }
    }

    private LocationDto mapToDto(LocationEntity entity) {
        List<UUID> employeeIds = employeeLocationRepository.findEmployeeIdsByLocationId(entity.getId());
        if (employeeIds == null) {
            employeeIds = Collections.emptyList();
        }

        return LocationDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .name(entity.getName())
                .code(entity.getCode())
                .addressLine1(entity.getAddressLine1())
                .addressLine2(entity.getAddressLine2())
                .city(entity.getCity())
                .state(entity.getState())
                .pincode(entity.getPincode())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .isHeadOffice(entity.isHeadOffice())
                .isActive(entity.isActive())
                .assignedEmployeeIds(employeeIds)
                .assignedEmployeeCount(employeeIds.size())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
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
