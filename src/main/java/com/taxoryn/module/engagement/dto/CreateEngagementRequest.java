package com.taxoryn.module.engagement.dto;

import com.taxoryn.module.engagement.model.EngagementPriority;
import com.taxoryn.module.engagement.model.EngagementStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create Engagement Request Payload")
public class CreateEngagementRequest {

    @NotNull(message = "Client ID is required")
    private UUID clientId;

    private UUID serviceId;
    private UUID locationId;
    private UUID clientServiceId;

    @Size(max = 100, message = "Engagement code cannot exceed 100 characters")
    private String engagementCode;

    @NotBlank(message = "Engagement name is required")
    @Size(min = 2, max = 255, message = "Engagement name must be between 2 and 255 characters")
    private String name;

    private String description;

    @Builder.Default
    private EngagementStatus status = EngagementStatus.ACTIVE;

    private LocalDate startDate;
    private LocalDate endDate;
    private UUID assignedUserId;
    private UUID reviewerUserId;

    @Builder.Default
    private EngagementPriority priority = EngagementPriority.MEDIUM;

    private String notes;
}
