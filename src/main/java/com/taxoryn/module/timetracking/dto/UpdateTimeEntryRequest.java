package com.taxoryn.module.timetracking.dto;

import com.taxoryn.module.timetracking.model.TimeEntryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update Time Entry Request Payload")
public class UpdateTimeEntryRequest {

    private UUID locationId;
    private UUID engagementId;
    private UUID workItemId;
    private UUID taskId;
    private UUID userId;
    private LocalDate entryDate;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes;

    private String description;
    private Boolean billable;
    private BigDecimal billingRate;
    private TimeEntryStatus status;
}
