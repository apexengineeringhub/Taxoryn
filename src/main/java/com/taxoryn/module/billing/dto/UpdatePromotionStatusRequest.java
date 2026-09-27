package com.taxoryn.module.billing.dto;

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
@Schema(description = "Update Promotion Status Request")
public class UpdatePromotionStatusRequest {

    @NotNull(message = "Active status is required")
    @Schema(description = "Set active to true or false", example = "true")
    private Boolean active;
}
