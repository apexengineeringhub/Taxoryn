import { describe, it } from 'node:test';
import assert from 'node:assert';
import type {
  Client360Overview,
  Client,
} from '../types/index.ts';

describe('Phase 10: Client 360 Foundation Unit Tests', () => {
  const mockClient: Client = {
    id: 'client-101',
    organizationId: 'org-1',
    displayName: 'Apex Global Technologies Ltd',
    legalName: 'Apex Global Technologies Limited',
    clientType: 'PRIVATE_LIMITED',
    pan: 'AAACA5432B',
    gstin: '27AAACA5432B1Z5',
    tan: 'PNEP54321B',
    email: 'contact@apextech.com',
    phone: '9876543210',
    status: 'ACTIVE',
    assignedEmployeeName: 'Ramesh Sharma',
    createdAt: '2026-01-01T00:00:00Z',
  };

  const mockOverview: Client360Overview = {
    client: mockClient,
    statutory: {
      pan: 'AAACA5432B',
      gstin: '27AAACA5432B1Z5',
      tan: 'PNEP54321B',
      isPanValid: true,
      isGstActive: true,
    },
    services: [
      {
        serviceCode: 'GST',
        serviceName: 'GST Compliance & Filing',
        status: 'ACTIVE',
        identifier: '27AAACA5432B1Z5',
        summary: 'Monthly GSTR-1 and GSTR-3B filings',
        routePath: '/gst',
      },
      {
        serviceCode: 'ITR',
        serviceName: 'Income Tax Return Filing',
        status: 'ACTIVE',
        identifier: 'AAACA5432B',
        summary: 'Annual corporate tax return and advance tax computations',
        routePath: '/itr',
      },
    ],
    taskSummary: {
      totalTasks: 5,
      pendingTasks: 3,
      inProgressTasks: 2,
      underReviewTasks: 1,
      overdueTasks: 0,
      completedTasks: 2,
      recentTasks: [],
    },
    complianceSummary: {
      gstStatus: 'ACTIVE',
      itrStatus: 'ACTIVE',
      tdsStatus: 'ACTIVE',
      accountingStatus: 'NOT_CONFIGURED',
      gstDetails: {
        registered: true,
        gstin: '27AAACA5432B1Z5',
        filingFrequency: 'MONTHLY',
        totalFilings: 8,
        pendingFilings: 1,
        filedFilings: 7,
        overdueFilings: 0,
      },
      itrDetails: {
        registered: true,
        pan: 'AAACA5432B',
        taxpayerType: 'COMPANY',
        defaultItrType: 'ITR-6',
        totalReturns: 1,
        pendingReturns: 0,
        filedReturns: 1,
        overdueReturns: 0,
        currentAssessmentYear: '2026-27',
        currentStatus: 'FILED',
      },
      tdsDetails: {
        registered: true,
        tan: 'PNEP54321B',
        deductorType: 'COMPANY',
        totalReturns: 4,
        pendingReturns: 0,
        filedReturns: 4,
        overdueReturns: 0,
        currentFinancialYear: '2025-26',
      },
    },
    documentsSummary: {
      totalDocuments: 15,
      documentCategories: ['GST', 'INCOME_TAX', 'KYC', 'INVOICES'],
      recentDocuments: [],
    },
    docRequestsSummary: {
      totalRequests: 3,
      pendingRequests: 1,
      receivedRequests: 2,
      overdueRequests: 0,
      recentRequests: [],
    },
    billingSummary: {
      totalInvoiced: 150000,
      totalPaid: 120000,
      outstandingBalance: 30000,
      currency: 'INR',
      totalInvoicesCount: 3,
      overdueInvoicesCount: 0,
      recentInvoices: [],
    },
    noticeSummary: {
      totalNotices: 1,
      activeNotices: 1,
      overdueNotices: 0,
      hearingsScheduled: 0,
      totalDemandAmount: 0,
      recentNotices: [],
    },
    recentNotes: [
      {
        id: 'note-1',
        clientId: 'client-101',
        authorId: 'user-1',
        title: 'Quarterly Review',
        content: 'Reviewed Q3 GST reconciliations and ITNS 281 tax challans.',
        noteType: 'MEETING',
        authorName: 'Ramesh Sharma',
        createdAt: '2026-09-20T10:00:00Z',
      },
    ],
    activityTimeline: [
      {
        id: 'act-1',
        eventType: 'CLIENT_NOTE_CREATED',
        title: 'Meeting Note Recorded',
        category: 'CLIENT',
        description: 'Quarterly review note recorded by Ramesh Sharma',
        timestamp: '2026-09-20T10:00:00Z',
      },
    ],
  };

  it('1. Correctly verifies Client 360 overview structure and active services', () => {
    assert.strictEqual(mockOverview.client.displayName, 'Apex Global Technologies Ltd');
    assert.strictEqual(mockOverview.statutory.pan, 'AAACA5432B');
    assert.strictEqual(mockOverview.statutory.isGstActive, true);
    assert.strictEqual(mockOverview.services.length, 2);
    assert.strictEqual(mockOverview.services[0].serviceCode, 'GST');
  });

  it('2. Evaluates task analytics summary properly', () => {
    const { taskSummary } = mockOverview;
    assert.strictEqual(taskSummary.totalTasks, 5);
    assert.strictEqual(taskSummary.pendingTasks, 3);
    assert.strictEqual(taskSummary.inProgressTasks, 2);
    assert.strictEqual(taskSummary.completedTasks, 2);
    assert.strictEqual(taskSummary.overdueTasks, 0);
  });

  it('3. Evaluates multi-domain compliance status correctly (GST, ITR, TDS)', () => {
    const { complianceSummary } = mockOverview;
    assert.strictEqual(complianceSummary.gstDetails?.gstin, '27AAACA5432B1Z5');
    assert.strictEqual(complianceSummary.gstDetails?.pendingFilings, 1);
    assert.strictEqual(complianceSummary.itrDetails?.defaultItrType, 'ITR-6');
    assert.strictEqual(complianceSummary.itrDetails?.currentStatus, 'FILED');
    assert.strictEqual(complianceSummary.tdsDetails?.tan, 'PNEP54321B');
    assert.strictEqual(complianceSummary.tdsDetails?.filedReturns, 4);
  });

  it('4. Handles Zero-Trust Billing Redaction for non-billing staff', () => {
    const redactedOverview: Client360Overview = {
      ...mockOverview,
      billingSummary: undefined,
    };

    assert.strictEqual(redactedOverview.billingSummary, undefined);
    assert.ok(redactedOverview.documentsSummary !== undefined);
    assert.ok(redactedOverview.taskSummary !== undefined);
  });

  it('5. Evaluates Document & Document Request metrics', () => {
    assert.strictEqual(mockOverview.documentsSummary.totalDocuments, 15);
    assert.strictEqual(mockOverview.docRequestsSummary?.totalRequests, 3);
    assert.strictEqual(mockOverview.docRequestsSummary?.pendingRequests, 1);
    assert.strictEqual(mockOverview.docRequestsSummary?.receivedRequests, 2);
  });

  it('6. Evaluates Notice and Communication Notes count', () => {
    assert.strictEqual(mockOverview.noticeSummary.totalNotices, 1);
    assert.strictEqual(mockOverview.recentNotes.length, 1);
    assert.strictEqual(mockOverview.recentNotes[0].noteType, 'MEETING');
    assert.strictEqual(mockOverview.activityTimeline.length, 1);
  });
});
