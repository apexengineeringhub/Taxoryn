package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientActionType;
import com.taxoryn.module.client.entity.SignalPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Next-best-action recommendation for client management")
public class ClientActionRecommendationDto {

    @Schema(description = "Recommendation identifier")
    private String id;

    @Schema(description = "Structured action type")
    private ClientActionType actionType;

    @Schema(description = "Recommendation headline")
    private String title;

    @Schema(description = "Business rationale / justification")
    private String reason;

    @Schema(description = "Action priority level")
    private SignalPriority priority;

    @Schema(description = "Target client ID")
    private UUID clientId;

    @Schema(description = "Target sub-entity ID if applicable")
    private String relatedEntityId;

    @Schema(description = "Source rule or signal code")
    private String source;

    @Schema(description = "Whether the action is actionable directly")
    private boolean actionable;

    @Schema(description = "Suggested frontend route path")
    private String suggestedRoute;
}
