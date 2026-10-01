package com.taxoryn.module.reminder.dto;

import com.taxoryn.module.reminder.entity.ReminderPriority;
import com.taxoryn.module.reminder.entity.ReminderStatus;
import com.taxoryn.module.reminder.entity.ReminderType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Query filter parameters for reminder list endpoints.
 * All fields are optional.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Filter parameters for reminder queries")
public class ReminderFilterRequest {

    @Schema(description = "Filter by status")
    private ReminderStatus status;

    @Schema(description = "Filter by reminder type")
    private ReminderType reminderType;

    @Schema(description = "Filter by priority")
    private ReminderPriority priority;

    @Schema(description = "Filter by target user ID (admin use)")
    private UUID targetUserId;

    @Schema(description = "Filter by client")
    private UUID clientId;

    @Schema(description = "Filter by engagement")
    private UUID engagementId;

    @Schema(description = "Filter by work instance")
    private UUID workInstanceId;

    @Schema(description = "Filter by task")
    private UUID taskId;

    @Schema(description = "Filter reminders scheduled on or after this time")
    private Instant scheduledFrom;

    @Schema(description = "Filter reminders scheduled on or before this time")
    private Instant scheduledTo;

    @Schema(description = "Include only overdue PENDING reminders (scheduledAt < now)")
    private Boolean overdueOnly;

    @Schema(description = "Page number (0-indexed)", defaultValue = "0")
    @Builder.Default
    private int page = 0;

    @Schema(description = "Page size", defaultValue = "20")
    @Builder.Default
    private int size = 20;
}
