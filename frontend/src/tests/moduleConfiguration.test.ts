import { describe, it } from 'node:test';
import assert from 'node:assert';
import type {
  ProductModuleCode,
  ProductModuleCategory,
  ProductModule,
  OrganizationModule,
  UpdateOrganizationModulePayload,
} from '../types/index.ts';

// Helper functions matching frontend module configuration logic
export function groupModulesByCategory(modules: OrganizationModule[]): Record<ProductModuleCategory, OrganizationModule[]> {
  const groups: Record<ProductModuleCategory, OrganizationModule[]> = {
    CORE: [],
    TAX: [],
    PRACTICE_OPERATIONS: [],
    NETWORK_GROWTH: [],
  };

  modules.forEach((mod) => {
    if (groups[mod.category]) {
      groups[mod.category].push(mod);
    } else {
      groups.CORE.push(mod);
    }
  });

  return groups;
}

export function isCoreModule(code: ProductModuleCode): boolean {
  const coreCodes: ProductModuleCode[] = [
    'CLIENTS',
    'TASKS',
    'DOCUMENTS',
    'DOCUMENT_REQUESTS',
    'CLIENT_PORTAL',
    'NOTIFICATIONS',
    'AUDIT',
  ];
  return coreCodes.includes(code);
}

export function canToggleModule(module: OrganizationModule): boolean {
  // Core modules cannot be disabled
  if (isCoreModule(module.moduleCode)) {
    return false;
  }
  return true;
}

export function applyModuleToggle(
  currentModules: OrganizationModule[],
  moduleCode: ProductModuleCode,
  payload: UpdateOrganizationModulePayload
): OrganizationModule[] {
  return currentModules.map((m) => {
    if (m.moduleCode === moduleCode) {
      if (isCoreModule(m.moduleCode) && !payload.enabled) {
        // Cannot disable core modules
        return m;
      }
      return {
        ...m,
        enabled: payload.enabled,
        explicitlyConfigured: true,
        updatedAt: new Date().toISOString(),
      };
    }
    return m;
  });
}

// Sample Catalog Mock
export const MOCK_CATALOG: ProductModule[] = [
  {
    id: 'pm-1',
    code: 'CLIENTS',
    name: 'Client Management (360°)',
    description: 'Master client directory, contacts, PAN/GSTIN repository, and client profile management.',
    category: 'CORE',
    displayOrder: 1,
    enabledByDefault: true,
    status: 'ACTIVE',
  },
  {
    id: 'pm-2',
    code: 'TASKS',
    name: 'Tasks & Workflow Management',
    description: 'Task assignments, workflow stages, recurring job generator, and dead-line tracking.',
    category: 'CORE',
    displayOrder: 2,
    enabledByDefault: true,
    status: 'ACTIVE',
  },
  {
    id: 'pm-8',
    code: 'GST',
    name: 'GST Compliance Suite',
    description: 'GSTR-1, GSTR-3B, GSTR-9 filing tracker, 2B vs Purchase recon, and GST taxpayer hub.',
    category: 'TAX',
    displayOrder: 10,
    enabledByDefault: true,
    status: 'ACTIVE',
  },
  {
    id: 'pm-9',
    code: 'ITR',
    name: 'Income Tax (ITR) Compliance',
    description: 'ITR filing lifecycle, advance tax computations, AIS/TIS tracker, and refund monitoring.',
    category: 'TAX',
    displayOrder: 11,
    enabledByDefault: true,
    status: 'ACTIVE',
  },
  {
    id: 'pm-10',
    code: 'TDS',
    name: 'TDS & TCS Returns Suite',
    description: 'Form 24Q, 26Q, 27Q filing management, challan ITNS 281 verification, and 16A generation.',
    category: 'TAX',
    displayOrder: 12,
    enabledByDefault: true,
    status: 'ACTIVE',
  },
  {
    id: 'pm-11',
    code: 'TAX_NOTICES',
    name: 'Notice Management Center',
    description: 'Centralized IT/GST notice intake, hearing dates, response drafting, and order archives.',
    category: 'TAX',
    displayOrder: 13,
    enabledByDefault: true,
    status: 'ACTIVE',
  },
  {
    id: 'pm-12',
    code: 'BILLING',
    name: 'Practice Billing & Invoicing',
    description: 'Fee schedules, GST-compliant proforma & tax invoices, payment tracking, and receipts.',
    category: 'PRACTICE_OPERATIONS',
    displayOrder: 20,
    enabledByDefault: true,
    status: 'ACTIVE',
  },
  {
    id: 'pm-13',
    code: 'REPORTS',
    name: 'Central Reports & BI Analytics',
    description: 'Aggregated compliance progress, partner utilization, revenue, and collection reports.',
    category: 'PRACTICE_OPERATIONS',
    displayOrder: 21,
    enabledByDefault: true,
    status: 'ACTIVE',
  },
  {
    id: 'pm-14',
    code: 'MARKETPLACE',
    name: 'Taxoryn Marketplace Presence',
    description: 'Public profile listing, inbound client leads, and direct client engagement channel.',
    category: 'NETWORK_GROWTH',
    displayOrder: 30,
    enabledByDefault: false,
    status: 'ACTIVE',
  },
];

export const MOCK_ORG_MODULES: OrganizationModule[] = MOCK_CATALOG.map((cat) => ({
  organizationId: 'org-test-123',
  moduleCode: cat.code,
  moduleName: cat.name,
  moduleDescription: cat.description,
  category: cat.category,
  enabled: cat.enabledByDefault,
  explicitlyConfigured: false,
  updatedAt: '2026-09-22T00:00:00Z',
}));

describe('Product Module Catalog & Organization Configuration', () => {
  it('1. Module catalog contains expected taxonomy categories', () => {
    const categories = new Set(MOCK_CATALOG.map((m) => m.category));
    assert.strictEqual(categories.has('CORE'), true);
    assert.strictEqual(categories.has('TAX'), true);
    assert.strictEqual(categories.has('PRACTICE_OPERATIONS'), true);
    assert.strictEqual(categories.has('NETWORK_GROWTH'), true);
  });

  it('2. Safe defaults: core and tax modules default to enabled, marketplace defaults to false', () => {
    const clients = MOCK_CATALOG.find((m) => m.code === 'CLIENTS');
    const tasks = MOCK_CATALOG.find((m) => m.code === 'TASKS');
    const gst = MOCK_CATALOG.find((m) => m.code === 'GST');
    const itr = MOCK_CATALOG.find((m) => m.code === 'ITR');
    const tds = MOCK_CATALOG.find((m) => m.code === 'TDS');
    const marketplace = MOCK_CATALOG.find((m) => m.code === 'MARKETPLACE');

    assert.strictEqual(clients?.enabledByDefault, true);
    assert.strictEqual(tasks?.enabledByDefault, true);
    assert.strictEqual(gst?.enabledByDefault, true);
    assert.strictEqual(itr?.enabledByDefault, true);
    assert.strictEqual(tds?.enabledByDefault, true);
    assert.strictEqual(marketplace?.enabledByDefault, false);
  });

  it('3. Grouping modules by category maintains correct grouping', () => {
    const grouped = groupModulesByCategory(MOCK_ORG_MODULES);

    assert.strictEqual(grouped.CORE.length, 2);
    assert.strictEqual(grouped.TAX.length, 4);
    assert.strictEqual(grouped.PRACTICE_OPERATIONS.length, 2);
    assert.strictEqual(grouped.NETWORK_GROWTH.length, 1);

    // Verify items in TAX
    assert.strictEqual(grouped.TAX[0].moduleCode, 'GST');
    assert.strictEqual(grouped.TAX[1].moduleCode, 'ITR');
    assert.strictEqual(grouped.TAX[2].moduleCode, 'TDS');
    assert.strictEqual(grouped.TAX[3].moduleCode, 'TAX_NOTICES');
  });

  it('4. Core modules cannot be disabled (isCore check and canToggleModule)', () => {
    const clientsMod = MOCK_ORG_MODULES.find((m) => m.moduleCode === 'CLIENTS')!;
    assert.strictEqual(isCoreModule(clientsMod.moduleCode), true);
    assert.strictEqual(canToggleModule(clientsMod), false);

    const gstMod = MOCK_ORG_MODULES.find((m) => m.moduleCode === 'GST')!;
    assert.strictEqual(isCoreModule(gstMod.moduleCode), false);
    assert.strictEqual(canToggleModule(gstMod), true);
  });

  it('5. Disabling a core module in applyModuleToggle is safely ignored', () => {
    const result = applyModuleToggle(MOCK_ORG_MODULES, 'CLIENTS', { enabled: false });
    const updatedClients = result.find((m) => m.moduleCode === 'CLIENTS')!;
    assert.strictEqual(updatedClients.enabled, true); // Stays true
  });

  it('6. Non-core module can be toggled on/off', () => {
    // Disable GST
    const afterGstDisable = applyModuleToggle(MOCK_ORG_MODULES, 'GST', {
      enabled: false,
    });
    const gstMod = afterGstDisable.find((m) => m.moduleCode === 'GST')!;
    assert.strictEqual(gstMod.enabled, false);
    assert.strictEqual(gstMod.explicitlyConfigured, true);

    // Enable MARKETPLACE
    const afterMarketplaceEnable = applyModuleToggle(afterGstDisable, 'MARKETPLACE', {
      enabled: true,
    });
    const mktMod = afterMarketplaceEnable.find((m) => m.moduleCode === 'MARKETPLACE')!;
    assert.strictEqual(mktMod.enabled, true);
    assert.strictEqual(mktMod.explicitlyConfigured, true);
  });

  it('7. Handles empty or unknown category gracefully', () => {
    const grouped = groupModulesByCategory([]);
    assert.strictEqual(grouped.CORE.length, 0);
    assert.strictEqual(grouped.TAX.length, 0);
    assert.strictEqual(grouped.PRACTICE_OPERATIONS.length, 0);
    assert.strictEqual(grouped.NETWORK_GROWTH.length, 0);
  });
});
