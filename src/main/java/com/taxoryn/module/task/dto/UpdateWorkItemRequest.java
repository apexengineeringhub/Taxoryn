package com.taxoryn.module.task.dto;

import com.taxoryn.module.task.model.WorkItemPriority;
import com.taxoryn.module.task.model.WorkItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request payload to update a Work Item")
public class UpdateWorkItemRequest {

    @Schema(description = "Work item title")
    private String title;

    @Schema(description = "Work item description")
    private String description;

    @Schema(description = "Location ID")
    private UUID locationId;

    @Schema(description = "Tax notice ID")
    private UUID noticeId;

    @Schema(description = "Status")
    private WorkItemStatus status;

    @Schema(description = "Priority")
    private WorkItemPriority priority;

    @Schema(description = "Assigned user ID")
    private UUID assignedUserId;

    @Schema(description = "Due date")
    private LocalDate dueDate;

    @Schema(description = "Operational notes")
    private String notes;
}
