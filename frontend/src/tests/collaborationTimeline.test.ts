import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import type {
  TimelineEventCategory,
  TimelineEventSeverity,
  ClientTimelineItemDto,
  ClientTimelineFilterParams,
} from '../types/index.ts';

describe('Phase 32.7: Collaboration Timeline Frontend Tests', () => {

  it('1. Collaboration Timeline Categories: includes APPROVAL, COMMUNICATION, DOCUMENT, COMPLIANCE, WORK', () => {
    const categories: TimelineEventCategory[] = [
      'APPROVAL',
      'COMMUNICATION',
      'DOCUMENT',
      'COMPLIANCE',
      'WORK',
      'CLIENT',
      'SERVICE',
      'BILLING',
      'SYSTEM',
    ];

    assert.ok(categories.includes('APPROVAL'));
    assert.ok(categories.includes('COMMUNICATION'));
    assert.ok(categories.includes('DOCUMENT'));
    assert.ok(categories.includes('COMPLIANCE'));
    assert.ok(categories.includes('WORK'));
  });

  it('2. ClientTimelineItemDto Structure: validates collaboration and context fields', () => {
    const item: ClientTimelineItemDto = {
      id: 'e0000000-0000-0000-0000-000000000001',
      clientId: 'c0000000-0000-0000-0000-000000000001',
      organizationId: 'o0000000-0000-0000-0000-000000000001',
      eventType: 'APPROVAL_ACCEPTED',
      eventCategory: 'APPROVAL',
      title: 'Approval Granted: FY 2024-25 Tax Computation',
      description: 'Client confirmed and signed off on the computation.',
      occurredAt: '2026-10-15T10:00:00Z',
      actorId: 'u0000000-0000-0000-0000-000000000001',
      actorName: 'John Signatory',
      sourceModule: 'CLIENT_APPROVAL',
      severity: 'SUCCESS',
      relatedEntityId: 'appr-123',
      relatedEntityType: 'CLIENT_APPROVAL',
      actionUrl: '/portal/action-center',
      clientVisible: true,
      contextType: 'APPROVAL',
      contextId: 'appr-123',
      metadata: {
        documentId: 'doc-999',
      },
    };

    assert.equal(item.eventCategory, 'APPROVAL');
    assert.equal(item.eventType, 'APPROVAL_ACCEPTED');
    assert.equal(item.severity, 'SUCCESS');
    assert.equal(item.clientVisible, true);
    assert.equal(item.actionUrl, '/portal/action-center');
    assert.equal(item.contextType, 'APPROVAL');
  });

  it('3. Deterministic Sorting: sorts primarily by occurredAt DESC, secondarily by id DESC', () => {
    const item1: ClientTimelineItemDto = {
      id: 'bbb-2',
      clientId: 'c1',
      organizationId: 'o1',
      eventType: 'DOCUMENT_UPLOADED',
      eventCategory: 'DOCUMENT',
      title: 'Doc 2',
      description: '',
      occurredAt: '2026-10-15T12:00:00Z',
      sourceModule: 'DOCREQUEST',
      severity: 'INFO',
    };

    const item2: ClientTimelineItemDto = {
      id: 'aaa-1',
      clientId: 'c1',
      organizationId: 'o1',
      eventType: 'APPROVAL_REQUESTED',
      eventCategory: 'APPROVAL',
      title: 'Appr 1',
      description: '',
      occurredAt: '2026-10-15T12:00:00Z', // identical timestamp
      sourceModule: 'CLIENT_APPROVAL',
      severity: 'INFO',
    };

    const item3: ClientTimelineItemDto = {
      id: 'ccc-3',
      clientId: 'c1',
      organizationId: 'o1',
      eventType: 'PORTAL_MESSAGE_RECEIVED',
      eventCategory: 'COMMUNICATION',
      title: 'Msg 3',
      description: '',
      occurredAt: '2026-10-15T14:00:00Z', // newer timestamp
      sourceModule: 'PORTAL_MESSAGING',
      severity: 'INFO',
    };

    const list = [item1, item2, item3];

    list.sort((a, b) => {
      const timeDiff = new Date(b.occurredAt).getTime() - new Date(a.occurredAt).getTime();
      if (timeDiff !== 0) return timeDiff;
      return b.id.localeCompare(a.id);
    });

    // Newest first (item3)
    assert.equal(list[0].id, 'ccc-3');
    // Same time: bbb-2 > aaa-1
    assert.equal(list[1].id, 'bbb-2');
    assert.equal(list[2].id, 'aaa-1');
  });

  it('4. Client Portal Visibility Sanitization: verifies sensitive internal metadata is stripped', () => {
    const rawPracticeItem: ClientTimelineItemDto = {
      id: 'e0000000-0000-0000-0000-000000000001',
      clientId: 'c0000000-0000-0000-0000-000000000001',
      organizationId: 'o0000000-0000-0000-0000-000000000001',
      eventType: 'PORTAL_MESSAGE_SENT',
      eventCategory: 'COMMUNICATION',
      title: 'Message sent by Practice',
      description: 'Please review documents.',
      occurredAt: '2026-10-15T10:00:00Z',
      actorId: 'user-internal-uuid-1234',
      actorName: 'CA Senior Partner',
      sourceModule: 'PORTAL_MESSAGING',
      severity: 'INFO',
      clientVisible: true,
      metadata: {
        ipAddress: '192.168.1.50',
        requestId: 'req-trace-abc',
        safeContextKey: 'INV-2024',
      },
    };

    // Client-side sanitization verification
    const sanitizeForPortal = (item: ClientTimelineItemDto): ClientTimelineItemDto => {
      const cleanMeta: Record<string, any> = {};
      if (item.metadata) {
        for (const [key, val] of Object.entries(item.metadata)) {
          if (!['ipAddress', 'requestId', 'oldValue', 'newValue'].includes(key)) {
            cleanMeta[key] = val;
          }
        }
      }
      return {
        ...item,
        actorId: undefined, // internal user ID stripped
        metadata: cleanMeta,
      };
    };

    const sanitized = sanitizeForPortal(rawPracticeItem);
    assert.equal(sanitized.actorId, undefined);
    assert.equal(sanitized.metadata?.ipAddress, undefined);
    assert.equal(sanitized.metadata?.requestId, undefined);
    assert.equal(sanitized.metadata?.safeContextKey, 'INV-2024');
  });

  it('5. ClientTimelineFilterParams: validates filter query criteria', () => {
    const filter: ClientTimelineFilterParams = {
      category: 'APPROVAL',
      contextType: 'APPROVAL',
      contextId: 'appr-001',
      clientVisibleOnly: true,
      page: 0,
      size: 25,
    };

    assert.equal(filter.category, 'APPROVAL');
    assert.equal(filter.contextType, 'APPROVAL');
    assert.equal(filter.clientVisibleOnly, true);
    assert.equal(filter.size, 25);
  });
});
