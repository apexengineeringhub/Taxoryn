package com.taxoryn.module.task.dto;

import com.taxoryn.module.task.model.WorkItemPriority;
import com.taxoryn.module.task.model.WorkItemStatus;
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
@Schema(description = "Work Item representation DTO")
public class WorkItemDto {

    @Schema(description = "Work Item unique identifier")
    private UUID id;

    @Schema(description = "Organization tenant ID")
    private UUID organizationId;

    @Schema(description = "Location ID")
    private UUID locationId;

    @Schema(description = "Location name")
    private String locationName;

    @Schema(description = "Client ID")
    private UUID clientId;

    @Schema(description = "Client display name")
    private String clientName;

    @Schema(description = "Client Service ID")
    private UUID clientServiceId;

    @Schema(description = "Compliance Workflow ID")
    private UUID workflowId;

    @Schema(description = "Tax Notice ID")
    private UUID noticeId;

    @Schema(description = "Tax Notice Number")
    private String noticeNumber;

    @Schema(description = "Work item title")
    private String title;

    @Schema(description = "Work item description")
    private String description;

    @Schema(description = "Lifecycle status")
    private WorkItemStatus status;

    @Schema(description = "Priority level")
    private WorkItemPriority priority;

    @Schema(description = "Assigned user ID")
    private UUID assignedUserId;

    @Schema(description = "Assigned user name")
    private String assignedUserName;

    @Schema(description = "Statutory or target due date")
    private LocalDate dueDate;

    @Schema(description = "Timestamp when work item completed")
    private Instant completedAt;

    @Schema(description = "Operational notes")
    private String notes;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;

    @Schema(description = "User who created the work item")
    private String createdBy;

    @Schema(description = "User who last updated the work item")
    private String updatedBy;

    @Schema(description = "Total number of child tasks")
    private int totalTasks;

    @Schema(description = "Completed child tasks count")
    private int completedTasks;
}
