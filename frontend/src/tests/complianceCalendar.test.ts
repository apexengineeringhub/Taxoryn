import { describe, it } from 'node:test';
import assert from 'node:assert';
import type {
  ComplianceObligationDto,
  ComplianceCalendarSummaryDto,
  ComplianceCycleTemplateDto,
  CreateComplianceObligationRequest,
  UpdateComplianceObligationRequest,
  UpdateObligationStatusRequest,
  AssignObligationRequest,
  ComplianceCalendarFilterParams,
  ComplianceObligationType,
  ComplianceObligationStatus,
  ComplianceRecurrenceType,
} from '../types/index.ts';

describe('Phase 14: Compliance Calendar & Recurring Compliance Cycle Foundation Tests', () => {
  it('should validate ComplianceObligationDto structure with dual statutory and internal target dates', () => {
    const obligation: ComplianceObligationDto = {
      id: 'ob-uuid-001',
      organizationId: 'org-uuid-001',
      clientId: 'client-uuid-001',
      clientDisplayName: 'Apex Tax Solutions Pvt Ltd',
      clientPan: 'AAACA1234D',
      clientServiceId: 'service-uuid-001',
      servicePeriodId: 'period-uuid-001',
      workflowId: 'workflow-uuid-001',
      obligationType: 'GST_RETURN',
      title: 'GSTR-3B July 2026',
      periodLabel: 'July 2026',
      financialYear: '2026-27',
      assessmentYear: '2027-28',
      statutoryDueDate: '2026-08-20',
      internalTargetDate: '2026-08-17',
      status: 'UPCOMING',
      priority: 'HIGH',
      assignedEmployeeId: 'emp-uuid-001',
      assignedEmployeeName: 'Anjali Sharma',
      daysRemaining: 15,
      isOverdue: false,
      isDueToday: false,
      waitingForClient: false,
      readyForFiling: false,
      createdAt: '2026-08-01T00:00:00Z',
      updatedAt: '2026-08-01T00:00:00Z',
    };

    assert.strictEqual(obligation.id, 'ob-uuid-001');
    assert.strictEqual(obligation.obligationType, 'GST_RETURN');
    assert.strictEqual(obligation.statutoryDueDate, '2026-08-20');
    assert.strictEqual(obligation.internalTargetDate, '2026-08-17');
    assert.strictEqual(obligation.status, 'UPCOMING');
    assert.strictEqual(obligation.priority, 'HIGH');
    assert.strictEqual(obligation.workflowId, 'workflow-uuid-001');
  });

  it('should validate ComplianceCalendarSummaryDto metrics', () => {
    const summary: ComplianceCalendarSummaryDto = {
      totalObligations: 45,
      dueToday: 3,
      upcoming: 25,
      overdue: 2,
      waitingForClient: 8,
      readyForFiling: 5,
      filed: 12,
      completed: 10,
      byType: {
        GST_RETURN: 20,
        ITR_FILING: 15,
        TDS_RETURN: 10,
      },
    };

    assert.strictEqual(summary.totalObligations, 45);
    assert.strictEqual(summary.dueToday, 3);
    assert.strictEqual(summary.overdue, 2);
    assert.strictEqual(summary.waitingForClient, 8);
    assert.strictEqual(summary.readyForFiling, 5);
    assert.strictEqual(summary.byType?.GST_RETURN, 20);
  });

  it('should validate ComplianceCycleTemplateDto structure', () => {
    const template: ComplianceCycleTemplateDto = {
      id: 'template-uuid-gst-monthly',
      serviceType: 'GST_COMPLIANCE',
      obligationType: 'GST_RETURN',
      recurrenceType: 'MONTHLY',
      templateName: 'Monthly GSTR-3B Filing Cycle',
      defaultDueDay: 20,
      defaultDueMonthOffset: 1,
      offsetDaysInternalTarget: 3,
      active: true,
      description: 'Standard monthly GSTR-3B return due 20th of subsequent month with 3-day internal target buffer',
    };

    assert.strictEqual(template.serviceType, 'GST_COMPLIANCE');
    assert.strictEqual(template.obligationType, 'GST_RETURN');
    assert.strictEqual(template.recurrenceType, 'MONTHLY');
    assert.strictEqual(template.defaultDueDay, 20);
    assert.strictEqual(template.defaultDueMonthOffset, 1);
    assert.strictEqual(template.offsetDaysInternalTarget, 3);
    assert.strictEqual(template.active, true);
  });

  it('should validate CreateComplianceObligationRequest payload', () => {
    const createReq: CreateComplianceObligationRequest = {
      clientId: 'client-uuid-001',
      clientServiceId: 'service-uuid-001',
      servicePeriodId: 'period-uuid-001',
      obligationType: 'ITR_FILING',
      title: 'ITR-6 Corporate Filing AY 2026-27',
      periodLabel: 'AY 2026-27',
      financialYear: '2025-26',
      assessmentYear: '2026-27',
      statutoryDueDate: '2026-10-31',
      internalTargetDate: '2026-10-25',
      priority: 'CRITICAL',
      assignedEmployeeId: 'emp-uuid-002',
      remarks: 'Requires tax audit sign-off under Sec 44AB prior to upload',
    };

    assert.strictEqual(createReq.clientId, 'client-uuid-001');
    assert.strictEqual(createReq.obligationType, 'ITR_FILING');
    assert.strictEqual(createReq.statutoryDueDate, '2026-10-31');
    assert.strictEqual(createReq.internalTargetDate, '2026-10-25');
    assert.strictEqual(createReq.priority, 'CRITICAL');
  });

  it('should validate UpdateObligationStatusRequest and status transitions', () => {
    const statusReq: UpdateObligationStatusRequest = {
      status: 'FILED',
      filedDate: '2026-08-19',
      acknowledgementNumber: 'ARN2708260001234',
      remarks: 'Filed via GSTN portal with OTP verification',
    };

    assert.strictEqual(statusReq.status, 'FILED');
    assert.strictEqual(statusReq.filedDate, '2026-08-19');
    assert.strictEqual(statusReq.acknowledgementNumber, 'ARN2708260001234');

    const assignReq: AssignObligationRequest = {
      assignedToId: 'emp-uuid-003',
    };
    assert.strictEqual(assignReq.assignedToId, 'emp-uuid-003');
  });

  it('should validate ComplianceCalendarFilterParams structure', () => {
    const filter: ComplianceCalendarFilterParams = {
      obligationType: 'TDS_RETURN',
      status: 'UPCOMING',
      priority: 'HIGH',
      dueFrom: '2026-08-01',
      dueTo: '2026-08-31',
      overdue: false,
      dueToday: false,
      waitingForClient: false,
      readyForFiling: false,
      page: 0,
      size: 25,
      sortBy: 'statutoryDueDate',
      sortDirection: 'ASC',
    };

    assert.strictEqual(filter.obligationType, 'TDS_RETURN');
    assert.strictEqual(filter.status, 'UPCOMING');
    assert.strictEqual(filter.sortBy, 'statutoryDueDate');
    assert.strictEqual(filter.sortDirection, 'ASC');
  });
});
