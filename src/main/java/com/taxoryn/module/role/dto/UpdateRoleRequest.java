package com.taxoryn.module.role.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
@Schema(description = "Update Custom Role Request")
public class UpdateRoleRequest {

    @NotBlank(message = "Role name is required")
    @Size(min = 2, max = 255, message = "Role name must be between 2 and 255 characters")
    @Schema(description = "Display name for the role", example = "Senior Audit Associate")
    private String name;

    @Schema(description = "Description of responsibilities and scope", example = "Audit lead for corporate clients")
    private String description;

    @NotEmpty(message = "At least one permission code must be assigned to the role")
    @Schema(description = "Assigned permission codes", example = "[\"CLIENT_VIEW\", \"TASK_VIEW\", \"GST_VIEW\"]")
    private Set<String> permissionCodes;
}
