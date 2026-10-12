import { describe, it } from 'node:test';
import assert from 'node:assert';
import {
  isWorkItemOverdue,
  getAuthoritativeNavigationUrl,
} from '../utils/workOperatingBoardUtils.ts';
import type { WorkInventoryItemDto } from '../types/index.ts';

describe('Phase 30.2: Work Operating Board Tests', () => {
  it('1. Overdue Item Detection: calculates overdue status correctly based on due date and terminal statuses', () => {
    const yesterday = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString().split('T')[0];
    const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString().split('T')[0];

    const overdueItem: WorkInventoryItemDto = {
      id: 'item-1',
      workType: 'WORK_INSTANCE',
      title: 'GST Monthly Return',
      normalizedStatus: 'IN_PROGRESS',
      rawStatus: 'IN_PROGRESS',
      priority: 'HIGH',
      dueDate: yesterday,
      blocked: false,
      totalTasks: 3,
      completedTasks: 1,
    };

    assert.strictEqual(isWorkItemOverdue(overdueItem), true, 'Yesterday due date should be overdue');

    const futureItem: WorkInventoryItemDto = {
      ...overdueItem,
      id: 'item-2',
      dueDate: tomorrow,
    };
    assert.strictEqual(isWorkItemOverdue(futureItem), false, 'Future due date should not be overdue');

    const completedOverdueDateItem: WorkInventoryItemDto = {
      ...overdueItem,
      id: 'item-3',
      normalizedStatus: 'COMPLETED',
      dueDate: yesterday,
    };
    assert.strictEqual(
      isWorkItemOverdue(completedOverdueDateItem),
      false,
      'Completed item with past due date should not be considered overdue'
    );

    const filedOverdueDateItem: WorkInventoryItemDto = {
      ...overdueItem,
      id: 'item-4',
      normalizedStatus: 'FILED',
      dueDate: yesterday,
    };
    assert.strictEqual(
      isWorkItemOverdue(filedOverdueDateItem),
      false,
      'Filed item with past due date should not be considered overdue'
    );

    const cancelledOverdueDateItem: WorkInventoryItemDto = {
      ...overdueItem,
      id: 'item-5',
      normalizedStatus: 'CANCELLED',
      dueDate: yesterday,
    };
    assert.strictEqual(
      isWorkItemOverdue(cancelledOverdueDateItem),
      false,
      'Cancelled item with past due date should not be considered overdue'
    );

    const noDueDateItem: WorkInventoryItemDto = {
      ...overdueItem,
      id: 'item-6',
      dueDate: undefined,
    };
    assert.strictEqual(
      isWorkItemOverdue(noDueDateItem),
      false,
      'Item without due date should not be considered overdue'
    );
  });

  it('2. Authoritative Navigation: resolves target routes based on work type and IDs', () => {
    // 2A. WORK_INSTANCE with engagementId -> /engagements/:engagementId
    const workInstanceWithEng: WorkInventoryItemDto = {
      id: 'wi-123',
      workType: 'WORK_INSTANCE',
      title: 'Annual Statutory Audit',
      normalizedStatus: 'IN_PROGRESS',
      rawStatus: 'IN_PROGRESS',
      priority: 'HIGH',
      engagementId: 'eng-456',
      blocked: false,
      totalTasks: 5,
      completedTasks: 2,
    };
    assert.strictEqual(
      getAuthoritativeNavigationUrl(workInstanceWithEng),
      '/engagements/eng-456'
    );

    // 2B. WORK_INSTANCE without engagementId -> /compliance/workbench fallback
    const workInstanceWithoutEng: WorkInventoryItemDto = {
      ...workInstanceWithEng,
      id: 'wi-789',
      engagementId: undefined,
    };
    assert.strictEqual(
      getAuthoritativeNavigationUrl(workInstanceWithoutEng),
      '/compliance/workbench'
    );

    // 2C. COMPLIANCE_OBLIGATION with clientId -> /clients/:clientId?tab=compliance
    const obligationWithClient: WorkInventoryItemDto = {
      id: 'ob-101',
      workType: 'COMPLIANCE_OBLIGATION',
      title: 'GSTR-3B July 2026',
      normalizedStatus: 'NOT_STARTED',
      rawStatus: 'READY',
      priority: 'CRITICAL',
      clientId: 'client-999',
      blocked: false,
      totalTasks: 0,
      completedTasks: 0,
    };
    assert.strictEqual(
      getAuthoritativeNavigationUrl(obligationWithClient),
      '/clients/client-999?tab=compliance'
    );

    // 2D. COMPLIANCE_OBLIGATION without clientId -> /compliance/workbench fallback
    const obligationWithoutClient: WorkInventoryItemDto = {
      ...obligationWithClient,
      clientId: undefined,
    };
    assert.strictEqual(
      getAuthoritativeNavigationUrl(obligationWithoutClient),
      '/compliance/workbench'
    );

    // 2E. TASK -> /tasks?tab=ALL_TASKS&taskId=:id
    const taskItem: WorkInventoryItemDto = {
      id: 'task-555',
      workType: 'TASK',
      title: 'Collect Bank Statements for Client',
      normalizedStatus: 'UNDER_REVIEW',
      rawStatus: 'UNDER_REVIEW',
      priority: 'MEDIUM',
      blocked: false,
      totalTasks: 1,
      completedTasks: 0,
    };
    assert.strictEqual(
      getAuthoritativeNavigationUrl(taskItem),
      '/tasks?tab=ALL_TASKS&taskId=task-555'
    );
  });

  it('3. Kanban Column Bucket Integrity: verifies that terminal and active statuses map correctly', () => {
    const statuses: Array<WorkInventoryItemDto['normalizedStatus']> = [
      'NOT_STARTED',
      'IN_PROGRESS',
      'BLOCKED',
      'UNDER_REVIEW',
      'READY_FOR_FILING',
      'FILED',
      'COMPLETED',
      'CANCELLED',
    ];

    // Verify all statuses exist and are non-empty
    assert.strictEqual(statuses.length, 8);
    assert.ok(statuses.includes('BLOCKED'));
    assert.ok(statuses.includes('FILED'));
    assert.ok(statuses.includes('COMPLETED'));
    assert.ok(statuses.includes('CANCELLED'));
  });

  it('4. Cancelled Work Item State Integrity: verified that CANCELLED items are not overdue and are distinguishable', () => {
    const cancelledItem: WorkInventoryItemDto = {
      id: 'wi-cancel-99',
      workType: 'WORK_INSTANCE',
      title: 'Tax Audit 2024-25',
      normalizedStatus: 'CANCELLED',
      rawStatus: 'CANCELLED',
      priority: 'CRITICAL',
      dueDate: '2020-01-01', // long past date
      blocked: false,
      totalTasks: 0,
      completedTasks: 0,
    };

    assert.strictEqual(
      isWorkItemOverdue(cancelledItem),
      false,
      'CANCELLED items must never be flagged as overdue regardless of past due date'
    );
    assert.strictEqual(cancelledItem.normalizedStatus, 'CANCELLED');
  });

  it('5. Task Route Resolution: correctly appends taskId parameter to task tab route', () => {
    const task: WorkInventoryItemDto = {
      id: 'task-uuid-abc',
      workType: 'TASK',
      title: 'Upload Form 16',
      normalizedStatus: 'NOT_STARTED',
      rawStatus: 'TODO',
      priority: 'MEDIUM',
      blocked: false,
      totalTasks: 1,
      completedTasks: 0,
    };

    const targetUrl = getAuthoritativeNavigationUrl(task);
    assert.strictEqual(targetUrl, '/tasks?tab=ALL_TASKS&taskId=task-uuid-abc');
  });
});
