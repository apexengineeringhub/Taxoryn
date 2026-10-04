package com.taxoryn.module.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Practice and user reminders summary metrics")
public class ReminderSummaryDto {

    @Schema(description = "Total pending reminders", example = "12")
    @Builder.Default
    private long pending = 0;

    @Schema(description = "Overdue pending reminders scheduled in the past", example = "3")
    @Builder.Default
    private long overdue = 0;

    @Schema(description = "Upcoming reminders due within next 7 days", example = "5")
    @Builder.Default
    private long upcoming = 0;

    @Schema(description = "Completed/Triggered reminders count", example = "28")
    @Builder.Default
    private long triggered = 0;
}
