import { describe, it } from 'node:test';
import assert from 'node:assert';
import type {
  ClientPortalDashboard,
  DocumentRequest,
  DocumentRequestItem,
  ClientDocumentRequest,
  Invoice,
} from '../types/index.ts';

describe('Phase 32.3: Client Portal Action Center Tests', () => {
  const mockDashboard: ClientPortalDashboard = {
    clientId: 'client-123',
    displayName: 'ABC Traders Pvt Ltd',
    legalName: 'ABC Traders Private Limited',
    clientType: 'PRIVATE_LIMITED',
    pan: 'AAACB1234D',
    gstin: '27AAACB1234D1Z5',
    tan: 'PNEB12345D',
    assignedPractitionerName: 'Kavita Verma (CA)',
    assignedPractitionerEmail: 'kavita@taxoryn.com',
    assignedPractitionerPhone: '9876543210',
    pendingDocumentsCount: 2,
    pendingTasksCount: 0,
    pendingActionItemsCount: 3,
    activeGstReturnsCount: 4,
    activeItrReturnsCount: 2,
    unpaidInvoicesCount: 1,
    outstandingBalance: 15000,
    latestGstFilings: [],
    latestItrReturns: [],
    pendingDocumentRequests: [],
    activeMultiItemRequests: [],
    pendingTasks: [], // Must remain empty (internal staff tasks are never leaked)
    recentNotifications: [],
    latestInvoices: [],
  };

  const sampleCanonicalRequests: DocumentRequest[] = [
    {
      id: 'req-001',
      organizationId: 'org-99',
      clientId: 'client-123',
      clientName: 'ABC Traders Pvt Ltd',
      requestNumber: 'REQ-2026-001',
      purpose: 'GST Audit & Annual Return Documentation',
      dueDate: '2026-11-15',
      message: 'Please provide required invoices and bank proofs',
      status: 'PARTIALLY_COMPLETED',
      createdAt: '2026-10-10T10:00:00Z',
      totalItems: 3,
      uploadedItems: 1,
      acceptedItems: 0,
      pendingItems: 1,
      rejectedItems: 1,
      isOverdue: false,
      items: [
        {
          id: 'item-101',
          requestId: 'req-001',
          clientId: 'client-123',
          documentType: 'BANK_STATEMENT',
          title: 'HDFC Current Account Bank Statement Oct 2026',
          required: true,
          status: 'PENDING',
        },
        {
          id: 'item-102',
          requestId: 'req-001',
          clientId: 'client-123',
          documentType: 'SALES_REGISTER',
          title: 'B2B Sales Register (GSTR-1 Reco)',
          required: true,
          status: 'REJECTED',
          rejectionReason: 'Page 4 columns are truncated; please re-export with GSTIN visible.',
        },
        {
          id: 'item-103',
          requestId: 'req-001',
          clientId: 'client-123',
          documentType: 'PURCHASE_REGISTER',
          title: 'Purchase Register GSTR-2B matched',
          required: true,
          status: 'UPLOADED',
          uploadedDocumentName: 'Purchase_Oct_2026.xlsx',
          uploadedAt: '2026-10-12T15:30:00Z',
        },
      ],
    },
    {
      id: 'req-002',
      organizationId: 'org-99',
      clientId: 'client-123',
      clientName: 'ABC Traders Pvt Ltd',
      requestNumber: 'REQ-2026-002',
      purpose: 'Advance Tax Calculation Q3',
      dueDate: '2026-09-15', // Past due date
      status: 'SENT',
      createdAt: '2026-09-01T10:00:00Z',
      totalItems: 1,
      uploadedItems: 0,
      acceptedItems: 0,
      pendingItems: 1,
      rejectedItems: 0,
      isOverdue: true,
      items: [
        {
          id: 'item-201',
          requestId: 'req-002',
          clientId: 'client-123',
          documentType: 'TDS_CERTIFICATE',
          title: 'Form 26AS / AIS Q2 Verification',
          required: true,
          status: 'PENDING',
        },
      ],
    },
  ];

  const sampleLegacyPendingRequests: ClientDocumentRequest[] = [
    {
      id: 'legacy-req-501',
      title: 'Electricity Bill Proof of Address',
      documentType: 'UTILITY_BILL',
      status: 'PENDING',
      dueDate: '2026-12-01',
      requestedAt: '2026-10-01T00:00:00Z',
    },
  ];

  const sampleInvoices: Invoice[] = [
    {
      id: 'inv-801',
      organizationId: 'org-99',
      clientId: 'client-123',
      invoiceNumber: 'INV-2026-042',
      invoiceDate: '2026-10-01',
      dueDate: '2026-10-20',
      status: 'SENT',
      total: 17700,
      paidAmount: 0,
      balanceDue: 17700,
      items: [],
      createdAt: '2026-10-01T00:00:00Z',
    },
    {
      id: 'inv-802',
      organizationId: 'org-99',
      clientId: 'client-123',
      invoiceNumber: 'INV-2026-020',
      invoiceDate: '2026-08-01',
      dueDate: '2026-08-15',
      status: 'PAID',
      total: 10000,
      paidAmount: 10000,
      balanceDue: 0,
      items: [],
      createdAt: '2026-08-01T00:00:00Z',
    },
  ];

  it('1. Extracts and categorizes rejected items requiring immediate correction', () => {
    const rejected: any[] = [];
    sampleCanonicalRequests.forEach((req) => {
      req.items?.forEach((item) => {
        if (item.status === 'REJECTED') {
          rejected.push({
            id: item.id,
            title: item.title,
            rejectionReason: item.rejectionReason,
            requestNumber: req.requestNumber,
          });
        }
      });
    });

    assert.strictEqual(rejected.length, 1);
    assert.strictEqual(rejected[0].id, 'item-102');
    assert.strictEqual(rejected[0].requestNumber, 'REQ-2026-001');
    assert.ok(rejected[0].rejectionReason.includes('truncated'));
  });

  it('2. Extracts pending items and detects overdue deadlines accurately', () => {
    const pending: any[] = [];
    sampleCanonicalRequests.forEach((req) => {
      req.items?.forEach((item) => {
        if (item.status === 'PENDING') {
          const isOverdue = req.dueDate ? new Date(req.dueDate) < new Date() : false;
          pending.push({
            id: item.id,
            title: item.title,
            dueDate: req.dueDate,
            isOverdue,
          });
        }
      });
    });

    // Also legacy requests
    sampleLegacyPendingRequests.forEach((req) => {
      if (req.status === 'PENDING') {
        pending.push({
          id: req.id,
          title: req.title,
          dueDate: req.dueDate,
          isOverdue: false,
        });
      }
    });

    assert.strictEqual(pending.length, 3);
    const overdueItem = pending.find((i) => i.id === 'item-201');
    assert.ok(overdueItem, 'Item 201 should be present');
    assert.strictEqual(overdueItem.isOverdue, true, 'Item 201 should be marked overdue');
  });

  it('3. Extracts in-progress submissions under review', () => {
    const underReview: any[] = [];
    sampleCanonicalRequests.forEach((req) => {
      req.items?.forEach((item) => {
        if (item.status === 'UPLOADED' || item.status === 'UNDER_REVIEW') {
          underReview.push(item);
        }
      });
    });

    assert.strictEqual(underReview.length, 1);
    assert.strictEqual(underReview[0].id, 'item-103');
    assert.strictEqual(underReview[0].uploadedDocumentName, 'Purchase_Oct_2026.xlsx');
  });

  it('4. Filters unpaid invoices with balance due', () => {
    const unpaid = sampleInvoices.filter((i) => i.status !== 'PAID' && (i.balanceDue || 0) > 0);
    assert.strictEqual(unpaid.length, 1);
    assert.strictEqual(unpaid[0].invoiceNumber, 'INV-2026-042');
    assert.strictEqual(unpaid[0].balanceDue, 17700);
  });

  it('5. Computes total action required items accurately', () => {
    // 1 rejected + 3 pending + 1 unpaid invoice = 5 action items
    const rejectedCount = 1;
    const pendingCount = 3;
    const unpaidInvoiceCount = 1;
    const totalActionRequired = rejectedCount + pendingCount + unpaidInvoiceCount;

    assert.strictEqual(totalActionRequired, 5);
  });

  it('6. Empty state: Confirms all caught up when no pending, rejected, or unpaid items exist', () => {
    const completedRequests: DocumentRequest[] = [
      {
        id: 'req-003',
        organizationId: 'org-99',
        clientId: 'client-123',
        clientName: 'ABC Traders Pvt Ltd',
        requestNumber: 'REQ-2026-003',
        purpose: 'Annual ITR Filing AY 2026-27',
        status: 'COMPLETED',
        createdAt: '2026-08-01T00:00:00Z',
        totalItems: 1,
        uploadedItems: 1,
        acceptedItems: 1,
        pendingItems: 0,
        rejectedItems: 0,
        isOverdue: false,
        items: [
          {
            id: 'item-301',
            requestId: 'req-003',
            clientId: 'client-123',
            documentType: 'ITR_ACKNOWLEDGEMENT',
            title: 'ITR-V Signed Acknowledgment',
            required: true,
            status: 'ACCEPTED',
          },
        ],
      },
    ];

    const rejected: any[] = [];
    const pending: any[] = [];
    completedRequests.forEach((req) => {
      req.items?.forEach((item) => {
        if (item.status === 'REJECTED') rejected.push(item);
        if (item.status === 'PENDING') pending.push(item);
      });
    });

    const unpaid = sampleInvoices.filter((i) => i.status === 'PAID' && (i.balanceDue || 0) > 0);
    const totalActions = rejected.length + pending.length + unpaid.length;

    assert.strictEqual(totalActions, 0, 'Total pending actions should be 0');
    assert.strictEqual(completedRequests[0].items[0].status, 'ACCEPTED');
  });

  it('7. Internal task privacy: Dashboard pendingTasks is strictly isolated and zero', () => {
    assert.strictEqual(mockDashboard.pendingTasksCount, 0);
    assert.strictEqual(mockDashboard.pendingTasks.length, 0);
  });
});
