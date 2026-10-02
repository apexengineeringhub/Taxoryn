package com.taxoryn.module.reminder.dto;

import com.taxoryn.module.reminder.entity.ReminderPriority;
import com.taxoryn.module.reminder.entity.ReminderRecurrenceType;
import com.taxoryn.module.reminder.entity.ReminderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Request payload for creating a new manual reminder.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new reminder")
public class CreateReminderRequest {

    @NotBlank(message = "Reminder title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Schema(description = "Short descriptive title", example = "Call ABC regarding missing bank statement")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    @Schema(description = "Optional detailed description")
    private String description;

    @Schema(description = "Reminder category", example = "FOLLOW_UP")
    @Builder.Default
    private ReminderType reminderType = ReminderType.GENERAL;

    @Schema(description = "Priority", example = "MEDIUM")
    @Builder.Default
    private ReminderPriority priority = ReminderPriority.MEDIUM;

    @NotNull(message = "Scheduled date/time is required")
    @Future(message = "Scheduled time must be in the future")
    @Schema(description = "When this reminder should fire (ISO-8601 instant)", example = "2026-10-03T09:00:00Z")
    private Instant scheduledAt;

    @Schema(description = "Recurrence pattern (default: NONE = one-time)", example = "NONE")
    @Builder.Default
    private ReminderRecurrenceType recurrenceType = ReminderRecurrenceType.NONE;

    // -------------------------------------------------------------------------
    // Optional context associations (all nullable)
    // -------------------------------------------------------------------------

    @Schema(description = "User to remind (defaults to current user if omitted)")
    private UUID targetUserId;

    @Schema(description = "Optional client this reminder is about")
    private UUID clientId;

    @Schema(description = "Optional engagement this reminder is about")
    private UUID engagementId;

    @Schema(description = "Optional work instance this reminder is about")
    private UUID workInstanceId;

    @Schema(description = "Optional task this reminder is about")
    private UUID taskId;

    @Size(max = 2000)
    @Schema(description = "Internal notes")
    private String notes;
}
