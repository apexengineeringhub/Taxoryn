package com.taxoryn.module.user.service;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.PasswordSecurityUtils;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.service.RoleService;
import com.taxoryn.module.user.dto.CreateUserRequest;
import com.taxoryn.module.user.dto.UpdateUserRequest;
import com.taxoryn.module.user.dto.UserDto;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.mapper.UserMapper;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final com.taxoryn.module.employee.repository.EmployeeRepository employeeRepository;
    private final RoleService roleService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ProfileImageService profileImageService;
    private final com.taxoryn.module.user.repository.UserLocationRepository userLocationRepository;
    private final com.taxoryn.module.organization.repository.LocationRepository locationRepository;
    private final com.taxoryn.module.organization.service.LocationService locationService;
    private final com.taxoryn.module.organization.repository.OrganizationRepository organizationRepository;
    private final com.taxoryn.module.organization.repository.PracticeProfileRepository practiceProfileRepository;
    private final com.taxoryn.module.moduleconfig.service.ModuleConfigurationService moduleConfigurationService;
    private final com.taxoryn.module.subscription.service.SubscriptionEntitlementService subscriptionEntitlementService;
    private final com.taxoryn.module.audit.service.AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserDto> getUsers(PageRequestDto pageRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        Page<UserEntity> page = userRepository.findAllByOrganizationId(organizationId, pageRequest.toPageable());
        return PagedResponse.of(page, this::toEnrichedDto);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(UUID userId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UserEntity entity = getUserEntityById(userId, organizationId);
        return toEnrichedDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getCurrentUserProfile() {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UserEntity entity = getUserEntityById(userId, organizationId);
        return toEnrichedDto(entity);
    }

    @Override
    @Transactional
    public UserDto updateMyProfile(com.taxoryn.module.user.dto.UpdateUserProfileRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UserEntity user = getUserEntityById(userId, organizationId);

        if (org.springframework.util.StringUtils.hasText(request.getFirstName())) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        // Avatar mutations MUST ONLY occur through dedicated /avatar endpoints.
        // Ignore request.getAvatarUrl() to prevent untrusted storage key injection.

        UserEntity saved = userRepository.save(user);

        // Sync with linked Employee record if present
        if (organizationId != null) {
            employeeRepository.findByOrganizationIdAndUserId(organizationId, userId).ifPresent(emp -> {
                emp.setFirstName(saved.getFirstName());
                emp.setLastName(saved.getLastName());
                emp.setPhone(saved.getPhone());
                employeeRepository.save(emp);
            });
        }

        log.info("Updated self-service user profile for userId={} in tenant={}", userId, organizationId);
        return toEnrichedDto(saved);
    }

    @Override
    @Transactional
    public UserDto uploadMyAvatar(org.springframework.web.multipart.MultipartFile file) {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UserEntity user = getUserEntityById(userId, organizationId);

        String storedKey = profileImageService.storeAvatar(organizationId, userId, "user", file, user.getAvatarUrl());
        user.setAvatarUrl(storedKey);
        UserEntity saved = userRepository.save(user);

        if (organizationId != null) {
            employeeRepository.findByOrganizationIdAndUserId(organizationId, userId).ifPresent(emp -> {
                emp.setAvatarUrl(storedKey);
                employeeRepository.save(emp);
            });
        }

        log.info("Uploaded avatar for userId={} in tenant={}", userId, organizationId);
        return toEnrichedDto(saved);
    }

    @Override
    @Transactional
    public void deleteMyAvatar() {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UserEntity user = getUserEntityById(userId, organizationId);

        if (org.springframework.util.StringUtils.hasText(user.getAvatarUrl())) {
            profileImageService.deleteAvatar(user.getAvatarUrl());
            user.setAvatarUrl(null);
            userRepository.save(user);
        }

        if (organizationId != null) {
            employeeRepository.findByOrganizationIdAndUserId(organizationId, userId).ifPresent(emp -> {
                emp.setAvatarUrl(null);
                employeeRepository.save(emp);
            });
        }
        log.info("Deleted avatar for userId={} in tenant={}", userId, organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileImageService.AvatarContent getAvatar(UUID userId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UserEntity user = getUserEntityById(userId, organizationId);
        if (!org.springframework.util.StringUtils.hasText(user.getAvatarUrl())) {
            throw new ResourceNotFoundException("User avatar", "userId", userId);
        }
        return profileImageService.retrieveAvatar(user.getAvatarUrl());
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileImageService.AvatarContent getMyAvatar() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return getAvatar(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getAvatarContent(UUID userId) {
        return getAvatar(userId).getData();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getMyAvatarContent() {
        return getMyAvatar().getData();
    }

    private UserDto toEnrichedDto(UserEntity entity) {
        if (entity == null) return null;
        UserDto dto = userMapper.toDto(entity);
        if (dto != null && entity.getAvatarUrl() != null) {
            dto.setAvatarUrl(profileImageService.resolveAvatarUrl(entity.getAvatarUrl()));
        }
        return dto;
    }

    @Override
    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        // Enforce Subscription Quota for TEAM_MEMBER
        subscriptionEntitlementService.checkCanCreate(organizationId, com.taxoryn.module.subscription.entity.SubscriptionResourceType.TEAM_MEMBER);

        if (userRepository.existsByOrganizationIdAndEmailIgnoreCase(organizationId, request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        PasswordSecurityUtils.validatePassword(request.getPassword());

        // 1. RBAC Privilege Escalation & Delegation Boundary Check
        SecurityUtils.validateRoleDelegation(request.getRoleCodes(), null);

        List<RoleEntity> roles = roleService.getRolesByCodes(request.getRoleCodes(), organizationId);
        if (roles.isEmpty() || roles.size() != request.getRoleCodes().size()) {
            throw new com.taxoryn.core.exception.BusinessValidationException("One or more specified role codes are invalid or not accessible");
        }

        UserEntity user = UserEntity.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName() != null ? request.getLastName().trim() : null)
                .phone(request.getPhone())
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(roles))
                .build();
        user.setOrganizationId(organizationId);

        UserEntity saved = userRepository.save(user);
        log.info("Created user {} in organization {}", saved.getId(), organizationId);
        return userMapper.toDto(saved);
    }

    @Override
    @Transactional
    public UserDto updateUser(UUID userId, UpdateUserRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UserEntity user = getUserEntityById(userId, organizationId);

        user.setFirstName(request.getFirstName().trim());
        if (request.getLastName() != null) user.setLastName(request.getLastName().trim());
        if (request.getPhone() != null) user.setPhone(request.getPhone());

        boolean isCurrentlyOrgAdmin = user.getRoles().stream().anyMatch(r -> "ORG_ADMIN".equals(r.getCode()));

        // Prevent deactivating the last remaining active ORG_ADMIN
        if (request.getStatus() != null) {
            if (isCurrentlyOrgAdmin && request.getStatus() != UserStatus.ACTIVE && user.getStatus() == UserStatus.ACTIVE) {
                long activeAdminCount = userRepository.countActiveOrgAdmins(organizationId);
                if (activeAdminCount <= 1) {
                    throw new com.taxoryn.core.exception.BusinessValidationException(
                            "Cannot deactivate the sole remaining active Organization Administrator"
                    );
                }
            }
            user.setStatus(request.getStatus());
        }

        if (request.getRoleCodes() != null && !request.getRoleCodes().isEmpty()) {
            // 1. Block Self-Role Mutation
            UUID currentUserId = SecurityUtils.getCurrentUser().map(SecurityUser::getUserId).orElse(null);
            if (currentUserId != null && currentUserId.equals(userId) && !SecurityUtils.isTaxorynSuperAdmin()) {
                Set<String> currentRoleCodes = user.getRoles().stream().map(RoleEntity::getCode).collect(Collectors.toSet());
                if (!currentRoleCodes.equals(request.getRoleCodes())) {
                    throw new com.taxoryn.core.exception.ForbiddenException("Self-role mutation denied: You cannot modify your own assigned roles");
                }
            }

            // 2. RBAC Privilege Escalation & Delegation Boundary Check
            SecurityUtils.validateRoleDelegation(request.getRoleCodes(), userId);

            List<RoleEntity> roles = roleService.getRolesByCodes(request.getRoleCodes(), organizationId);
            if (roles.isEmpty() || roles.size() != request.getRoleCodes().size()) {
                throw new com.taxoryn.core.exception.BusinessValidationException("One or more specified role codes are invalid or not accessible");
            }

            // 3. Prevent removing ORG_ADMIN from sole remaining admin
            boolean willBeOrgAdmin = roles.stream().anyMatch(r -> "ORG_ADMIN".equals(r.getCode()));
            if (isCurrentlyOrgAdmin && !willBeOrgAdmin) {
                long activeAdminCount = userRepository.countActiveOrgAdmins(organizationId);
                if (activeAdminCount <= 1) {
                    throw new com.taxoryn.core.exception.BusinessValidationException(
                            "Cannot demote the sole remaining active Organization Administrator"
                    );
                }
            }

            user.setRoles(new HashSet<>(roles));
        }

        UserEntity saved = userRepository.save(user);
        log.info("Updated user {} in organization {}", saved.getId(), organizationId);
        return userMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UserEntity user = getUserEntityById(userId, organizationId);

        // Prevent deactivating the last remaining active ORG_ADMIN
        boolean isCurrentlyOrgAdmin = user.getRoles().stream().anyMatch(r -> "ORG_ADMIN".equals(r.getCode()));
        if (isCurrentlyOrgAdmin && user.getStatus() == UserStatus.ACTIVE) {
            long activeAdminCount = userRepository.countActiveOrgAdmins(organizationId);
            if (activeAdminCount <= 1) {
                throw new com.taxoryn.core.exception.BusinessValidationException(
                        "Cannot deactivate the sole remaining active Organization Administrator"
                );
            }
        }

        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        log.info("Deactivated user {} in organization {}", userId, organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserEntity getUserEntityById(UUID userId, UUID organizationId) {
        return userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    @Override
    @Transactional(readOnly = true)
    public com.taxoryn.module.user.dto.UserContextDto getCurrentUserContext() {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (userId == null) {
            throw new com.taxoryn.core.exception.UnauthorizedException("Authenticated user context is required");
        }

        UserEntity userEntity = getUserEntityById(userId, organizationId);
        UserDto userDto = toEnrichedDto(userEntity);

        String orgName = null;
        String practiceType = null;
        if (organizationId != null) {
            orgName = organizationRepository.findById(organizationId)
                    .map(com.taxoryn.module.organization.entity.OrganizationEntity::getName)
                    .orElse(null);
            practiceType = practiceProfileRepository.findByOrganizationId(organizationId)
                    .map(p -> p.getPracticeType() != null ? p.getPracticeType().name() : null)
                    .orElse(null);
        }

        Set<String> roles = userEntity.getRoles() != null
                ? userEntity.getRoles().stream().map(RoleEntity::getCode).collect(Collectors.toSet())
                : java.util.Collections.emptySet();

        boolean isPracticeAdmin = roles.contains("PRACTICE_ADMIN") || roles.contains("ORG_ADMIN")
                || roles.contains("PRACTICE_OWNER") || roles.contains("SUPER_ADMIN") || SecurityUtils.isTaxorynSuperAdmin();

        List<com.taxoryn.module.organization.dto.LocationDto> accessibleLocations;
        Set<UUID> assignedLocationIds;
        String scopeType;

        if (isPracticeAdmin) {
            scopeType = "ALL_LOCATIONS";
            accessibleLocations = organizationId != null ? locationService.getLocations(organizationId) : java.util.Collections.emptyList();
            assignedLocationIds = accessibleLocations.stream().map(com.taxoryn.module.organization.dto.LocationDto::getId).collect(Collectors.toSet());
        } else {
            scopeType = "ASSIGNED_LOCATIONS";
            accessibleLocations = getUserLocations(userId);
            assignedLocationIds = accessibleLocations.stream().map(com.taxoryn.module.organization.dto.LocationDto::getId).collect(Collectors.toSet());
        }

        com.taxoryn.module.user.dto.UserContextDto.LocationScopeDto locationScope = com.taxoryn.module.user.dto.UserContextDto.LocationScopeDto.builder()
                .scopeType(scopeType)
                .isAllLocations(isPracticeAdmin)
                .assignedLocationIds(assignedLocationIds)
                .accessibleLocations(accessibleLocations)
                .build();

        com.taxoryn.module.moduleconfig.dto.EffectiveConfigurationResponse effectiveConfig = organizationId != null
                ? moduleConfigurationService.getEffectiveConfiguration(organizationId)
                : null;

        return com.taxoryn.module.user.dto.UserContextDto.builder()
                .user(userDto)
                .organizationId(organizationId)
                .organizationName(orgName)
                .practiceType(practiceType)
                .roles(roles)
                .locationScope(locationScope)
                .effectiveConfiguration(effectiveConfig)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.taxoryn.module.organization.dto.LocationDto> getUserLocations(UUID userId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UserEntity user = getUserEntityById(userId, organizationId);

        boolean isPracticeAdmin = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> "PRACTICE_ADMIN".equals(r.getCode()) || "ORG_ADMIN".equals(r.getCode()) || "PRACTICE_OWNER".equals(r.getCode()) || "SUPER_ADMIN".equals(r.getCode()));

        if (isPracticeAdmin) {
            return locationService.getLocations(organizationId);
        }

        Set<UUID> locationIds = new HashSet<>(userLocationRepository.findLocationIdsByUserIdAndOrganizationId(userId, organizationId));

        employeeRepository.findByOrganizationIdAndUserId(organizationId, userId).ifPresent(emp -> {
            locationIds.addAll(locationService.getLocationsForEmployee(organizationId, emp.getId()));
        });

        if (locationIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return locationRepository.findAllById(locationIds).stream()
                .filter(loc -> loc.getOrganizationId().equals(organizationId) && loc.isActive())
                .map(loc -> locationService.getLocationById(organizationId, loc.getId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<com.taxoryn.module.organization.dto.LocationDto> assignUserLocations(UUID userId, List<UUID> locationIds) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UserEntity user = getUserEntityById(userId, organizationId);

        if (locationIds != null && !locationIds.isEmpty()) {
            for (UUID locId : locationIds) {
                com.taxoryn.module.organization.entity.LocationEntity loc = locationRepository.findById(locId)
                        .orElseThrow(() -> new ResourceNotFoundException("Location", "id", locId));
                if (!organizationId.equals(loc.getOrganizationId())) {
                    throw new com.taxoryn.core.exception.TenantAccessDeniedException(
                            "Cross-tenant location assignment violation: Location " + locId +
                                    " belongs to organization " + loc.getOrganizationId() +
                                    ", but user " + userId + " belongs to organization " + organizationId
                    );
                }
            }
        }

        userLocationRepository.deleteByUserIdAndOrganizationId(userId, organizationId);

        if (locationIds != null && !locationIds.isEmpty()) {
            for (UUID locId : locationIds) {
                userLocationRepository.save(
                        com.taxoryn.module.user.entity.UserLocationEntity.builder()
                                .id(new com.taxoryn.module.user.entity.UserLocationEntity.UserLocationId(userId, locId))
                                .organizationId(organizationId)
                                .build()
                );
            }
        }

        auditService.logEvent(
                organizationId,
                SecurityUtils.getCurrentUserId(),
                "USER_LOCATIONS_ASSIGNED",
                "USER",
                userId.toString(),
                null,
                java.util.Map.of("assignedLocationCount", locationIds != null ? locationIds.size() : 0)
        );

        log.info("Assigned {} locations to user {} in organization {}", locationIds != null ? locationIds.size() : 0, userId, organizationId);
        return getUserLocations(userId);
    }

    @Override
    @Transactional
    public com.taxoryn.module.organization.dto.LocationDto assignUserLocation(UUID userId, UUID locationId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UserEntity user = getUserEntityById(userId, organizationId);

        com.taxoryn.module.organization.entity.LocationEntity loc = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", locationId));
        if (!organizationId.equals(loc.getOrganizationId())) {
            throw new com.taxoryn.core.exception.TenantAccessDeniedException(
                    "Cross-tenant location assignment violation: Location " + locationId +
                            " belongs to organization " + loc.getOrganizationId() +
                            ", but user " + userId + " belongs to organization " + organizationId
            );
        }

        if (!userLocationRepository.existsByIdUserIdAndIdLocationIdAndOrganizationId(userId, locationId, organizationId)) {
            userLocationRepository.save(
                    com.taxoryn.module.user.entity.UserLocationEntity.builder()
                            .id(new com.taxoryn.module.user.entity.UserLocationEntity.UserLocationId(userId, locationId))
                            .organizationId(organizationId)
                            .build()
            );
        }

        auditService.logEvent(
                organizationId,
                SecurityUtils.getCurrentUserId(),
                "USER_LOCATION_ASSIGNED",
                "USER",
                userId.toString(),
                null,
                java.util.Map.of("locationId", locationId.toString())
        );

        return locationService.getLocationById(organizationId, locationId);
    }

    @Override
    @Transactional
    public void removeUserLocation(UUID userId, UUID locationId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UserEntity user = getUserEntityById(userId, organizationId);

        userLocationRepository.deleteByUserIdAndLocationIdAndOrganizationId(userId, locationId, organizationId);

        auditService.logEvent(
                organizationId,
                SecurityUtils.getCurrentUserId(),
                "USER_LOCATION_REMOVED",
                "USER",
                userId.toString(),
                null,
                java.util.Map.of("locationId", locationId.toString())
        );
        log.info("Removed location {} from user {} in organization {}", locationId, userId, organizationId);
    }

    @Override
    @Transactional
    public UserDto updateUserStatus(UUID userId, UserStatus status) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UserEntity user = getUserEntityById(userId, organizationId);

        boolean isCurrentlyAdmin = user.getRoles().stream()
                .anyMatch(r -> "ORG_ADMIN".equals(r.getCode()) || "PRACTICE_ADMIN".equals(r.getCode()) || "PRACTICE_OWNER".equals(r.getCode()));

        if (isCurrentlyAdmin && status != UserStatus.ACTIVE && user.getStatus() == UserStatus.ACTIVE) {
            long activeAdminCount = userRepository.countActiveOrgAdmins(organizationId);
            if (activeAdminCount <= 1) {
                throw new com.taxoryn.core.exception.BusinessValidationException(
                        "Cannot deactivate the sole remaining active Organization Administrator"
                );
            }
        }

        user.setStatus(status);
        UserEntity saved = userRepository.save(user);

        auditService.logEvent(
                organizationId,
                SecurityUtils.getCurrentUserId(),
                "USER_STATUS_UPDATED",
                "USER",
                userId.toString(),
                null,
                java.util.Map.of("newStatus", status.name())
        );
        log.info("Updated status of user {} to {} in organization {}", userId, status, organizationId);
        return toEnrichedDto(saved);
    }

    @Override
    @Transactional
    public UserDto updateUserRole(UUID userId, Set<String> roleCodes) {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName(getUserEntityById(userId, SecurityUtils.getCurrentOrganizationId()).getFirstName());
        request.setRoleCodes(roleCodes);
        return updateUser(userId, request);
    }
}
