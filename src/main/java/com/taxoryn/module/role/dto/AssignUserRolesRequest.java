package com.taxoryn.module.role.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Assign Roles to User Request")
public class AssignUserRolesRequest {

    @NotEmpty(message = "At least one role code must be assigned to the user")
    @Schema(description = "Set of role codes to assign to user", example = "[\"TAX_PROFESSIONAL\", \"ACCOUNTANT\"]")
    private Set<String> roleCodes;
}
