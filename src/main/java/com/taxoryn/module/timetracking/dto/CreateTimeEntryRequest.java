package com.taxoryn.module.timetracking.dto;

import com.taxoryn.module.timetracking.model.TimeEntryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Create Time Entry Request Payload")
public class CreateTimeEntryRequest {

    @NotNull(message = "Client ID is required")
    private UUID clientId;

    private UUID locationId;
    private UUID engagementId;
    private UUID workItemId;
    private UUID taskId;
    private UUID userId;

    @NotNull(message = "Entry date is required")
    private LocalDate entryDate;

    @NotNull(message = "Duration in minutes is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes;

    private String description;

    @Builder.Default
    private Boolean billable = true;

    private BigDecimal billingRate;

    @Builder.Default
    private TimeEntryStatus status = TimeEntryStatus.SUBMITTED;
}
