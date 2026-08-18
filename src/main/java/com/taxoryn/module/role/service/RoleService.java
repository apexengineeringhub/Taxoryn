package com.taxoryn.module.role.service;

import com.taxoryn.module.role.dto.AssignUserRolesRequest;
import com.taxoryn.module.role.dto.CreateRoleRequest;
import com.taxoryn.module.role.dto.PermissionDto;
import com.taxoryn.module.role.dto.RoleDto;
import com.taxoryn.module.role.dto.UpdateRoleRequest;
import com.taxoryn.module.role.dto.UserRolesResponse;
import com.taxoryn.module.role.entity.RoleEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface RoleService {

    List<RoleDto> getAvailableRoles();

    RoleDto getRoleById(UUID roleId);

    List<PermissionDto> getAllPermissions();

    RoleDto createCustomRole(CreateRoleRequest request);

    RoleDto updateCustomRole(UUID roleId, UpdateRoleRequest request);

    void deleteCustomRole(UUID roleId);

    UserRolesResponse assignRolesToUser(UUID userId, AssignUserRolesRequest request);

    UserRolesResponse removeRoleFromUser(UUID userId, UUID roleId);

    UserRolesResponse getUserRolesAndPermissions(UUID userId);

    List<RoleEntity> getRolesByCodes(Set<String> codes, UUID organizationId);
}
