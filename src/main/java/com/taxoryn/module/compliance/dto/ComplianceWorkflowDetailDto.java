package com.taxoryn.module.compliance.dto;

import com.taxoryn.module.task.dto.TaskDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed Compliance Execution Workflow View")
public class ComplianceWorkflowDetailDto {

    @Schema(description = "Base workflow metadata")
    private ComplianceWorkflowDto workflow;

    @Schema(description = "Execution readiness checklist items in sequence order")
    @Builder.Default
    private List<ComplianceWorkflowChecklistItemDto> checklistItems = new ArrayList<>();

    @Schema(description = "Associated operational tasks in Task module")
    @Builder.Default
    private List<TaskDto> linkedTasks = new ArrayList<>();

    @Schema(description = "Review notes recorded by reviewer")
    private String reviewNotes;

    @Schema(description = "Reason recorded when reviewer requests changes")
    private String changesRequestedReason;

    @Schema(description = "Name/email of reviewer who approved the workflow")
    private String approvedBy;

    @Schema(description = "Name/email of user who recorded waiting-for-client")
    private String waitingRequestedBy;

    @Schema(description = "Name/email of user who recorded government filing")
    private String filedBy;

    @Schema(description = "Name/email of user who finalized completion")
    private String completedBy;

    @Schema(description = "General practitioner notes and remarks")
    private String notes;
}
