package com.taxoryn.module.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated intelligence summary containing active signals and action recommendations")
public class ClientIntelligenceSummaryDto {

    @Schema(description = "Client identifier")
    private UUID clientId;

    @Schema(description = "Total count of active attention signals")
    private int totalSignalsCount;

    @Schema(description = "Count of high priority signals")
    private int highPrioritySignalsCount;

    @Schema(description = "Count of critical priority signals")
    private int criticalSignalsCount;

    @Schema(description = "Active attention signals requiring review")
    @Builder.Default
    private List<ClientIntelligenceSignalDto> needsAttentionSignals = new ArrayList<>();

    @Schema(description = "Ordered next-best-action recommendations")
    @Builder.Default
    private List<ClientActionRecommendationDto> recommendations = new ArrayList<>();

    @Schema(description = "Timestamp when intelligence evaluation occurred")
    private Instant evaluatedAt;
}
