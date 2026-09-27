package com.taxoryn.module.timetracking.dto;

import com.taxoryn.module.timetracking.model.TimeEntryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Time Entry Detail Model")
public class TimeEntryDto {

    private UUID id;
    private UUID organizationId;
    private UUID locationId;
    private String locationName;
    private UUID clientId;
    private String clientName;
    private UUID engagementId;
    private String engagementName;
    private UUID workItemId;
    private String workItemTitle;
    private UUID taskId;
    private String taskTitle;
    private UUID userId;
    private String userName;
    private LocalDate entryDate;
    private Integer durationMinutes;
    private String description;
    private Boolean billable;
    private BigDecimal billingRate;
    private TimeEntryStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
}
