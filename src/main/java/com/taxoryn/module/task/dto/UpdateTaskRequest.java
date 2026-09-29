package com.taxoryn.module.task.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update Task Payload")
public class UpdateTaskRequest {

    private UUID clientId;
    private UUID engagementId;

    @JsonAlias({"assignedUserId", "assignedTo"})
    private UUID assignedTo;
    private Boolean unassign;

    private UUID workItemId;
    private UUID locationId;

    @Size(min = 3, max = 255, message = "Task title must be between 3 and 255 characters")
    private String title;

    private String description;
    private TaskCategory taskCategory;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDate startDate;
    private LocalDate dueDate;
    private UUID complianceId;
    private UUID documentRequestId;
    private UUID noticeId;
    private String blockedReason;
    private Boolean clearBlockedReason;
    private String notes;
    private Integer estimatedMinutes;
    private Integer actualMinutes;

    public UUID getAssignedUserId() {
        return assignedTo;
    }

    public void setAssignedUserId(UUID assignedUserId) {
        this.assignedTo = assignedUserId;
    }
}
