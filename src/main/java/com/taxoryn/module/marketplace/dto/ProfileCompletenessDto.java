package com.taxoryn.module.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Practice Marketplace Profile Completeness Metrics")
public class ProfileCompletenessDto {

    @Schema(description = "Overall completeness percentage (0 - 100)", example = "70")
    private Integer percentage;

    @Schema(description = "List of completed profile fields / sections", example = "[\"Practice name\", \"Description\", \"Phone\"]")
    private List<String> completedItems;

    @Schema(description = "List of missing profile fields / sections", example = "[\"Website\", \"Location\"]")
    private List<String> missingItems;
}
