package com.taxoryn.module.engagement.dto;

import com.taxoryn.module.engagement.model.EngagementStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Engagement Detail Model")
public class EngagementDto {

    private UUID id;
    private UUID organizationId;
    private UUID locationId;
    private String locationName;
    private UUID clientId;
    private String clientName;
    private UUID clientServiceId;
    private String engagementCode;
    private String name;
    private String description;
    private EngagementStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private UUID assignedUserId;
    private String assignedUserName;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
}
