package com.taxoryn.module.role.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create Custom Organization Role Request")
public class CreateRoleRequest {

    @NotBlank(message = "Role code is required")
    @Pattern(regexp = "^[A-Z0-9_]{3,50}$", message = "Role code must be uppercase alphanumeric and underscores, length 3-50")
    private String code;

    @NotBlank(message = "Role name is required")
    @Size(min = 2, max = 255, message = "Role name must be between 2 and 255 characters")
    private String name;

    private String description;

    @NotEmpty(message = "At least one permission code must be assigned to the role")
    private Set<String> permissionCodes;
}
