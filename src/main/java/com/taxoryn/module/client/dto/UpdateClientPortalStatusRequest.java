package com.taxoryn.module.client.dto;

import com.taxoryn.module.user.entity.UserEntity.UserStatus;
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
@Schema(description = "Update Client Portal Access Lifecycle Status Payload")
public class UpdateClientPortalStatusRequest {

    @NotNull(message = "Portal status is required (ACTIVE, SUSPENDED, INACTIVE)")
    @Schema(description = "Target portal access status", example = "SUSPENDED")
    private UserStatus portalStatus;

    @Schema(description = "Optional reason or internal administrative note for the status change", example = "Temporary client request")
    private String reason;
}
