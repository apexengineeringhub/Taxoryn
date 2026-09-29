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
    FOUNDATION: [],
    BUSINESS: [],
    OPTIONAL: [],
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

export function isNonConfigurableModule(module: OrganizationModule): boolean {
  return module.mandatory === true || module.category === 'CORE' || module.category === 'FOUNDATION';
}

export function canToggleModule(module: OrganizationModule): boolean {
  if (isNonConfigurableModule(module)) {
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
      if (isNonConfigurableModule(m) && !payload.enabled) {
        // Cannot disable core or foundation modules
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

// Sample Catalog Mock with 4-tier Classification
export const MOCK_CATALOG: ProductModule[] = [
  {
    id: 'pm-1',
    code: 'CLIENTS',
    name: 'Client Management (360°)',
    description: 'Master client directory, contacts, PAN/GSTIN repository, and client profile management.',
    category: 'FOUNDATION',
    displayOrder: 1,
    enabledByDefault: true,
    mandatory: true,
    configurable: false,
    subscriptionControlled: false,
    usageControlled: true,
    status: 'ACTIVE',
  },
  {
    id: 'pm-2',
    code: 'TASKS',
    name: 'Tasks & Workflow Management',
    description: 'Task assignments, workflow stages, recurring job generator, and dead-line tracking.',
    category: 'FOUNDATION',
    displayOrder: 2,
    enabledByDefault: true,
    mandatory: true,
    configurable: false,
    subscriptionControlled: false,
    usageControlled: true,
    status: 'ACTIVE',
  },
  {
    id: 'pm-8',
    code: 'GST',
    name: 'GST Compliance Suite',
    description: 'GSTR-1, GSTR-3B, GSTR-9 filing tracker, 2B vs Purchase recon, and GST taxpayer hub.',
    category: 'BUSINESS',
    displayOrder: 10,
    enabledByDefault: true,
    mandatory: false,
    configurable: true,
    subscriptionControlled: true,
    usageControlled: false,
    status: 'ACTIVE',
  },
  {
    id: 'pm-9',
    code: 'ITR',
    name: 'Income Tax (ITR) Compliance',
    description: 'ITR filing lifecycle, advance tax computations, AIS/TIS tracker, and refund monitoring.',
    category: 'BUSINESS',
    displayOrder: 11,
    enabledByDefault: true,
    mandatory: false,
    configurable: true,
    subscriptionControlled: true,
    usageControlled: false,
    status: 'ACTIVE',
  },
  {
    id: 'pm-10',
    code: 'TDS',
    name: 'TDS & TCS Returns Suite',
    description: 'Form 24Q, 26Q, 27Q filing management, challan ITNS 281 verification, and 16A generation.',
    category: 'BUSINESS',
    displayOrder: 12,
    enabledByDefault: true,
    mandatory: false,
    configurable: true,
    subscriptionControlled: true,
    usageControlled: false,
    status: 'ACTIVE',
  },
  {
    id: 'pm-11',
    code: 'TAX_NOTICES',
    name: 'Notice Management Center',
    description: 'Centralized IT/GST notice intake, hearing dates, response drafting, and order archives.',
    category: 'BUSINESS',
    displayOrder: 13,
    enabledByDefault: true,
    mandatory: false,
    configurable: true,
    subscriptionControlled: true,
    usageControlled: false,
    status: 'ACTIVE',
  },
  {
    id: 'pm-12',
    code: 'BILLING',
    name: 'Practice Billing & Invoicing',
    description: 'Fee schedules, GST-compliant proforma & tax invoices, payment tracking, and receipts.',
    category: 'FOUNDATION',
    displayOrder: 20,
    enabledByDefault: true,
    mandatory: true,
    configurable: false,
    subscriptionControlled: false,
    usageControlled: false,
    status: 'ACTIVE',
  },
  {
    id: 'pm-13',
    code: 'REPORTS',
    name: 'Central Reports & BI Analytics',
    description: 'Aggregated compliance progress, partner utilization, revenue, and collection reports.',
    category: 'FOUNDATION',
    displayOrder: 21,
    enabledByDefault: true,
    mandatory: true,
    configurable: false,
    subscriptionControlled: false,
    usageControlled: false,
    status: 'ACTIVE',
  },
  {
    id: 'pm-14',
    code: 'MARKETPLACE',
    name: 'Taxoryn Marketplace Presence',
    description: 'Public profile listing, inbound client leads, and direct client engagement channel.',
    category: 'OPTIONAL',
    displayOrder: 30,
    enabledByDefault: false,
    mandatory: false,
    configurable: true,
    subscriptionControlled: true,
    usageControlled: false,
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
  mandatory: cat.mandatory,
  configurable: cat.configurable,
  subscriptionControlled: cat.subscriptionControlled,
  usageControlled: cat.usageControlled,
  updatedAt: '2026-09-22T00:00:00Z',
}));

describe('Product Module Catalog & Organization Configuration', () => {
  it('1. Module catalog contains expected taxonomy categories', () => {
    const categories = new Set(MOCK_CATALOG.map((m) => m.category));
    assert.strictEqual(categories.has('FOUNDATION'), true);
    assert.strictEqual(categories.has('BUSINESS'), true);
    assert.strictEqual(categories.has('OPTIONAL'), true);
  });

  it('2. Safe defaults: foundation and business modules default to enabled, marketplace defaults to false', () => {
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

    assert.strictEqual(grouped.FOUNDATION.length, 4); // CLIENTS, TASKS, BILLING, REPORTS
    assert.strictEqual(grouped.BUSINESS.length, 4);   // GST, ITR, TDS, TAX_NOTICES
    assert.strictEqual(grouped.OPTIONAL.length, 1);   // MARKETPLACE

    // Verify items in BUSINESS
    assert.strictEqual(grouped.BUSINESS[0].moduleCode, 'GST');
    assert.strictEqual(grouped.BUSINESS[1].moduleCode, 'ITR');
    assert.strictEqual(grouped.BUSINESS[2].moduleCode, 'TDS');
    assert.strictEqual(grouped.BUSINESS[3].moduleCode, 'TAX_NOTICES');
  });

  it('4. Foundation modules cannot be disabled (canToggleModule)', () => {
    const clientsMod = MOCK_ORG_MODULES.find((m) => m.moduleCode === 'CLIENTS')!;
    assert.strictEqual(canToggleModule(clientsMod), false);

    const gstMod = MOCK_ORG_MODULES.find((m) => m.moduleCode === 'GST')!;
    assert.strictEqual(canToggleModule(gstMod), true);
  });

  it('5. Disabling a foundation module in applyModuleToggle is safely ignored', () => {
    const result = applyModuleToggle(MOCK_ORG_MODULES, 'CLIENTS', { enabled: false });
    const updatedClients = result.find((m) => m.moduleCode === 'CLIENTS')!;
    assert.strictEqual(updatedClients.enabled, true); // Stays true
  });

  it('6. Business / Optional module can be toggled on/off', () => {
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

  it('7. Handles empty category grouping gracefully', () => {
    const grouped = groupModulesByCategory([]);
    assert.strictEqual(grouped.CORE.length, 0);
    assert.strictEqual(grouped.FOUNDATION.length, 0);
    assert.strictEqual(grouped.BUSINESS.length, 0);
    assert.strictEqual(grouped.OPTIONAL.length, 0);
  });
});
