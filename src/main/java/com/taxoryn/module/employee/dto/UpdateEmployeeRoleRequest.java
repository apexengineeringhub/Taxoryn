package com.taxoryn.module.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update Employee Practice Role Request")
public class UpdateEmployeeRoleRequest {

    @Schema(description = "Optional target Role ID")
    private UUID roleId;

    @Schema(description = "Target Role Code (e.g. TAX_MANAGER, TAX_ASSOCIATE, PRACTITIONER, PARTNER, ARTICLE_ASSISTANT)", example = "TAX_MANAGER")
    private String roleCode;
}
