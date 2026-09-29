package com.taxoryn.module.engagement.dto;

import com.taxoryn.module.engagement.model.EngagementPriority;
import com.taxoryn.module.engagement.model.EngagementStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Update Engagement Request Payload")
public class UpdateEngagementRequest {

    private UUID serviceId;
    private UUID locationId;
    private UUID clientServiceId;

    @Size(max = 100, message = "Engagement code cannot exceed 100 characters")
    private String engagementCode;

    @Size(min = 2, max = 255, message = "Engagement name must be between 2 and 255 characters")
    private String name;

    private String description;
    private EngagementStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private UUID assignedUserId;
    private UUID reviewerUserId;
    private EngagementPriority priority;
    private String notes;
}
