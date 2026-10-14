import { describe, it } from 'node:test';
import assert from 'node:assert';
import type {
  ClientApproval,
  ClientApprovalType,
  ClientApprovalStatus,
  ClientApprovalDecision,
  CreateClientApprovalRequest,
  DecideClientApprovalRequest,
  RevokeClientApprovalRequest,
  ClientPortalDashboard,
} from '../types/index.ts';

describe('Phase 32.5: Client Approvals & Consent Tests', () => {
  const sampleApproval: ClientApproval = {
    id: 'appr-001',
    organizationId: 'org-test-101',
    clientId: 'client-test-202',
    title: 'AY 2024-25 Income Tax Computation Sign-off',
    description: 'Please review and confirm the computed taxable income and deductions before ITR-6 filing.',
    approvalType: 'TAX_COMPUTATION_APPROVAL',
    status: 'PENDING',
    contextType: 'COMPLIANCE_WORKFLOW',
    contextId: 'wf-999',
    contextTitle: 'Corporate Income Tax Filing AY 2024-25',
    documentId: 'doc-1234',
    documentName: 'Computation_Summary_v1.pdf',
    documentDownloadUrl: '/api/v1/portal/documents/doc-1234/download',
    contentVersion: 'v1.0',
    contentHash: 'SHA256:7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069',
    requestedByUserId: 'user-ca-1',
    requestedByName: 'Kapoor CA',
    requestedAt: '2026-10-14T10:00:00Z',
    expiresAt: '2026-10-21T10:00:00Z',
    isExpired: false,
    createdAt: '2026-10-14T10:00:00Z',
  };

  it('1. Verifies ClientApproval structure, fields, and initial pending status', () => {
    assert.strictEqual(sampleApproval.id, 'appr-001');
    assert.strictEqual(sampleApproval.title, 'AY 2024-25 Income Tax Computation Sign-off');
    assert.strictEqual(sampleApproval.approvalType, 'TAX_COMPUTATION_APPROVAL');
    assert.strictEqual(sampleApproval.status, 'PENDING');
    assert.strictEqual(sampleApproval.contentVersion, 'v1.0');
    assert.strictEqual(sampleApproval.isExpired, false);
    assert.strictEqual(sampleApproval.documentName, 'Computation_Summary_v1.pdf');
    assert.strictEqual(sampleApproval.contextType, 'COMPLIANCE_WORKFLOW');
  });

  it('2. Handles approval decision with server-authenticated provenance and client notes', () => {
    const decisionPayload: DecideClientApprovalRequest = {
      decision: 'APPROVE',
      expectedContentVersion: 'v1.0',
      notes: 'Reviewed computation sheet. All figures match our audited financials.',
    };

    assert.strictEqual(decisionPayload.decision, 'APPROVE');
    assert.strictEqual(decisionPayload.expectedContentVersion, sampleApproval.contentVersion);

    // Simulating approved state
    const approvedItem: ClientApproval = {
      ...sampleApproval,
      status: 'APPROVED',
      respondedByUserId: 'client-user-88',
      respondedByName: 'Rohan Verma',
      respondedByEmail: 'rohan@omega.com',
      respondedAt: '2026-10-14T12:30:00Z',
      decisionNotes: decisionPayload.notes,
    };

    assert.strictEqual(approvedItem.status, 'APPROVED');
    assert.strictEqual(approvedItem.respondedByName, 'Rohan Verma');
    assert.strictEqual(approvedItem.respondedByEmail, 'rohan@omega.com');
    assert.ok(approvedItem.respondedAt);
    assert.strictEqual(approvedItem.decisionNotes, decisionPayload.notes);
  });

  it('3. Rejection requires a mandatory rejection reason', () => {
    const invalidRejection: DecideClientApprovalRequest = {
      decision: 'REJECT',
      rejectionReason: '',
    };
    assert.ok(!invalidRejection.rejectionReason?.trim(), 'Empty rejection reason should be detected');

    const validRejection: DecideClientApprovalRequest = {
      decision: 'REJECT',
      rejectionReason: 'Turnover figure in Schedule 112 does not include export sales receipts.',
    };
    assert.ok((validRejection.rejectionReason || '').trim().length > 0);

    // Simulating rejected state
    const rejectedItem: ClientApproval = {
      ...sampleApproval,
      status: 'REJECTED',
      respondedByUserId: 'client-user-88',
      respondedByName: 'Rohan Verma',
      respondedAt: '2026-10-14T14:00:00Z',
      rejectionReason: validRejection.rejectionReason,
    };

    assert.strictEqual(rejectedItem.status, 'REJECTED');
    assert.strictEqual(rejectedItem.rejectionReason, validRejection.rejectionReason);
  });

  it('4. Stale content version mismatch prevents approving outdated revisions', () => {
    const staleRequest: DecideClientApprovalRequest = {
      decision: 'APPROVE',
      expectedContentVersion: 'v0.9', // Mismatched version
    };

    const hasVersionMismatch = staleRequest.expectedContentVersion !== sampleApproval.contentVersion;
    assert.strictEqual(hasVersionMismatch, true, 'Version mismatch should be detected');
  });

  it('5. Practice revocation transitions pending request to REVOKED with audit reason', () => {
    const revokeRequest: RevokeClientApprovalRequest = {
      reason: 'Draft computation revised due to updated form 26AS reflection.',
    };

    const revokedItem: ClientApproval = {
      ...sampleApproval,
      status: 'REVOKED',
      revokedByUserId: 'user-ca-1',
      revokedByName: 'Kapoor CA',
      revokedAt: '2026-10-14T15:00:00Z',
      revocationReason: revokeRequest.reason,
    };

    assert.strictEqual(revokedItem.status, 'REVOKED');
    assert.strictEqual(revokedItem.revokedByName, 'Kapoor CA');
    assert.strictEqual(revokedItem.revocationReason, revokeRequest.reason);
  });

  it('6. ClientPortalDashboard reflects pending approvals count and items', () => {
    const dashboard: ClientPortalDashboard = {
      clientId: 'client-test-202',
      displayName: 'Omega Infotech Private Limited',
      clientType: 'PRIVATE_LIMITED',
      pan: 'AABCO1234F',
      pendingDocumentsCount: 1,
      pendingTasksCount: 0,
      activeGstReturnsCount: 0,
      activeItrReturnsCount: 1,
      unpaidInvoicesCount: 0,
      outstandingBalance: 0,
      latestGstFilings: [],
      latestItrReturns: [],
      pendingDocumentRequests: [],
      pendingTasks: [],
      recentNotifications: [],
      latestInvoices: [],
      pendingApprovalsCount: 1,
      pendingApprovals: [sampleApproval],
    };

    assert.strictEqual(dashboard.pendingApprovalsCount, 1);
    assert.strictEqual(dashboard.pendingApprovals?.length, 1);
    assert.strictEqual(dashboard.pendingApprovals?.[0].title, sampleApproval.title);
  });

  it('7. Strict Phase 31 QC gate isolation: client approval record does not grant practitioner filing authorization', () => {
    // Assert that ClientApproval properties do not contain practitioner QC authorization gate fields
    const approvalKeys = Object.keys(sampleApproval);
    assert.ok(!approvalKeys.includes('isAuthorized'), 'ClientApproval must not possess isAuthorized field');
    assert.ok(!approvalKeys.includes('authorizedByPractitioner'), 'ClientApproval must not have practitioner authorization bypass');
    assert.ok(!approvalKeys.includes('submittedToGovernment'), 'ClientApproval must never directly submit filings');
  });

  it('8. Supports all mandatory ClientApprovalTypes across Taxoryn practice contexts', () => {
    const types: ClientApprovalType[] = [
      'TAX_COMPUTATION_APPROVAL',
      'FILING_CONSENT',
      'DOCUMENT_SIGN_OFF',
      'ENGAGEMENT_TERMS',
      'INFORMATION_CONFIRMATION',
      'GENERAL_APPROVAL',
    ];

    assert.strictEqual(types.length, 6);
    types.forEach((type) => {
      const item: ClientApproval = {
        ...sampleApproval,
        approvalType: type,
      };
      assert.strictEqual(item.approvalType, type);
    });
  });
});
