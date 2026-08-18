package com.taxoryn.module.role.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.TenantAccessDeniedException;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.role.dto.AssignUserRolesRequest;
import com.taxoryn.module.role.dto.CreateRoleRequest;
import com.taxoryn.module.role.dto.PermissionDto;
import com.taxoryn.module.role.dto.RoleDto;
import com.taxoryn.module.role.dto.UpdateRoleRequest;
import com.taxoryn.module.role.dto.UserRolesResponse;
import com.taxoryn.module.role.entity.PermissionEntity;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.mapper.RoleMapper;
import com.taxoryn.module.role.repository.PermissionRepository;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final RoleMapper roleMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RoleDto> getAvailableRoles() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        List<RoleEntity> roles = roleRepository.findAllAvailableForOrganization(organizationId);
        return roleMapper.toDtoList(roles);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleDto getRoleById(UUID roleId) {
        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        if (!role.isSystemRole()) {
            validateTenantAccess(role.getOrganizationId());
        }

        return roleMapper.toDto(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionDto> getAllPermissions() {
        List<PermissionEntity> permissions = permissionRepository.findAll();
        return roleMapper.toPermissionDtoList(permissions);
    }

    @Override
    @Transactional
    public RoleDto createCustomRole(CreateRoleRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        String code = request.getCode().toUpperCase().trim();

        // Check if role code exists in system roles or within the organization
        if (roleRepository.findByCodeAndIsSystemRoleTrue(code).isPresent()
                || roleRepository.existsByCodeAndOrganizationId(code, organizationId)) {
            throw new DuplicateResourceException("Role", "code", code);
        }

        List<PermissionEntity> matchedPermissions = permissionRepository.findByCodeIn(request.getPermissionCodes());
        if (matchedPermissions.isEmpty()) {
            throw new BusinessValidationException("At least one valid permission code must be provided");
        }

        RoleEntity role = RoleEntity.builder()
                .organizationId(organizationId)
                .code(code)
                .name(request.getName().trim())
                .description(request.getDescription())
                .isSystemRole(false)
                .permissions(new HashSet<>(matchedPermissions))
                .build();

        RoleEntity saved = roleRepository.save(role);
        log.info("Created custom role: id={}, code={} for tenant={}", saved.getId(), saved.getCode(), organizationId);
        return roleMapper.toDto(saved);
    }

    @Override
    @Transactional
    public RoleDto updateCustomRole(UUID roleId, UpdateRoleRequest request) {
        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        if (role.isSystemRole()) {
            throw new ForbiddenException("System default roles cannot be modified");
        }

        validateTenantAccess(role.getOrganizationId());

        List<PermissionEntity> matchedPermissions = permissionRepository.findByCodeIn(request.getPermissionCodes());
        if (matchedPermissions.isEmpty()) {
            throw new BusinessValidationException("At least one valid permission code must be provided");
        }

        role.setName(request.getName().trim());
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription().trim());
        }
        role.setPermissions(new HashSet<>(matchedPermissions));

        RoleEntity saved = roleRepository.save(role);
        log.info("Updated custom role: id={} for tenant={}", saved.getId(), role.getOrganizationId());
        return roleMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteCustomRole(UUID roleId) {
        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        if (role.isSystemRole()) {
            throw new ForbiddenException("System default roles cannot be deleted");
        }

        validateTenantAccess(role.getOrganizationId());

        roleRepository.delete(role);
        log.info("Deleted custom role: id={} for tenant={}", roleId, role.getOrganizationId());
    }

    @Override
    @Transactional
    public UserRolesResponse assignRolesToUser(UUID userId, AssignUserRolesRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UserEntity user = userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        List<RoleEntity> roles = getRolesByCodes(request.getRoleCodes(), organizationId);
        if (roles.isEmpty()) {
            throw new BusinessValidationException("At least one valid role must be assigned");
        }

        user.setRoles(new HashSet<>(roles));
        UserEntity saved = userRepository.save(user);

        log.info("Assigned {} roles to user {} in tenant {}", roles.size(), userId, organizationId);
        return buildUserRolesResponse(saved);
    }

    @Override
    @Transactional
    public UserRolesResponse removeRoleFromUser(UUID userId, UUID roleId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UserEntity user = userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        boolean removed = user.getRoles().removeIf(r -> r.getId().equals(roleId));
        if (!removed) {
            throw new ResourceNotFoundException("Role assignment", "roleId", roleId);
        }

        if (user.getRoles().isEmpty()) {
            throw new BusinessValidationException("A user must have at least one role assigned");
        }

        UserEntity saved = userRepository.save(user);
        log.info("Removed role {} from user {} in tenant {}", roleId, userId, organizationId);
        return buildUserRolesResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserRolesResponse getUserRolesAndPermissions(UUID userId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UserEntity user = userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return buildUserRolesResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleEntity> getRolesByCodes(Set<String> codes, UUID organizationId) {
        return roleRepository.findByCodesAndOrganizationId(codes, organizationId);
    }

    private UserRolesResponse buildUserRolesResponse(UserEntity user) {
        Set<RoleDto> roleDtos = user.getRoles().stream()
                .map(roleMapper::toDto)
                .collect(Collectors.toSet());

        Set<PermissionDto> permissionDtos = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(roleMapper::toDto)
                .collect(Collectors.toSet());

        return UserRolesResponse.builder()
                .userId(user.getId())
                .organizationId(user.getOrganizationId())
                .roles(roleDtos)
                .effectivePermissions(permissionDtos)
                .build();
    }

    private void validateTenantAccess(UUID requestedOrganizationId) {
        if (SecurityUtils.hasRole("SUPER_ADMIN")) {
            return;
        }

        UUID currentTenantId = SecurityUtils.getCurrentOrganizationId();
        if (currentTenantId == null || !currentTenantId.equals(requestedOrganizationId)) {
            throw new TenantAccessDeniedException("Cross-tenant access violation: Action denied for organization " + requestedOrganizationId);
        }
    }
}
