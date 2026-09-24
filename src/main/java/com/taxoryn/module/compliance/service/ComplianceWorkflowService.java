package com.taxoryn.module.compliance.service;

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
import com.taxoryn.module.task.dto.TaskDto;

import java.util.UUID;

/**
 * Service interface for managing practitioner compliance execution workflows, checklists, and workbench.
 */
public interface ComplianceWorkflowService {

    ComplianceWorkflowDto getOrCreateWorkflowForObligation(UUID obligationId);

    ComplianceWorkflowDetailDto getWorkflowById(UUID workflowId);

    PagedResponse<ComplianceWorkflowDto> getWorkbenchWorkflows(ComplianceWorkbenchFilterRequest filter);

    ComplianceWorkbenchSummaryDto getWorkbenchSummary();

    ComplianceWorkflowDto assignWorkflow(UUID workflowId, AssignComplianceWorkflowRequest request);

    ComplianceWorkflowDto startWorkflow(UUID workflowId);

    ComplianceWorkflowDto waitClient(UUID workflowId, WaitClientWorkflowRequest request);

    ComplianceWorkflowDto resumeClient(UUID workflowId);

    ComplianceWorkflowDto submitReview(UUID workflowId);

    ComplianceWorkflowDto requestChanges(UUID workflowId, RequestWorkflowChangesRequest request);

    ComplianceWorkflowDto approveWorkflow(UUID workflowId, ApproveWorkflowRequest request);

    ComplianceWorkflowDto markFiled(UUID workflowId, MarkWorkflowFiledRequest request);

    ComplianceWorkflowDto completeWorkflow(UUID workflowId, CompleteComplianceWorkflowRequest request);

    ComplianceWorkflowChecklistItemDto updateChecklistItem(UUID workflowId, UUID itemId, UpdateChecklistItemRequest request);

    TaskDto createWorkflowTask(UUID workflowId, CreateWorkflowTaskRequest request);
}
