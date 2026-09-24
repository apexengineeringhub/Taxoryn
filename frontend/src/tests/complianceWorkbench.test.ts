import { describe, it } from 'node:test';
import assert from 'node:assert';
import type {
  ComplianceWorkflowDto,
  ComplianceWorkflowChecklistItemDto,
  ComplianceWorkflowDetailDto,
  ComplianceWorkbenchSummaryDto,
  ComplianceWorkbenchFilterParams,
  ComplianceWorkflowStatus,
  AssignComplianceWorkflowRequest,
  WaitClientWorkflowRequest,
  RequestWorkflowChangesRequest,
  ApproveWorkflowRequest,
  MarkWorkflowFiledRequest,
  CompleteComplianceWorkflowRequest,
  UpdateChecklistItemRequest,
  CreateWorkflowTaskRequest,
} from '../types/index.ts';

describe('Phase 15: Compliance Execution Workflow & Practitioner Workbench Tests', () => {
  it('should validate ComplianceWorkflowDto structure with 9-step progress and dual target dates', () => {
    const workflow: ComplianceWorkflowDto = {
      id: 'wf-uuid-001',
      organizationId: 'org-uuid-001',
      clientId: 'client-uuid-001',
      clientName: 'Apex Tax Solutions Pvt Ltd',
      pan: 'AAACA1234D',
      gstin: '27AAACA1234D1Z5',
      clientServiceId: 'service-uuid-001',
      serviceName: 'GST_RETURN',
      complianceObligationId: 'ob-uuid-001',
      obligationTitle: 'GSTR-3B Monthly Return - July 2026',
      obligationType: 'GST_RETURN',
      periodLabel: 'July 2026',
      statutoryDueDate: '2026-08-20',
      targetDate: '2026-08-17',
      statutoryStatus: 'IN_PROGRESS',
      workflowStatus: 'UNDER_REVIEW',
      priority: 'HIGH',
      assignedEmployeeId: 'emp-uuid-001',
      assignedEmployeeName: 'Rajesh Sharma',
      reviewerEmployeeId: 'emp-uuid-002',
      reviewerEmployeeName: 'CA Vikram Verma',
      waitingForClient: false,
      totalChecklistSteps: 9,
      completedChecklistSteps: 5,
      progressPercentage: 55,
      daysRemaining: 15,
      targetDaysRemaining: 12,
      isOverdue: false,
      createdAt: '2026-08-01T00:00:00Z',
      updatedAt: '2026-08-02T00:00:00Z',
    };

    assert.strictEqual(workflow.id, 'wf-uuid-001');
    assert.strictEqual(workflow.workflowStatus, 'UNDER_REVIEW');
    assert.strictEqual(workflow.totalChecklistSteps, 9);
    assert.strictEqual(workflow.completedChecklistSteps, 5);
    assert.strictEqual(workflow.progressPercentage, 55);
    assert.strictEqual(workflow.assignedEmployeeName, 'Rajesh Sharma');
    assert.strictEqual(workflow.reviewerEmployeeName, 'CA Vikram Verma');
    assert.strictEqual(workflow.waitingForClient, false);
  });

  it('should validate ComplianceWorkbenchSummaryDto cockpit counts', () => {
    const summary: ComplianceWorkbenchSummaryDto = {
      totalActive: 38,
      dueToday: 4,
      dueThisWeek: 11,
      overdue: 2,
      waitingForClient: 7,
      underReview: 5,
      readyForFiling: 6,
      completedThisMonth: 18,
      myAssigned: 12,
      myReviews: 4,
    };

    assert.strictEqual(summary.totalActive, 38);
    assert.strictEqual(summary.dueToday, 4);
    assert.strictEqual(summary.dueThisWeek, 11);
    assert.strictEqual(summary.overdue, 2);
    assert.strictEqual(summary.waitingForClient, 7);
    assert.strictEqual(summary.underReview, 5);
    assert.strictEqual(summary.readyForFiling, 6);
    assert.strictEqual(summary.completedThisMonth, 18);
    assert.strictEqual(summary.myAssigned, 12);
    assert.strictEqual(summary.myReviews, 4);
  });

  it('should validate 9 standard operational checklist checkpoints in sequence', () => {
    const checklistItems: ComplianceWorkflowChecklistItemDto[] = [
      { id: '1', workflowId: 'wf-1', itemKey: 'DOCUMENTS_REQUESTED', title: 'Request Supporting Documents', sequenceOrder: 1, isCompleted: true, isRequired: true },
      { id: '2', workflowId: 'wf-1', itemKey: 'DOCUMENTS_RECEIVED', title: 'Receive Client Documents', sequenceOrder: 2, isCompleted: true, isRequired: true },
      { id: '3', workflowId: 'wf-1', itemKey: 'DATA_VALIDATED', title: 'Validate & Reconcile Data', sequenceOrder: 3, isCompleted: true, isRequired: true },
      { id: '4', workflowId: 'wf-1', itemKey: 'COMPUTATION_PREPARED', title: 'Prepare Tax Computation', sequenceOrder: 4, isCompleted: true, isRequired: true },
      { id: '5', workflowId: 'wf-1', itemKey: 'PRACTITIONER_REVIEW_COMPLETED', title: 'Practitioner Internal Review', sequenceOrder: 5, isCompleted: true, isRequired: true },
      { id: '6', workflowId: 'wf-1', itemKey: 'CLIENT_CONFIRMATION_RECEIVED', title: 'Client Approval & Confirmation', sequenceOrder: 6, isCompleted: false, isRequired: true },
      { id: '7', workflowId: 'wf-1', itemKey: 'FILING_PACKAGE_PREPARED', title: 'Prepare Filing Package & JSON', sequenceOrder: 7, isCompleted: false, isRequired: true },
      { id: '8', workflowId: 'wf-1', itemKey: 'FILING_COMPLETED', title: 'File on Government Portal', sequenceOrder: 8, isCompleted: false, isRequired: true },
      { id: '9', workflowId: 'wf-1', itemKey: 'ACKNOWLEDGEMENT_RECEIVED', title: 'Download & Archive Acknowledgement', sequenceOrder: 9, isCompleted: false, isRequired: true },
    ];

    assert.strictEqual(checklistItems.length, 9);
    assert.strictEqual(checklistItems[0].itemKey, 'DOCUMENTS_REQUESTED');
    assert.strictEqual(checklistItems[8].itemKey, 'ACKNOWLEDGEMENT_RECEIVED');
    const completedCount = checklistItems.filter(i => i.isCompleted).length;
    assert.strictEqual(completedCount, 5);
  });

  it('should validate WaitClientWorkflowRequest and RequestWorkflowChangesRequest payload contracts', () => {
    const waitReq: WaitClientWorkflowRequest = {
      reason: 'Awaiting bank statements for July 2026',
      expectedResponseDate: '2026-08-14',
    };
    assert.strictEqual(waitReq.reason, 'Awaiting bank statements for July 2026');
    assert.strictEqual(waitReq.expectedResponseDate, '2026-08-14');

    const changesReq: RequestWorkflowChangesRequest = {
      reason: 'Table 4B ITC reversal Rule 42 missing',
    };
    assert.strictEqual(changesReq.reason, 'Table 4B ITC reversal Rule 42 missing');
  });

  it('should validate MarkWorkflowFiledRequest and CompleteComplianceWorkflowRequest payload contracts', () => {
    const filedReq: MarkWorkflowFiledRequest = {
      filedDate: '2026-08-19',
      acknowledgementNumber: 'AA2708260192837',
    };
    assert.strictEqual(filedReq.filedDate, '2026-08-19');
    assert.strictEqual(filedReq.acknowledgementNumber, 'AA2708260192837');

    const completeReq: CompleteComplianceWorkflowRequest = {
      acknowledgementNumber: 'AA2708260192837',
      notes: 'Delivered to client via email portal',
    };
    assert.strictEqual(completeReq.acknowledgementNumber, 'AA2708260192837');
    assert.strictEqual(completeReq.notes, 'Delivered to client via email portal');
  });
});
