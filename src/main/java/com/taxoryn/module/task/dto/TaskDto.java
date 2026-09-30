package com.taxoryn.module.task.dto;

import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
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
@Schema(description = "Task Details Payload")
public class TaskDto {

    private UUID id;
    private UUID organizationId;
    private UUID clientId;
    private String clientName;
    private UUID engagementId;
    private String engagementTitle;
    private String engagementCode;
    private UUID workInstanceId;
    private UUID workTemplateTaskId;
    private UUID locationId;
    private String locationName;
    private UUID workItemId;
    private UUID assignedTo;
    private UUID assignedUserId;
    private String assigneeName;
    private String assigneeEmail;
    private String title;
    private String description;
    private TaskCategory taskCategory;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDate startDate;
    private LocalDate dueDate;
    private UUID complianceId;
    private String complianceTitle;
    private LocalDate statutoryDueDate;
    private UUID documentRequestId;
    private String documentRequestNumber;
    private String documentRequestStatus;
    private Integer documentRequestItemsCount;
    private Integer documentRequestReceivedCount;
    private UUID noticeId;
    private String noticeNumber;
    private String blockedReason;
    private String notes;
    private Integer estimatedMinutes;
    private Integer actualMinutes;
    private UUID completedBy;
    private String completedByName;
    private Boolean isOverdue;
    private Boolean isDueToday;
    private Boolean isDueThisWeek;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
