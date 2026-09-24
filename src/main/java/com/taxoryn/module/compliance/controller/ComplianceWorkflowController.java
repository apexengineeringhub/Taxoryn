package com.taxoryn.module.compliance.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.compliance.dto.ApproveWorkflowRequest;
import com.taxoryn.module.compliance.dto.AssignComplianceWorkflowRequest;
import com.taxoryn.module.compliance.dto.CompleteComplianceWorkflowRequest;
import com.taxoryn.module.compliance.dto.ComplianceWorkbenchFilterRequest;
import com.taxoryn.module.compliance.dto.ComplianceWorkbenchSummaryDto;
import com.taxoryn.module.compliance.dto.ComplianceWorkflowChecklistItemDto;
import com.taxoryn.module.compliance.dto.ComplianceWorkflowDetailDto;
import com.taxoryn.module.compliance.dto.ComplianceWorkflowDto;
import com.taxoryn.module.compliance.dto.CreateWorkflowTaskRequest;
import com.taxoryn.module.compliance.dto.MarkWorkflowFiledRequest;
import com.taxoryn.module.compliance.dto.RequestWorkflowChangesRequest;
import com.taxoryn.module.compliance.dto.UpdateChecklistItemRequest;
import com.taxoryn.module.compliance.dto.WaitClientWorkflowRequest;
import com.taxoryn.module.compliance.service.ComplianceWorkflowService;
import com.taxoryn.module.task.dto.TaskDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/compliance", "/api/compliance"})
@RequiredArgsConstructor
@Tag(name = "Compliance Execution Workflow & Practitioner Workbench", description = "Practitioner operational execution workflows, 9-step checklists, maker-checker review, and workbench lifecycle management")
@SecurityRequirement(name = "BearerAuth")
public class ComplianceWorkflowController {

    private final ComplianceWorkflowService workflowService;

    // =========================================================================
    // 1. Workbench Workflows & Summary
    // =========================================================================

    @GetMapping("/workbench")
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'CLIENT_VIEW', 'GST_VIEW', 'ITR_VIEW', 'TDS_VIEW', 'NOTICE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "Get practitioner workbench workflows", description = "Retrieves paginated operational compliance execution workflows filtered by view type, status, staff assignment, target/statutory dates, or client.")
    public ResponseEntity<ApiResponse<PagedResponse<ComplianceWorkflowDto>>> getWorkbenchWorkflows(
            @Valid @ModelAttribute ComplianceWorkbenchFilterRequest filter
    ) {
        PagedResponse<ComplianceWorkflowDto> response = workflowService.getWorkbenchWorkflows(filter);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/workbench/summary")
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'CLIENT_VIEW', 'GST_VIEW', 'ITR_VIEW', 'TDS_VIEW', 'NOTICE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "Get workbench summary metrics", description = "Retrieves count badges for total active, due today, due this week, overdue, waiting for client, under review, and staff queues.")
    public ResponseEntity<ApiResponse<ComplianceWorkbenchSummaryDto>> getWorkbenchSummary() {
        ComplianceWorkbenchSummaryDto summary = workflowService.getWorkbenchSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    // =========================================================================
    // 2. Workflow Creation & Details
    // =========================================================================

    @PostMapping("/obligations/{obligationId}/workflow")
    @PreAuthorize("hasAnyAuthority('TASK_CREATE', 'CLIENT_EDIT', 'GST_EDIT', 'ITR_EDIT', 'TDS_EDIT', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "Get or create workflow for obligation", description = "Idempotently returns or initializes an operational compliance execution workflow with default 9 checklist items for a statutory obligation.")
    public ResponseEntity<ApiResponse<ComplianceWorkflowDto>> getOrCreateWorkflowForObligation(
            @PathVariable UUID obligationId
    ) {
        ComplianceWorkflowDto workflow = workflowService.getOrCreateWorkflowForObligation(obligationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Compliance workflow initialized", workflow));
    }

    @GetMapping("/workflows/{workflowId}")
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'CLIENT_VIEW', 'GST_VIEW', 'ITR_VIEW', 'TDS_VIEW', 'NOTICE_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "Get detailed compliance workflow", description = "Retrieves detailed execution workflow including operational checkpoints, linked tasks, maker-checker notes, and filing metadata.")
    public ResponseEntity<ApiResponse<ComplianceWorkflowDetailDto>> getWorkflowById(
            @PathVariable UUID workflowId
    ) {
        ComplianceWorkflowDetailDto detail = workflowService.getWorkflowById(workflowId);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    // =========================================================================
    // 3. Operational State Transitions
    // =========================================================================

    @PostMapping("/workflows/{workflowId}/assign")
    @PreAuthorize("hasAnyAuthority('TASK_ASSIGN', 'CLIENT_EDIT', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_PRACTITIONER')")
    @Operation(summary = "Assign workflow staff and reviewer", description = "Assigns preparer, reviewer, priority, and internal target completion date.")
    public ResponseEntity<ApiResponse<ComplianceWorkflowDto>> assignWorkflow(
            @PathVariable UUID workflowId,
            @Valid @RequestBody AssignComplianceWorkflowRequest request
    ) {
        ComplianceWorkflowDto updated = workflowService.assignWorkflow(workflowId, request);
        return ResponseEntity.ok(ApiResponse.success("Workflow assignments updated", updated));
    }

    @PostMapping("/workflows/{workflowId}/start")
    @PreAuthorize("hasAnyAuthority('TASK_EDIT', 'CLIENT_EDIT', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_STAFF', 'ROLE_PRACTITIONER', 'ROLE_MANAGER')")
    @Operation(summary = "Start workflow execution", description = "Transitions workflow from CREATED / READY to IN_PROGRESS.")
    public ResponseEntity<ApiResponse<ComplianceWorkflowDto>> startWorkflow(
            @PathVariable UUID workflowId
    ) {
        ComplianceWorkflowDto updated = workflowService.startWorkflow(workflowId);
        return ResponseEntity.ok(ApiResponse.success("Workflow started", updated));
    }

    @PostMapping("/workflows/{workflowId}/wait-client")
    @PreAuthorize("hasAnyAuthority('TASK_EDIT', 'CLIENT_EDIT', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_STAFF', 'ROLE_PRACTITIONER', 'ROLE_MANAGER')")
    @Operation(summary = "Mark workflow waiting for client", description = "Pauses practitioner execution while awaiting client documents, inputs, or signature.")
    public ResponseEntity<ApiResponse<ComplianceWorkflowDto>> waitClient(
            @PathVariable UUID workflowId,
            @Valid @RequestBody WaitClientWorkflowRequest request
    ) {
        ComplianceWorkflowDto updated = workflowService.waitClient(workflowId, request);
        return ResponseEntity.ok(ApiResponse.success("Workflow set to waiting for client", updated));
    }

    @PostMapping("/workflows/{workflowId}/resume-client")
    @PreAuthorize("hasAnyAuthority('TASK_EDIT', 'CLIENT_EDIT', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_STAFF', 'ROLE_PRACTITIONER', 'ROLE_MANAGER')")
    @Operation(summary = "Resume workflow from waiting state", description = "Resumes practitioner execution back to IN_PROGRESS after client documents or response received.")
    public ResponseEntity<ApiResponse<ComplianceWorkflowDto>> resumeClient(
            @PathVariable UUID workflowId
    ) {
        ComplianceWorkflowDto updated = workflowService.resumeClient(workflowId);
        return ResponseEntity.ok(ApiResponse.success("Workflow resumed to IN_PROGRESS", updated));
    }

    @PostMapping("/workflows/{workflowId}/submit-review")
    @PreAuthorize("hasAnyAuthority('TASK_EDIT', 'CLIENT_EDIT', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_STAFF', 'ROLE_PRACTITIONER', 'ROLE_MANAGER')")
    @Operation(summary = "Submit workflow for review", description = "Submits prepared return computation for maker-checker review by reviewer.")
    public ResponseEntity<ApiResponse<ComplianceWorkflowDto>> submitReview(
            @PathVariable UUID workflowId
    ) {
        ComplianceWorkflowDto updated = workflowService.submitReview(workflowId);
        return ResponseEntity.ok(ApiResponse.success("Workflow submitted for review", updated));
    }

    @PostMapping("/workflows/{workflowId}/request-changes")
    @PreAuthorize("hasAnyAuthority('TASK_EDIT', 'CLIENT_EDIT', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_PRACTITIONER')")
    @Operation(summary = "Request revisions / changes", description = "Reviewer requests corrections from preparer before filing.")
    public ResponseEntity<ApiResponse<ComplianceWorkflowDto>> requestChanges(
            @PathVariable UUID workflowId,
            @Valid @RequestBody RequestWorkflowChangesRequest request
    ) {
        ComplianceWorkflowDto updated = workflowService.requestChanges(workflowId, request);
        return ResponseEntity.ok(ApiResponse.success("Changes requested on workflow", updated));
    }

    @PostMapping("/workflows/{workflowId}/approve")
    @PreAuthorize("hasAnyAuthority('TASK_EDIT', 'CLIENT_EDIT', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_PRACTITIONER')")
    @Operation(summary = "Approve workflow", description = "Reviewer approves computation; workflow transitions to READY_FOR_FILING.")
    public ResponseEntity<ApiResponse<ComplianceWorkflowDto>> approveWorkflow(
            @PathVariable UUID workflowId,
            @Valid @RequestBody ApproveWorkflowRequest request
    ) {
        ComplianceWorkflowDto updated = workflowService.approveWorkflow(workflowId, request);
        return ResponseEntity.ok(ApiResponse.success("Workflow approved and marked READY_FOR_FILING", updated));
    }

    @PostMapping("/workflows/{workflowId}/mark-filed")
    @PreAuthorize("hasAnyAuthority('TASK_EDIT', 'CLIENT_EDIT', 'GST_EDIT', 'ITR_EDIT', 'TDS_EDIT', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_STAFF', 'ROLE_PRACTITIONER', 'ROLE_MANAGER')")
    @Operation(summary = "Record government filing", description = "Records official filing date and portal acknowledgement ARN.")
    public ResponseEntity<ApiResponse<ComplianceWorkflowDto>> markFiled(
            @PathVariable UUID workflowId,
            @Valid @RequestBody MarkWorkflowFiledRequest request
    ) {
        ComplianceWorkflowDto updated = workflowService.markFiled(workflowId, request);
        return ResponseEntity.ok(ApiResponse.success("Filing recorded successfully", updated));
    }

    @PostMapping("/workflows/{workflowId}/complete")
    @PreAuthorize("hasAnyAuthority('TASK_EDIT', 'CLIENT_EDIT', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_STAFF', 'ROLE_PRACTITIONER', 'ROLE_MANAGER')")
    @Operation(summary = "Complete workflow", description = "Finalizes workflow after acknowledgement archiving.")
    public ResponseEntity<ApiResponse<ComplianceWorkflowDto>> completeWorkflow(
            @PathVariable UUID workflowId,
            @Valid @RequestBody CompleteComplianceWorkflowRequest request
    ) {
        ComplianceWorkflowDto updated = workflowService.completeWorkflow(workflowId, request);
        return ResponseEntity.ok(ApiResponse.success("Workflow completed", updated));
    }

    // =========================================================================
    // 4. Checklist Item Update & Task Creation
    // =========================================================================

    @PatchMapping("/workflows/{workflowId}/checklist/{itemId}")
    @PreAuthorize("hasAnyAuthority('TASK_EDIT', 'CLIENT_EDIT', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_STAFF', 'ROLE_PRACTITIONER', 'ROLE_MANAGER')")
    @Operation(summary = "Update checklist item", description = "Toggles completion status or notes for an operational checkpoint.")
    public ResponseEntity<ApiResponse<ComplianceWorkflowChecklistItemDto>> updateChecklistItem(
            @PathVariable UUID workflowId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateChecklistItemRequest request
    ) {
        ComplianceWorkflowChecklistItemDto item = workflowService.updateChecklistItem(workflowId, itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Checklist item updated", item));
    }

    @PostMapping("/workflows/{workflowId}/tasks")
    @PreAuthorize("hasAnyAuthority('TASK_CREATE', 'CLIENT_EDIT', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_STAFF', 'ROLE_PRACTITIONER', 'ROLE_MANAGER')")
    @Operation(summary = "Create task linked to workflow", description = "Creates a discrete task in Task module linked to this compliance workflow.")
    public ResponseEntity<ApiResponse<TaskDto>> createWorkflowTask(
            @PathVariable UUID workflowId,
            @Valid @RequestBody CreateWorkflowTaskRequest request
    ) {
        TaskDto task = workflowService.createWorkflowTask(workflowId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Task created for workflow", task));
    }
}
