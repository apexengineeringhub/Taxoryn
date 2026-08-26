package com.taxoryn.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update role assignment for an internal Taxoryn platform user")
public class UpdatePlatformUserRoleRequest {

    @NotBlank(message = "Target role code is required")
    @Schema(description = "Target Taxoryn Platform Role Code (e.g. TAXORYN_OPERATIONS_ADMIN, TAXORYN_SUPPORT_ADMIN, TAXORYN_FINANCE_ADMIN, TAXORYN_MARKETPLACE_ADMIN, TAXORYN_CONTENT_ADMIN, TAXORYN_SECURITY_ADMIN, TAXORYN_ENGINEERING_ADMIN)", example = "TAXORYN_OPERATIONS_ADMIN")
    private String roleCode;
}
