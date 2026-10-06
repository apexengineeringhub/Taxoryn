package com.taxoryn.module.client.businesscontext.dto;

import com.taxoryn.module.client.dto.ClientActionRecommendationDto;
import com.taxoryn.module.client.dto.ClientIntelligenceSignalDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Level 3: Attention & Intelligence Context Model (What signals need review?)")
public class AttentionSummaryContextDto {

    @Schema(description = "Total number of active attention signals")
    private int totalAttentionSignalsCount;

    @Schema(description = "Number of CRITICAL or HIGH priority attention signals")
    private int highPrioritySignalsCount;

    @Schema(description = "Active deterministic attention signals")
    @Builder.Default
    private List<ClientIntelligenceSignalDto> signals = new ArrayList<>();

    @Schema(description = "Recommended next-best actions")
    @Builder.Default
    private List<ClientActionRecommendationDto> recommendedActions = new ArrayList<>();
}
