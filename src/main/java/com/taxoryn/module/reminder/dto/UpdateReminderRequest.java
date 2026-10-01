package com.taxoryn.module.reminder.dto;

import com.taxoryn.module.reminder.entity.ReminderPriority;
import com.taxoryn.module.reminder.entity.ReminderRecurrenceType;
import com.taxoryn.module.reminder.entity.ReminderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Request payload for updating an existing reminder.
 * All fields are optional — only non-null values are applied.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update a reminder")
public class UpdateReminderRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Schema(description = "New title")
    private String title;

    @Size(max = 2000)
    @Schema(description = "New description")
    private String description;

    @Schema(description = "New reminder type")
    private ReminderType reminderType;

    @Schema(description = "New priority")
    private ReminderPriority priority;

    @Schema(description = "New scheduled time (must be in future if provided)")
    private Instant scheduledAt;

    @Schema(description = "New recurrence type")
    private ReminderRecurrenceType recurrenceType;

    @Schema(description = "Re-assign reminder to a different user")
    private UUID targetUserId;

    @Schema(description = "Link to a client")
    private UUID clientId;

    @Schema(description = "Link to an engagement")
    private UUID engagementId;

    @Schema(description = "Link to a work instance")
    private UUID workInstanceId;

    @Schema(description = "Link to a task")
    private UUID taskId;

    @Size(max = 2000)
    @Schema(description = "Internal notes")
    private String notes;
}
