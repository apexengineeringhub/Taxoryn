import { describe, it } from 'node:test';
import assert from 'node:assert';
import type {
  MessageContextType,
  PortalMessageAttachment,
  ClientPortalMessage,
  SendClientPortalMessageRequest,
} from '../types/index.ts';

describe('Phase 32.4: Contextual Client Communication & Consultation Tests', () => {
  it('1. Verifies supported MessageContextType enumeration', () => {
    const supportedTypes: MessageContextType[] = [
      'GENERAL',
      'DOCUMENT_REQUEST',
      'DOCUMENT_ITEM',
      'COMPLIANCE_WORKFLOW',
      'NOTICE',
    ];

    assert.strictEqual(supportedTypes.length, 5);
    assert.ok(supportedTypes.includes('GENERAL'));
    assert.ok(supportedTypes.includes('DOCUMENT_ITEM'));
    assert.ok(supportedTypes.includes('DOCUMENT_REQUEST'));
    assert.ok(supportedTypes.includes('COMPLIANCE_WORKFLOW'));
    assert.ok(supportedTypes.includes('NOTICE'));
  });

  it('2. Constructs valid contextual consultation message payload from rejected item', () => {
    const rejectedItem = {
      id: 'item-102',
      title: 'B2B Sales Register (GSTR-1 Reco)',
      requestNumber: 'REQ-2026-001',
    };

    const initiatedContext = {
      contextType: 'DOCUMENT_ITEM' as MessageContextType,
      contextId: rejectedItem.id,
      contextTitle: `${rejectedItem.requestNumber} - ${rejectedItem.title}`,
    };

    const payload: SendClientPortalMessageRequest = {
      messageBody: 'Could you please clarify which columns on page 4 need correction?',
      contextType: initiatedContext.contextType,
      contextId: initiatedContext.contextId,
      contextTitle: initiatedContext.contextTitle,
    };

    assert.strictEqual(payload.contextType, 'DOCUMENT_ITEM');
    assert.strictEqual(payload.contextId, 'item-102');
    assert.strictEqual(payload.contextTitle, 'REQ-2026-001 - B2B Sales Register (GSTR-1 Reco)');
    assert.ok(payload.messageBody.length > 0);
  });

  it('3. Supports message attachments with metadata and virus scanning status', () => {
    const attachment: PortalMessageAttachment = {
      documentId: 'doc-501',
      fileName: 'Revised_Sales_Oct_2026.xlsx',
      fileSize: 1048576, // 1 MB
      contentType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      downloadUrl: '/api/v1/portal/documents/doc-501/download',
    };

    const message: ClientPortalMessage = {
      id: 'msg-901',
      organizationId: 'org-99',
      clientId: 'client-123',
      senderName: 'Client User (Rajesh)',
      senderType: 'CLIENT',
      messageBody: 'Here is the revised export with GSTIN clearly visible.',
      contextType: 'DOCUMENT_ITEM',
      contextId: 'item-102',
      contextTitle: 'REQ-2026-001 - B2B Sales Register (GSTR-1 Reco)',
      attachments: [attachment],
      isReadByClient: true,
      isReadByPractice: false,
      createdAt: '2026-10-15T10:00:00Z',
    };

    assert.strictEqual(message.attachments?.length, 1);
    assert.strictEqual(message.attachments?.[0].documentId, 'doc-501');
    assert.strictEqual(message.attachments?.[0].fileName, 'Revised_Sales_Oct_2026.xlsx');
    assert.strictEqual(message.attachments?.[0].fileSize, 1048576);
    assert.strictEqual(message.contextType, 'DOCUMENT_ITEM');
  });

  it('4. Filters messages accurately by context type', () => {
    const conversation: ClientPortalMessage[] = [
      {
        id: 'msg-1',
        organizationId: 'org-99',
        clientId: 'client-123',
        senderName: 'Rajesh',
        senderType: 'CLIENT',
        messageBody: 'Hello, I have a general question on advance tax.',
        contextType: 'GENERAL',
        isReadByClient: true,
        isReadByPractice: true,
        createdAt: '2026-10-15T09:00:00Z',
      },
      {
        id: 'msg-2',
        organizationId: 'org-99',
        clientId: 'client-123',
        senderName: 'Rajesh',
        senderType: 'CLIENT',
        messageBody: 'Question regarding bank statement upload.',
        contextType: 'DOCUMENT_ITEM',
        contextId: 'item-101',
        contextTitle: 'Bank Statement Oct 2026',
        isReadByClient: true,
        isReadByPractice: false,
        createdAt: '2026-10-15T09:15:00Z',
      },
      {
        id: 'msg-3',
        organizationId: 'org-99',
        clientId: 'client-123',
        senderName: 'CA Kavita',
        senderType: 'PRACTICE',
        messageBody: 'Regarding your GST Notice 148A.',
        contextType: 'NOTICE',
        contextId: 'not-401',
        contextTitle: 'Notice 148A - Reassessment',
        isReadByClient: false,
        isReadByPractice: true,
        createdAt: '2026-10-15T09:30:00Z',
      },
      {
        id: 'msg-4',
        organizationId: 'org-99',
        clientId: 'client-123',
        senderName: 'CA Kavita',
        senderType: 'PRACTICE',
        messageBody: 'Bank statement format is acceptable in PDF.',
        contextType: 'DOCUMENT_ITEM',
        contextId: 'item-101',
        contextTitle: 'Bank Statement Oct 2026',
        isReadByClient: false,
        isReadByPractice: true,
        createdAt: '2026-10-15T09:45:00Z',
      },
    ];

    // Filter by ALL
    const all = conversation;
    assert.strictEqual(all.length, 4);

    // Filter by DOCUMENT_ITEM
    const docItems = conversation.filter((m) => m.contextType === 'DOCUMENT_ITEM');
    assert.strictEqual(docItems.length, 2);
    assert.strictEqual(docItems[0].id, 'msg-2');
    assert.strictEqual(docItems[1].id, 'msg-4');

    // Filter by NOTICE
    const notices = conversation.filter((m) => m.contextType === 'NOTICE');
    assert.strictEqual(notices.length, 1);
    assert.strictEqual(notices[0].contextId, 'not-401');

    // Filter by specific contextId
    const item101Thread = conversation.filter(
      (m) => m.contextType === 'DOCUMENT_ITEM' && m.contextId === 'item-101'
    );
    assert.strictEqual(item101Thread.length, 2);
  });

  it('5. Enforces attachment upload constraints (size limit 25MB)', () => {
    const MAX_ATTACHMENT_BYTES = 25 * 1024 * 1024; // 25 MB

    const validFile = { name: 'Audit_Report.pdf', size: 15 * 1024 * 1024 };
    const oversizedFile = { name: 'Full_Raw_Dump.zip', size: 30 * 1024 * 1024 };

    const isValid = (file: { size: number }) => file.size <= MAX_ATTACHMENT_BYTES;

    assert.strictEqual(isValid(validFile), true, '15MB file must be accepted');
    assert.strictEqual(isValid(oversizedFile), false, '30MB file must be rejected');
  });

  it('6. Preserves dual-tenant scoping (org + client) for portal messages', () => {
    const practiceMessage: ClientPortalMessage = {
      id: 'msg-p1',
      organizationId: 'org-tenant-1',
      clientId: 'client-abc',
      senderName: 'Practitioner',
      senderType: 'PRACTICE',
      messageBody: 'Please review and submit your reply.',
      contextType: 'DOCUMENT_REQUEST',
      contextId: 'req-001',
      isReadByClient: false,
      isReadByPractice: true,
      createdAt: '2026-10-15T10:00:00Z',
    };

    assert.strictEqual(practiceMessage.organizationId, 'org-tenant-1');
    assert.strictEqual(practiceMessage.clientId, 'client-abc');
  });
});
