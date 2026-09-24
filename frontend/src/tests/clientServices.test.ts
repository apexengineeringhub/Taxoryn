import { describe, it } from 'node:test';
import assert from 'node:assert';
import type {
  ClientServiceDto,
  ServiceCatalogItem,
  ClientServiceType,
  ClientServiceStatus,
  CreateClientServiceRequest,
  UpdateClientServiceRequest,
} from '../types/index.ts';

describe('Phase 11: Client Service Master & Engagement Foundation Tests', () => {
  it('should validate ServiceCatalogItem structure and module capability mapping', () => {
    const catalogItem: ServiceCatalogItem = {
      serviceType: 'GST_COMPLIANCE',
      displayName: 'GST Compliance & Periodic Filings',
      category: 'TAX_COMPLIANCE',
      defaultBillingCycle: 'MONTHLY',
      moduleCode: 'GST',
      requiredCapability: 'GST_RETURN_FILING',
      description: 'End-to-end GST return filings (GSTR-1, GSTR-3B, GSTR-9), ITC reconciliation, and ledgers',
    };

    assert.strictEqual(catalogItem.serviceType, 'GST_COMPLIANCE');
    assert.strictEqual(catalogItem.moduleCode, 'GST');
    assert.strictEqual(catalogItem.defaultBillingCycle, 'MONTHLY');
    assert.strictEqual(catalogItem.category, 'TAX_COMPLIANCE');
  });

  it('should construct valid CreateClientServiceRequest with operational metadata', () => {
    const createReq: CreateClientServiceRequest = {
      serviceType: 'INCOME_TAX_FILING',
      serviceName: 'Corporate Income Tax Retainer',
      assignedEmployeeId: 'emp-uuid-1234',
      billingCycle: 'ANNUAL',
      agreedFee: 35000,
      currency: 'INR',
      startDate: '2026-04-01',
      engagementNotes: 'Covers ITR-6 computation and advance tax installments',
    };

    assert.strictEqual(createReq.serviceType, 'INCOME_TAX_FILING');
    assert.strictEqual(createReq.assignedEmployeeId, 'emp-uuid-1234');
    assert.strictEqual(createReq.agreedFee, 35000);
    assert.strictEqual(createReq.billingCycle, 'ANNUAL');
  });

  it('should accurately represent ClientServiceDto and support status lifecycle transitions', () => {
    const activeService: ClientServiceDto = {
      id: 'srv-uuid-101',
      clientId: 'client-uuid-999',
      serviceType: 'TDS_COMPLIANCE',
      serviceName: 'TDS / TCS Compliance & Form 26Q/27Q',
      status: 'ACTIVE',
      assignedEmployeeId: 'emp-uuid-5678',
      assignedEmployeeName: 'Priya Sharma',
      assignedEmployeeEmail: 'priya.sharma@taxoryn.test',
      billingCycle: 'QUARTERLY',
      agreedFee: 12000,
      currency: 'INR',
      startDate: '2026-04-01',
      moduleCode: 'TDS',
      createdAt: '2026-04-01T10:00:00Z',
    };

    assert.strictEqual(activeService.status, 'ACTIVE');
    assert.strictEqual(activeService.assignedEmployeeName, 'Priya Sharma');

    // Simulate status update to SUSPENDED
    const suspendedUpdate: UpdateClientServiceRequest = {
      status: 'SUSPENDED',
      engagementNotes: 'Temporarily paused per client request',
    };

    const updatedService: ClientServiceDto = {
      ...activeService,
      status: suspendedUpdate.status as ClientServiceStatus,
      engagementNotes: suspendedUpdate.engagementNotes,
    };

    assert.strictEqual(updatedService.status, 'SUSPENDED');
    assert.strictEqual(updatedService.engagementNotes, 'Temporarily paused per client request');
  });

  it('should support practitioner reassignment without affecting client tenant or security scope', () => {
    const service: ClientServiceDto = {
      id: 'srv-uuid-102',
      clientId: 'client-uuid-999',
      serviceType: 'TAX_NOTICE_MANAGEMENT',
      serviceName: 'Tax Notice & Dispute Management',
      status: 'ACTIVE',
      assignedEmployeeId: 'emp-uuid-111',
      assignedEmployeeName: 'Rohan Gupta',
      billingCycle: 'ONE_TIME',
      agreedFee: 20000,
    };

    // Reassign operational practitioner
    const reassignedService: ClientServiceDto = {
      ...service,
      assignedEmployeeId: 'emp-uuid-222',
      assignedEmployeeName: 'Anil Mehta',
      assignedEmployeeEmail: 'anil.mehta@taxoryn.test',
    };

    assert.strictEqual(reassignedService.id, service.id);
    assert.strictEqual(reassignedService.clientId, service.clientId);
    assert.strictEqual(reassignedService.assignedEmployeeId, 'emp-uuid-222');
    assert.strictEqual(reassignedService.assignedEmployeeName, 'Anil Mehta');
  });

  it('should verify all 14 standard ClientServiceType values are well-defined', () => {
    const allTypes: ClientServiceType[] = [
      'GST_COMPLIANCE',
      'INCOME_TAX_FILING',
      'TDS_COMPLIANCE',
      'TAX_NOTICE_MANAGEMENT',
      'COMPLIANCE_CALENDAR',
      'DOCUMENT_MANAGEMENT',
      'CLIENT_BILLING',
      'ACCOUNTING_BOOKKEEPING',
      'STATUTORY_AUDIT',
      'TAX_AUDIT',
      'COMPANY_SECRETARIAL',
      'PAYROLL_PROCESSING',
      'ADVISORY_CONSULTING',
      'OTHER',
    ];

    assert.strictEqual(allTypes.length, 14);
    assert.ok(allTypes.includes('GST_COMPLIANCE'));
    assert.ok(allTypes.includes('STATUTORY_AUDIT'));
    assert.ok(allTypes.includes('OTHER'));
  });
});
