package com.taxoryn.module.task.dto;

import com.taxoryn.module.task.model.WorkItemPriority;
import com.taxoryn.module.task.model.WorkItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Request payload to create a new Work Item")
public class CreateWorkItemRequest {

    @NotBlank(message = "Title is required")
    @Schema(description = "Work item title", example = "Q4 GSTR-1 Verification & Filing", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "Work item description")
    private String description;

    @Schema(description = "Client ID associated with this work item")
    private UUID clientId;

    @Schema(description = "Location ID")
    private UUID locationId;

    @Schema(description = "Client service ID")
    private UUID clientServiceId;

    @Schema(description = "Compliance workflow ID")
    private UUID workflowId;

    @Schema(description = "Initial status")
    @Builder.Default
    private WorkItemStatus status = WorkItemStatus.TODO;

    @Schema(description = "Work priority")
    @Builder.Default
    private WorkItemPriority priority = WorkItemPriority.MEDIUM;

    @Schema(description = "Assigned user ID")
    private UUID assignedUserId;

    @Schema(description = "Due date")
    private LocalDate dueDate;

    @Schema(description = "Operational notes")
    private String notes;
}
