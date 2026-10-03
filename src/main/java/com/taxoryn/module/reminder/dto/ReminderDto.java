package com.taxoryn.module.reminder.dto;

import com.taxoryn.module.reminder.entity.ReminderEntity;
import com.taxoryn.module.reminder.entity.ReminderPriority;
import com.taxoryn.module.reminder.entity.ReminderRecurrenceType;
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
 * Response DTO for a Reminder.
 *
 * Contains enriched fields (clientName, taskTitle, etc.) populated by the service layer.
 * Never exposes internal JPA entity references.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Reminder response")
public class ReminderDto {

    private UUID id;
    private UUID organizationId;

    private String title;
    private String description;
    private ReminderType reminderType;
    private ReminderStatus status;
    private ReminderPriority priority;

    // Relationships
    private UUID targetUserId;
    private String targetUserName;

    private UUID clientId;
    private String clientName;

    private UUID engagementId;
    private String engagementName;

    private UUID workInstanceId;
    private String workInstanceTitle;

    private UUID taskId;
    private String taskTitle;

    // Automation linkage
    private UUID automationRuleId;
    private String referenceType;
    private String referenceId;
    private String idempotencyKey;

    // Scheduling
    private Instant scheduledAt;
    private ReminderRecurrenceType recurrenceType;

    // Derived
    @Schema(description = "True if scheduled time has passed and reminder is still PENDING")
    private boolean overdue;

    // Lifecycle
    private Instant triggeredAt;
    private Instant completedAt;
    private Instant cancelledAt;

    private Integer notificationAttempts;
    private Instant lastAttemptAt;
    private String lastError;
    private UUID parentReminderId;

    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Builds a base DTO from entity without enriched names.
     * Service layer populates enriched name fields separately.
     */
    public static ReminderDto fromEntity(ReminderEntity entity) {
        return ReminderDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .reminderType(entity.getReminderType())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .targetUserId(entity.getTargetUserId())
                .clientId(entity.getClientId())
                .engagementId(entity.getEngagementId())
                .workInstanceId(entity.getWorkInstanceId())
                .taskId(entity.getTaskId())
                .automationRuleId(entity.getAutomationRuleId())
                .referenceType(entity.getReferenceType())
                .referenceId(entity.getReferenceId())
                .idempotencyKey(entity.getIdempotencyKey())
                .scheduledAt(entity.getScheduledAt())
                .recurrenceType(entity.getRecurrenceType())
                .overdue(entity.isOverdue())
                .triggeredAt(entity.getTriggeredAt())
                .completedAt(entity.getCompletedAt())
                .cancelledAt(entity.getCancelledAt())
                .notificationAttempts(entity.getNotificationAttempts())
                .lastAttemptAt(entity.getLastAttemptAt())
                .lastError(entity.getLastError())
                .parentReminderId(entity.getParentReminderId())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
