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
 * Practitioner-facing follow-up item representing a pending client action,
 * overdue request, expiring approval, unanswered question, or compliance deadline.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Follow-up item response for practitioner dashboard and actions")
public class FollowUpItemDto {

    private UUID id;
    private UUID organizationId;

    private UUID clientId;
    private String clientName;

    private ReminderType category;
    private String title;
    private String description;
    private ReminderPriority priority;
    private ReminderStatus status;
    private boolean overdue;

    private String referenceType;
    private String referenceId;

    private UUID targetUserId;
    private String targetUserName;

    private String actionUrl;

    private Instant scheduledAt;
    private Instant triggeredAt;
    private Instant lastAttemptAt;
    private Integer notificationAttempts;
    private String lastError;

    private String notes;
    private Instant createdAt;
}
