package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientActionType;
import com.taxoryn.module.client.entity.SignalCategory;
import com.taxoryn.module.client.entity.SignalPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Deterministic intelligence attention signal for a client")
public class ClientIntelligenceSignalDto {

    @Schema(description = "Signal unique identifier / code")
    private String id;

    @Schema(description = "Machine-readable signal code (e.g. NO_ACTIVE_SERVICE, PROFILE_INCOMPLETE)")
    private String signalCode;

    @Schema(description = "Category of the signal")
    private SignalCategory category;

    @Schema(description = "Priority level (CRITICAL, HIGH, MEDIUM, LOW, INFO)")
    private SignalPriority priority;

    @Schema(description = "Short human-readable signal title")
    private String title;

    @Schema(description = "Deterministic reason why this signal was raised")
    private String reason;

    @Schema(description = "Recommended action description for the practitioner")
    private String recommendedAction;

    @Schema(description = "Structured action type for UI dispatching")
    private ClientActionType actionType;

    @Schema(description = "Suggested frontend route to resolve this item")
    private String suggestedRoute;

    @Schema(description = "Whether the signal has a direct actionable resolution in the system")
    private boolean actionable;

    @Schema(description = "Source entity or field reference")
    private String sourceDataRef;

    @Schema(description = "Evaluation timestamp")
    private Instant detectedAt;
}
