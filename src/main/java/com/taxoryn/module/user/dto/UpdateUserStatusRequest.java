package com.taxoryn.module.user.dto;

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
@Schema(description = "Request payload to update user lifecycle status")
public class UpdateUserStatusRequest {

    @NotNull(message = "Status cannot be null")
    @Schema(description = "Target user status", example = "ACTIVE")
    private UserStatus status;
}
