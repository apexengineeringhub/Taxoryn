package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.marketplace.entity.MarketplaceReviewEntity.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to moderate a practitioner review status")
public class ModerateReviewRequest {

    @NotNull(message = "Review status is required")
    private ReviewStatus status;

    @Size(max = 500, message = "Moderation note cannot exceed 500 characters")
    private String moderationNote;
}
