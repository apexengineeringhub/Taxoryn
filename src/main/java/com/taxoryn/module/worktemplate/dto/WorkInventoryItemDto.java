package com.taxoryn.module.worktemplate.dto;

import com.taxoryn.module.worktemplate.model.WorkInventoryStatus;
import com.taxoryn.module.worktemplate.model.WorkInventoryType;
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
@Schema(description = "Unified work operating item projected across work instances, compliance obligations, and tasks")
public class WorkInventoryItemDto {

    @Schema(description = "Underlying entity unique identifier")
    private UUID id;

    @Schema(description = "Work item category")
    private WorkInventoryType workType;

    @Schema(description = "Title or description of the work item")
    private String title;

    @Schema(description = "Normalized lifecycle status")
    private WorkInventoryStatus normalizedStatus;

    @Schema(description = "Raw entity-specific status code string")
    private String rawStatus;

    @Schema(description = "Priority level: LOW, MEDIUM, HIGH, URGENT, CRITICAL")
    private String priority;

    @Schema(description = "Client unique identifier")
    private UUID clientId;

    @Schema(description = "Client display or company name")
    private String clientName;

    @Schema(description = "Client PAN if available")
    private String clientPan;

    @Schema(description = "Linked engagement unique identifier if applicable")
    private UUID engagementId;

    @Schema(description = "Linked engagement name")
    private String engagementName;

    @Schema(description = "Linked compliance obligation identifier if applicable")
    private UUID complianceObligationId;

    @Schema(description = "Statutory compliance rule code if applicable (e.g. GST_GSTR3B_MONTHLY)")
    private String ruleCode;

    @Schema(description = "Target or statutory due date")
    private LocalDate dueDate;

    @Schema(description = "Period start date if applicable")
    private LocalDate periodStart;

    @Schema(description = "Period end date if applicable")
    private LocalDate periodEnd;

    @Schema(description = "Assigned user unique identifier")
    private UUID assignedUserId;

    @Schema(description = "Assigned user full name")
    private String assignedUserName;

    @Schema(description = "Reviewer user unique identifier")
    private UUID reviewerUserId;

    @Schema(description = "Reviewer user full name")
    private String reviewerUserName;

    @Schema(description = "Whether this work item is actively blocked")
    private boolean blocked;

    @Schema(description = "Explanation or blocker reason")
    private String blockedReason;

    @Schema(description = "Next actionable step")
    private String nextActionableStep;

    @Schema(description = "Total associated actionable subtasks")
    private int totalTasks;

    @Schema(description = "Total completed subtasks")
    private int completedTasks;

    @Schema(description = "Timestamp when work record was created")
    private Instant createdAt;

    @Schema(description = "Timestamp when work record was completed if finished")
    private Instant completedAt;
}
