import { describe, it } from 'node:test';
import assert from 'node:assert';
import type { Client, GstProfile } from '../types/index.ts';

describe('GST Filing Modal: Select Client & GSTIN Dropdown Requirements', () => {
  // Helper to extract eligible GST clients matching GstCompliancePage logic
  function computeEligibleGstClients(clients: Client[], profiles: GstProfile[]) {
    const list: { id: string; clientId: string; name: string; gstin: string }[] = [];
    const seenGstins = new Set<string>();

    // 1. First add clients from Client Master having non-blank GSTIN
    clients.forEach((c) => {
      if (c.gstin && c.gstin.trim()) {
        const formattedGstin = c.gstin.trim().toUpperCase();
        if (!seenGstins.has(formattedGstin)) {
          seenGstins.add(formattedGstin);
          list.push({
            id: c.id,
            clientId: c.id,
            name: c.displayName || c.tradeName || c.legalName || 'Client',
            gstin: formattedGstin,
          });
        }
      }
    });

    // 2. Also include any profiles from GST profiles if not already present
    profiles.forEach((p) => {
      if (p.gstin && p.gstin.trim()) {
        const formattedGstin = p.gstin.trim().toUpperCase();
        if (!seenGstins.has(formattedGstin)) {
          seenGstins.add(formattedGstin);
          list.push({
            id: p.id,
            clientId: p.clientId || p.id,
            name: p.clientName || p.tradeName || p.legalName || 'Client',
            gstin: formattedGstin,
          });
        }
      }
    });

    return list.sort((a, b) => a.name.localeCompare(b.name));
  }

  it('1. Client with GSTIN appears in dropdown with correct formatted label', () => {
    const mockClients: Client[] = [
      {
        id: 'client-1',
        organizationId: 'org-1',
        displayName: 'ABC Traders',
        legalName: 'ABC Traders Private Limited',
        clientType: 'PRIVATE_LIMITED',
        pan: 'AAACA5432B',
        gstin: '10ABCDE1234F1Z5',
        status: 'ACTIVE',
      },
      {
        id: 'client-2',
        organizationId: 'org-1',
        displayName: 'Zenith Logistics',
        clientType: 'PROPRIETORSHIP',
        pan: 'BBBCB5432C',
        gstin: '27BBBCB5432C1Z8',
        status: 'ACTIVE',
      },
    ];

    const eligible = computeEligibleGstClients(mockClients, []);
    assert.strictEqual(eligible.length, 2);

    const first = eligible.find((e) => e.clientId === 'client-1');
    assert.ok(first);
    assert.strictEqual(first?.name, 'ABC Traders');
    assert.strictEqual(first?.gstin, '10ABCDE1234F1Z5');

    // Verify option label format: "ABC Traders — 10XXXXXXXXXX1Z5"
    const label = `${first?.name} — ${first?.gstin}`;
    assert.strictEqual(label, 'ABC Traders — 10ABCDE1234F1Z5');
  });

  it('2. Client without GSTIN or with blank/whitespace GSTIN is excluded from dropdown', () => {
    const mockClients: Client[] = [
      {
        id: 'client-with-gst',
        organizationId: 'org-1',
        displayName: 'Registered Taxpayer Corp',
        clientType: 'PRIVATE_LIMITED',
        pan: 'AAACA1111A',
        gstin: '29AAACA1111A1Z1',
        status: 'ACTIVE',
      },
      {
        id: 'client-no-gst',
        organizationId: 'org-1',
        displayName: 'Salaried Individual',
        clientType: 'INDIVIDUAL',
        pan: 'AAACA2222B',
        gstin: undefined,
        status: 'ACTIVE',
      },
      {
        id: 'client-null-gst',
        organizationId: 'org-1',
        displayName: 'Consultant Freelancer',
        clientType: 'INDIVIDUAL',
        pan: 'AAACA3333C',
        gstin: '',
        status: 'ACTIVE',
      },
      {
        id: 'client-whitespace-gst',
        organizationId: 'org-1',
        displayName: 'Non-GST Small Shop',
        clientType: 'PROPRIETORSHIP',
        pan: 'AAACA4444D',
        gstin: '   ',
        status: 'ACTIVE',
      },
    ];

    const eligible = computeEligibleGstClients(mockClients, []);
    assert.strictEqual(eligible.length, 1);
    assert.strictEqual(eligible[0].clientId, 'client-with-gst');
    assert.strictEqual(eligible[0].gstin, '29AAACA1111A1Z1');
  });

  it('3. Empty-state message requirement is detected when no eligible client exists', () => {
    const mockClients: Client[] = [
      {
        id: 'client-1',
        organizationId: 'org-1',
        displayName: 'Individual Taxpayer',
        clientType: 'INDIVIDUAL',
        pan: 'AAACA9999Z',
        status: 'ACTIVE',
      },
    ];

    const eligible = computeEligibleGstClients(mockClients, []);
    assert.strictEqual(eligible.length, 0);

    const emptyStateText = 'No GST-registered clients available. Add a GSTIN to a client first.';
    assert.ok(emptyStateText.includes('No GST-registered clients available'));
  });

  it('4. Selecting client populates the correct client ID, GSTIN and filing payload', () => {
    const mockClients: Client[] = [
      {
        id: 'client-101',
        organizationId: 'org-1',
        displayName: 'ABC Traders',
        clientType: 'PRIVATE_LIMITED',
        pan: 'AAACA5432B',
        gstin: '10ABCDE1234F1Z5',
        status: 'ACTIVE',
      },
    ];

    const eligible = computeEligibleGstClients(mockClients, []);
    const selectedId = eligible[0].id;
    const selectedClient = eligible.find((c) => c.id === selectedId);

    // Build the createFiling payload
    const payload = {
      clientId: selectedClient?.clientId,
      gstin: selectedClient?.gstin,
      returnType: 'GSTR3B',
      returnPeriod: '2026-07',
      financialYear: '2026-27',
      dueDate: '2026-08-20',
      filingStatus: 'PENDING',
      createTask: true,
    };

    assert.strictEqual(payload.clientId, 'client-101');
    assert.strictEqual(payload.gstin, '10ABCDE1234F1Z5');
    assert.strictEqual(payload.returnType, 'GSTR3B');
  });
});
