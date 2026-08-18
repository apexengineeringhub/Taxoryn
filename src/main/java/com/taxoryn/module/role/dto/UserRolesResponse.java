package com.taxoryn.module.role.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User Roles and Effective Permissions Response")
public class UserRolesResponse {

    @Schema(description = "User ID")
    private UUID userId;

    @Schema(description = "Organization ID")
    private UUID organizationId;

    @Schema(description = "Assigned roles")
    private Set<RoleDto> roles;

    @Schema(description = "Aggregated distinct effective permissions")
    private Set<PermissionDto> effectivePermissions;
}
