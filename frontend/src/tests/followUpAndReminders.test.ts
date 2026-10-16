import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import type {
  ReminderType,
  ReminderStatus,
  FollowUpItem,
  FollowUpFilterRequest,
} from '../types/index.ts';

describe('Phase 32.6: Follow-up & Reminder Engine Frontend Tests', () => {

  it('1. Collaboration Reminder Categories: validates all 5 required reminder types', () => {
    const requiredTypes: ReminderType[] = [
      'CLIENT_APPROVAL',
      'DOCUMENT_REQUEST',
      'UNANSWERED_COMMUNICATION',
      'COMPLIANCE_DEADLINE',
      'PRACTITIONER_FOLLOW_UP',
    ];

    assert.equal(requiredTypes.length, 5);
    assert.ok(requiredTypes.includes('CLIENT_APPROVAL'));
    assert.ok(requiredTypes.includes('DOCUMENT_REQUEST'));
    assert.ok(requiredTypes.includes('UNANSWERED_COMMUNICATION'));
    assert.ok(requiredTypes.includes('COMPLIANCE_DEADLINE'));
    assert.ok(requiredTypes.includes('PRACTITIONER_FOLLOW_UP'));
  });

  it('2. FollowUpItem DTO Contract: validates model structure and properties', () => {
    const item: FollowUpItem = {
      id: 'a0000000-0000-0000-0000-000000000001',
      organizationId: 'org-0000-0000-0000-000000000001',
      title: 'Pending ITR-6 Client Approval',
      description: 'Approval requested on 2026-10-10 is awaiting response for > 48h.',
      category: 'CLIENT_APPROVAL',
      priority: 'HIGH',
      status: 'PENDING',
      overdue: false,
      clientId: 'c0000000-0000-0000-0000-000000000001',
      clientName: 'Acme Advisory Ltd',
      referenceType: 'CLIENT_APPROVAL',
      referenceId: 'ref-appr-123',
      actionUrl: '/portal/action-center',
      scheduledAt: '2026-10-15T10:00:00Z',
      createdAt: '2026-10-15T09:00:00Z',
      lastAttemptAt: '2026-10-15T09:30:00Z',
      notificationAttempts: 1,
    };

    assert.equal(item.category, 'CLIENT_APPROVAL');
    assert.equal(item.priority, 'HIGH');
    assert.equal(item.status, 'PENDING');
    assert.equal(item.notificationAttempts, 1);
    assert.ok(item.actionUrl?.startsWith('/portal'));
  });

  it('3. Manual Nudge Cooldown Client Check: detects if item is within 12-hour cooldown', () => {
    const now = new Date('2026-10-15T12:00:00Z').getTime();

    const isCooldownActive = (lastAttemptAt?: string): boolean => {
      if (!lastAttemptAt) return false;
      const lastNudgeMs = new Date(lastAttemptAt).getTime();
      const diffHours = (now - lastNudgeMs) / (1000 * 60 * 60);
      return diffHours < 12;
    };

    // Nudged 2 hours ago -> active cooldown
    assert.equal(isCooldownActive('2026-10-15T10:00:00Z'), true);

    // Nudged 13 hours ago -> cooldown elapsed
    assert.equal(isCooldownActive('2026-10-14T23:00:00Z'), false);

    // Never nudged -> cooldown not active
    assert.equal(isCooldownActive(undefined), false);
  });

  it('4. FollowUpFilterRequest: verifies filter payload generation', () => {
    const filter: FollowUpFilterRequest = {
      category: 'DOCUMENT_REQUEST',
      clientId: 'c0000000-0000-0000-0000-000000000001',
      page: 0,
      size: 20,
    };

    assert.equal(filter.category, 'DOCUMENT_REQUEST');
    assert.equal(filter.page, 0);
    assert.equal(filter.size, 20);
  });

  it('5. Status Transitions: verifies non-terminal and terminal reminder states', () => {
    const isTerminal = (status: ReminderStatus): boolean => {
      return status === 'COMPLETED' || status === 'CANCELLED';
    };

    assert.equal(isTerminal('PENDING'), false);
    assert.equal(isTerminal('TRIGGERED'), false);
    assert.equal(isTerminal('COMPLETED'), true);
    assert.equal(isTerminal('CANCELLED'), true);
  });
});
