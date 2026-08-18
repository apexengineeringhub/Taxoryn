package com.taxoryn.module.organization.dto;

import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update Organization Status Request")
public class UpdateOrganizationStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "Target status (ACTIVE, INACTIVE, SUSPENDED, DEACTIVATED)", example = "ACTIVE")
    private OrganizationStatus status;

    @Schema(description = "Optional reason for status modification", example = "Subscription renewed / payment verified")
    private String reason;
}
