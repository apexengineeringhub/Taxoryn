package com.taxoryn.module.role.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.role.dto.AssignUserRolesRequest;
import com.taxoryn.module.role.dto.CreateRoleRequest;
import com.taxoryn.module.role.dto.PermissionDto;
import com.taxoryn.module.role.dto.RoleDto;
import com.taxoryn.module.role.dto.UpdateRoleRequest;
import com.taxoryn.module.role.dto.UserRolesResponse;
import com.taxoryn.module.role.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Roles & Permissions (RBAC)", description = "Endpoints for managing roles, granular permissions, and assigning user roles within the organization")
@SecurityRequirement(name = "BearerAuth")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List all available roles", description = "Retrieves all standard system roles and tenant-specific custom roles.")
    public ResponseEntity<ApiResponse<List<RoleDto>>> getAvailableRoles() {
        List<RoleDto> roles = roleService.getAvailableRoles();
        return ResponseEntity.ok(ApiResponse.success("Available roles retrieved successfully", roles));
    }

    @GetMapping("/{roleId}")
    @PreAuthorize("hasAuthority('ROLE_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get role by ID", description = "Retrieves role details and assigned permissions with tenant isolation verification.")
    public ResponseEntity<ApiResponse<RoleDto>> getRoleById(@PathVariable UUID roleId) {
        RoleDto role = roleService.getRoleById(roleId);
        return ResponseEntity.ok(ApiResponse.success("Role details retrieved successfully", role));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('ROLE_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List all system permissions", description = "Retrieves all standard system permission definitions across all practice modules.")
    public ResponseEntity<ApiResponse<List<PermissionDto>>> getAllPermissions() {
        List<PermissionDto> permissions = roleService.getAllPermissions();
        return ResponseEntity.ok(ApiResponse.success("Permissions retrieved successfully", permissions));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create custom tenant role", description = "Creates a new custom role for the current tenant organization with specific permissions.")
    public ResponseEntity<ApiResponse<RoleDto>> createCustomRole(@Valid @RequestBody CreateRoleRequest request) {
        RoleDto created = roleService.createCustomRole(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Custom role created successfully", created));
    }

    @PutMapping("/{roleId}")
    @PreAuthorize("hasAuthority('ROLE_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update custom role", description = "Updates name, description, and permissions of a custom tenant role. System roles cannot be modified.")
    public ResponseEntity<ApiResponse<RoleDto>> updateCustomRole(@PathVariable UUID roleId, @Valid @RequestBody UpdateRoleRequest request) {
        RoleDto updated = roleService.updateCustomRole(roleId, request);
        return ResponseEntity.ok(ApiResponse.success("Custom role updated successfully", updated));
    }

    @DeleteMapping("/{roleId}")
    @PreAuthorize("hasAuthority('ROLE_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete custom role", description = "Deletes a custom tenant role. System default roles cannot be deleted.")
    public ResponseEntity<ApiResponse<Void>> deleteCustomRole(@PathVariable UUID roleId) {
        roleService.deleteCustomRole(roleId);
        return ResponseEntity.ok(ApiResponse.success("Custom role deleted successfully", null));
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('USER_VIEW') or hasAuthority('ROLE_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get user roles and effective permissions", description = "Retrieves assigned roles and aggregated effective permissions for a user within the tenant.")
    public ResponseEntity<ApiResponse<UserRolesResponse>> getUserRolesAndPermissions(@PathVariable UUID userId) {
        UserRolesResponse response = roleService.getUserRolesAndPermissions(userId);
        return ResponseEntity.ok(ApiResponse.success("User roles and permissions retrieved successfully", response));
    }

    @PutMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('USER_UPDATE') or hasAuthority('ROLE_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Assign roles to user", description = "Assigns or replaces roles for a team member within the tenant organization.")
    public ResponseEntity<ApiResponse<UserRolesResponse>> assignRolesToUser(@PathVariable UUID userId, @Valid @RequestBody AssignUserRolesRequest request) {
        UserRolesResponse response = roleService.assignRolesToUser(userId, request);
        return ResponseEntity.ok(ApiResponse.success("User roles updated successfully", response));
    }

    @DeleteMapping("/users/{userId}/{roleId}")
    @PreAuthorize("hasAuthority('USER_UPDATE') or hasAuthority('ROLE_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Remove role from user", description = "Removes a specific role assignment from a user within the tenant organization.")
    public ResponseEntity<ApiResponse<UserRolesResponse>> removeRoleFromUser(@PathVariable UUID userId, @PathVariable UUID roleId) {
        UserRolesResponse response = roleService.removeRoleFromUser(userId, roleId);
        return ResponseEntity.ok(ApiResponse.success("Role removed from user successfully", response));
    }
}
