package com.taxoryn.module.task.dto;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.module.task.model.WorkItemPriority;
import com.taxoryn.module.task.model.WorkItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Filter and pagination criteria for querying Work Items")
public class WorkItemFilterRequest extends PageRequestDto {

    @Schema(description = "Filter by client ID")
    private UUID clientId;

    @Schema(description = "Filter by location ID")
    private UUID locationId;

    @Schema(description = "Filter by compliance workflow ID")
    private UUID workflowId;

    @Schema(description = "Filter by client service ID")
    private UUID clientServiceId;

    @Schema(description = "Filter by assigned user ID")
    private UUID assignedUserId;

    @Schema(description = "Filter by status")
    private WorkItemStatus status;

    @Schema(description = "Filter by priority")
    private WorkItemPriority priority;

    @Schema(description = "Filter due on or before date")
    private LocalDate dueBefore;

    @Schema(description = "Filter due on or after date")
    private LocalDate dueAfter;

    @Schema(description = "Free text search in title and description")
    private String search;
}
