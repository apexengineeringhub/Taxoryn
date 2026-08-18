package com.taxoryn.module.role.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Role Details Payload")
public class RoleDto {

    private UUID id;
    private UUID organizationId;
    private String code;
    private String name;
    private String description;
    private boolean isSystemRole;
    private Set<PermissionDto> permissions;
    private Instant createdAt;
    private Instant updatedAt;
}
