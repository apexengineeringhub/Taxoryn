import { describe, it } from 'node:test';
import assert from 'node:assert';
import type {
  ComplianceWorkItem,
  ComplianceWorkType,
  ComplianceWorkStatus,
  CreateComplianceWorkItemRequest,
  UpdateComplianceWorkItemRequest,
  UpdateComplianceWorkStatusRequest,
  AssignComplianceWorkRequest,
} from '../types/index.ts';

describe('Phase 12: Compliance Workflow Foundation & Work Items Tests', () => {
  it('should validate ComplianceWorkItem structure with statutory and internal target dates', () => {
    const workItem: ComplianceWorkItem = {
      id: 'work-item-101',
      organizationId: 'org-uuid-1',
      clientId: 'client-uuid-1',
      clientName: 'Apex Engineering Ltd',
      clientServiceId: 'service-uuid-1',
      serviceName: 'GST Compliance & Filing',
      serviceType: 'GST_COMPLIANCE',
      workType: 'GST_RETURN',
      title: 'GSTR-3B September 2026',
      description: 'Monthly summary return computation and ITC validation',
      financialYear: '2026-27',
      compliancePeriod: 'September 2026',
      status: 'IN_PREPARATION',
      statutoryDueDate: '2026-10-20',
      internalTargetDate: '2026-10-15',
      assignedEmployeeId: 'emp-uuid-1',
      assignedEmployeeName: 'Ramesh Sharma',
      reviewerEmployeeId: 'emp-uuid-2',
      reviewerEmployeeName: 'Priya Mehta',
      startedAt: '2026-10-05T10:00:00Z',
      overdue: false,
    };

    assert.strictEqual(workItem.id, 'work-item-101');
    assert.strictEqual(workItem.workType, 'GST_RETURN');
    assert.strictEqual(workItem.status, 'IN_PREPARATION');
    assert.strictEqual(workItem.statutoryDueDate, '2026-10-20');
    assert.strictEqual(workItem.internalTargetDate, '2026-10-15');
    assert.strictEqual(workItem.assignedEmployeeName, 'Ramesh Sharma');
    assert.strictEqual(workItem.reviewerEmployeeName, 'Priya Mehta');
  });

  it('should construct valid CreateComplianceWorkItemRequest payload', () => {
    const req: CreateComplianceWorkItemRequest = {
      clientId: 'client-uuid-123',
      clientServiceId: 'service-uuid-456',
      workType: 'ITR_RETURN',
      title: 'ITR-6 Corporate Filing AY 2026-27',
      description: 'Corporate income tax return preparation',
      financialYear: '2025-26',
      assessmentYear: '2026-27',
      compliancePeriod: 'Annual',
      statutoryDueDate: '2026-10-31',
      internalTargetDate: '2026-10-25',
      assignedEmployeeId: 'emp-uuid-789',
      reviewerEmployeeId: 'emp-uuid-999',
    };

    assert.strictEqual(req.clientId, 'client-uuid-123');
    assert.strictEqual(req.clientServiceId, 'service-uuid-456');
    assert.strictEqual(req.workType, 'ITR_RETURN');
    assert.strictEqual(req.assessmentYear, '2026-27');
  });

  it('should format UpdateComplianceWorkStatusRequest and support lifecycle progression', () => {
    const statusReq: UpdateComplianceWorkStatusRequest = {
      status: 'READY_TO_FILE',
      notes: 'Reviewed and approved by partner, client authorization received',
    };

    assert.strictEqual(statusReq.status, 'READY_TO_FILE');
    assert.ok(statusReq.notes?.includes('approved by partner'));
  });

  it('should format AssignComplianceWorkRequest with practitioner and reviewer', () => {
    const assignReq: AssignComplianceWorkRequest = {
      assignedEmployeeId: 'emp-uuid-new-preparer',
      reviewerEmployeeId: 'emp-uuid-partner',
      notes: 'Reassigned for expedited tax audit review',
    };

    assert.strictEqual(assignReq.assignedEmployeeId, 'emp-uuid-new-preparer');
    assert.strictEqual(assignReq.reviewerEmployeeId, 'emp-uuid-partner');
  });

  it('should verify all 7 ComplianceWorkType enum values are well-defined', () => {
    const types: ComplianceWorkType[] = [
      'GST_RETURN',
      'ITR_RETURN',
      'TDS_RETURN',
      'TAX_NOTICE',
      'COMPLIANCE_TASK',
      'DOCUMENT_COLLECTION',
      'OTHER',
    ];

    assert.strictEqual(types.length, 7);
    assert.ok(types.includes('GST_RETURN'));
    assert.ok(types.includes('TAX_NOTICE'));
    assert.ok(types.includes('DOCUMENT_COLLECTION'));
  });

  it('should verify all 9 ComplianceWorkStatus lifecycle values are well-defined', () => {
    const statuses: ComplianceWorkStatus[] = [
      'NOT_STARTED',
      'DOCUMENTS_PENDING',
      'IN_PREPARATION',
      'IN_REVIEW',
      'READY_TO_FILE',
      'FILED',
      'COMPLETED',
      'ON_HOLD',
      'CANCELLED',
    ];

    assert.strictEqual(statuses.length, 9);
    assert.ok(statuses.includes('NOT_STARTED'));
    assert.ok(statuses.includes('READY_TO_FILE'));
    assert.ok(statuses.includes('FILED'));
    assert.ok(statuses.includes('ON_HOLD'));
  });
});
