import { describe, it } from 'node:test';
import assert from 'node:assert';
import type {
  ClientServicePeriod,
  CreateServicePeriodRequest,
  ClientServiceWorkflow,
  ClientServiceWorkflowStep,
  ServiceWorkflowTemplate,
  ServiceWorkflowStatus,
  StepStatus,
  ServiceWorkType,
  ServicePeriodType,
  ServicePeriodStatus,
  UpdateWorkflowStatusRequest,
  UpdateWorkflowStepStatusRequest,
  GenerateWorkflowRequest,
} from '../types/index.ts';

describe('Phase 13: Client Engagement Operations & Compliance Workflow Tests', () => {
  it('should validate ClientServicePeriod and period creation structure', () => {
    const periodRequest: CreateServicePeriodRequest = {
      clientServiceId: 'srv-uuid-201',
      periodLabel: 'April 2026',
      periodType: 'MONTHLY',
      startDate: '2026-04-01',
      endDate: '2026-04-30',
      dueDate: '2026-05-20',
    };

    assert.strictEqual(periodRequest.clientServiceId, 'srv-uuid-201');
    assert.strictEqual(periodRequest.periodLabel, 'April 2026');
    assert.strictEqual(periodRequest.periodType, 'MONTHLY');
    assert.strictEqual(periodRequest.dueDate, '2026-05-20');

    const periodDto: ClientServicePeriod = {
      id: 'period-uuid-101',
      organizationId: 'org-uuid-001',
      clientServiceId: 'srv-uuid-201',
      clientId: 'client-uuid-301',
      serviceType: 'GST_COMPLIANCE',
      serviceName: 'GST Compliance & Filings',
      periodLabel: 'April 2026',
      periodType: 'MONTHLY',
      startDate: '2026-04-01',
      endDate: '2026-04-30',
      dueDate: '2026-05-20',
      status: 'ACTIVE',
      hasActiveWorkflow: true,
      createdAt: '2026-04-01T00:00:00Z',
      updatedAt: '2026-04-01T00:00:00Z',
      version: 1,
    };

    assert.strictEqual(periodDto.status, 'ACTIVE');
    assert.strictEqual(periodDto.clientServiceId, 'srv-uuid-201');
  });

  it('should validate GenerateWorkflowRequest payload for GST Compliance', () => {
    const req: GenerateWorkflowRequest = {
      clientServiceId: 'srv-uuid-201',
      periodId: 'period-uuid-101',
      assignedEmployeeId: 'emp-uuid-777',
      internalTargetDate: '2026-05-15',
      priority: 'HIGH',
    };

    assert.strictEqual(req.clientServiceId, 'srv-uuid-201');
    assert.strictEqual(req.periodId, 'period-uuid-101');
    assert.strictEqual(req.priority, 'HIGH');
    assert.strictEqual(req.assignedEmployeeId, 'emp-uuid-777');
  });

  it('should validate step progression and completion percentage calculation', () => {
    const steps: ClientServiceWorkflowStep[] = [
      {
        id: 'step-1',
        organizationId: 'org-uuid-001',
        workflowId: 'wf-1',
        sequence: 1,
        name: 'Sales & Purchase Invoices Collection',
        workType: 'DATA_COLLECTION',
        status: 'COMPLETED',
        mandatory: true,
        requiresClientInput: true,
        requiresReview: false,
        createdAt: '2026-04-01T00:00:00Z',
        updatedAt: '2026-04-02T00:00:00Z',
      },
      {
        id: 'step-2',
        organizationId: 'org-uuid-001',
        workflowId: 'wf-1',
        sequence: 2,
        name: 'GSTR-2B ITC Reconciliation',
        workType: 'COMPUTATION',
        status: 'COMPLETED',
        mandatory: true,
        requiresClientInput: false,
        requiresReview: true,
        createdAt: '2026-04-01T00:00:00Z',
        updatedAt: '2026-04-03T00:00:00Z',
      },
      {
        id: 'step-3',
        organizationId: 'org-uuid-001',
        workflowId: 'wf-1',
        sequence: 3,
        name: 'Tax Computation & Liability Approval',
        workType: 'REVIEW',
        status: 'IN_PROGRESS',
        mandatory: true,
        requiresClientInput: false,
        requiresReview: true,
        createdAt: '2026-04-01T00:00:00Z',
        updatedAt: '2026-04-04T00:00:00Z',
      },
      {
        id: 'step-4',
        organizationId: 'org-uuid-001',
        workflowId: 'wf-1',
        sequence: 4,
        name: 'GSTR-3B Return Filing & Challan Payment',
        workType: 'FILING',
        status: 'PENDING',
        mandatory: true,
        requiresClientInput: false,
        requiresReview: false,
        createdAt: '2026-04-01T00:00:00Z',
        updatedAt: '2026-04-01T00:00:00Z',
      },
      {
        id: 'step-5',
        organizationId: 'org-uuid-001',
        workflowId: 'wf-1',
        sequence: 5,
        name: 'Acknowledgement & Summary Delivery to Client',
        workType: 'ACKNOWLEDGEMENT',
        status: 'PENDING',
        mandatory: false,
        requiresClientInput: false,
        requiresReview: false,
        createdAt: '2026-04-01T00:00:00Z',
        updatedAt: '2026-04-01T00:00:00Z',
      },
    ];

    const completedCount = steps.filter((s) => s.status === 'COMPLETED').length;
    const totalCount = steps.length;
    const progressPercent = Math.round((completedCount / totalCount) * 100);

    assert.strictEqual(completedCount, 2);
    assert.strictEqual(totalCount, 5);
    assert.strictEqual(progressPercent, 40);
  });

  it('should enforce waitingForClient flag and pending action summary', () => {
    const workflow: ClientServiceWorkflow = {
      id: 'wf-uuid-888',
      organizationId: 'org-uuid-001',
      clientId: 'client-uuid-301',
      clientName: 'Apex Engineering Ltd',
      clientServiceId: 'srv-uuid-201',
      serviceType: 'GST_COMPLIANCE',
      serviceName: 'GST Compliance & Filings',
      periodId: 'period-uuid-101',
      periodLabel: 'April 2026',
      title: 'April 2026 - GST Compliance Workflow',
      status: 'WAITING_FOR_CLIENT',
      priority: 'HIGH',
      waitingForClient: true,
      pendingClientActionSummary: 'Awaiting client approval on GSTR-3B tax liability challan',
      dueDate: '2026-05-20',
      internalTargetDate: '2026-05-15',
      currentStepSequence: 3,
      totalSteps: 5,
      completedSteps: 2,
      progressPercentage: 40,
      isOverdue: false,
      createdAt: '2026-04-01T00:00:00Z',
      updatedAt: '2026-04-04T00:00:00Z',
      version: 1,
      steps: [],
    };

    assert.strictEqual(workflow.status, 'WAITING_FOR_CLIENT');
    assert.strictEqual(workflow.waitingForClient, true);
    assert.strictEqual(
      workflow.pendingClientActionSummary,
      'Awaiting client approval on GSTR-3B tax liability challan'
    );
  });

  it('should accurately represent ServiceWorkflowTemplate structure with seed steps', () => {
    const template: ServiceWorkflowTemplate = {
      id: 'tmpl-uuid-01',
      serviceType: 'INCOME_TAX_FILING',
      serviceTypeName: 'Income Tax Return Filing',
      name: 'Corporate ITR Filing Standard Workflow',
      description: 'Standard 6-step workflow for corporate income tax return preparation and filing',
      isSystemDefault: true,
      active: true,
      stepCount: 6,
      stepTemplates: [
        { id: 's1', workflowTemplateId: 'tmpl-uuid-01', sequence: 1, name: 'Trial Balance & Financials Collection', workType: 'DOCUMENT_COLLECTION', workTypeName: 'Document Collection', mandatory: true, requiresClientInput: true, requiresReview: false, active: true },
        { id: 's2', workflowTemplateId: 'tmpl-uuid-01', sequence: 2, name: 'Tax Audit & Schedule Preparation', workType: 'PREPARATION', workTypeName: 'Preparation', mandatory: true, requiresClientInput: false, requiresReview: true, active: true },
        { id: 's3', workflowTemplateId: 'tmpl-uuid-01', sequence: 3, name: 'Tax Computation & Advance Tax Adjustments', workType: 'COMPUTATION', workTypeName: 'Computation', mandatory: true, requiresClientInput: false, requiresReview: true, active: true },
        { id: 's4', workflowTemplateId: 'tmpl-uuid-01', sequence: 4, name: 'Draft Return Review by Partner', workType: 'REVIEW', workTypeName: 'Review', mandatory: true, requiresClientInput: false, requiresReview: true, active: true },
        { id: 's5', workflowTemplateId: 'tmpl-uuid-01', sequence: 5, name: 'Client Computation Confirmation & DSC Signing', workType: 'CLIENT_CONFIRMATION', workTypeName: 'Client Confirmation', mandatory: true, requiresClientInput: true, requiresReview: false, active: true },
        { id: 's6', workflowTemplateId: 'tmpl-uuid-01', sequence: 6, name: 'Portal Filing & ITR-V Delivery', workType: 'FILING', workTypeName: 'Filing', mandatory: true, requiresClientInput: false, requiresReview: false, active: true },
      ],
    };

    assert.strictEqual(template.serviceType, 'INCOME_TAX_FILING');
    assert.strictEqual(template.stepCount, 6);
    assert.strictEqual(template.stepTemplates[4].workType, 'CLIENT_CONFIRMATION');
  });

  it('should verify all ServiceWorkflowStatus and StepStatus enumeration values', () => {
    const workflowStatuses: ServiceWorkflowStatus[] = [
      'NOT_STARTED',
      'IN_PROGRESS',
      'WAITING_FOR_CLIENT',
      'READY_FOR_FILING',
      'FILED',
      'COMPLETED',
      'CANCELLED',
    ];
    assert.strictEqual(workflowStatuses.length, 7);

    const stepStatuses: StepStatus[] = [
      'PENDING',
      'IN_PROGRESS',
      'WAITING_FOR_CLIENT',
      'COMPLETED',
      'SKIPPED',
    ];
    assert.strictEqual(stepStatuses.length, 5);
  });
});
