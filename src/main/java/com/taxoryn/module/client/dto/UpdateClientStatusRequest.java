package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
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
@Schema(description = "Update Client Lifecycle Status Payload")
public class UpdateClientStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "Target client status", example = "ACTIVE")
    private ClientStatus status;
}
