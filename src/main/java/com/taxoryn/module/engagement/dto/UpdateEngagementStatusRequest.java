package com.taxoryn.module.engagement.dto;

import com.taxoryn.module.engagement.model.EngagementStatus;
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
@Schema(description = "Update Engagement Status Request Payload")
public class UpdateEngagementStatusRequest {

    @NotNull(message = "Engagement status is required")
    private EngagementStatus status;

    private String notes;
}
