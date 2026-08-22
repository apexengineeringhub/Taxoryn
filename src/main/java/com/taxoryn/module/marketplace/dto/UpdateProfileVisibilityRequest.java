package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.VisibilityStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update marketplace practice profile visibility status")
public class UpdateProfileVisibilityRequest {

    @NotNull(message = "Visibility status is required")
    @Schema(description = "Visibility status (PRIVATE, PUBLIC, SUSPENDED)", example = "PUBLIC")
    private VisibilityStatus visibility;
}
