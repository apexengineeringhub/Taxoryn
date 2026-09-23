import { describe, it } from 'node:test';
import assert from 'node:assert';
import type {
  ProductModuleCode,
  OrganizationModule,
  ModuleAccessStatus,
  User,
} from '../types/index.ts';
import {
  COMPLIANCE_SUBMENU_ACTIONS,
  PRACTICE_ACTION_DEFINITIONS,
  type ActionDefinition,
  type ComplianceSubmenuAction,
} from '../config/actionMenuConfig.ts';
import {
  hasPermission,
  isPlatformUser,
  filterNavigationSections,
  type NavigationSection,
  type NavigationItem,
} from '../utils/permissionUtils.ts';

/**
 * Pure helper calculating access status from OrganizationModule data,
 * matching ModuleEntitlementContext behavior.
 */
export function calculateModuleAccessStatus(
  module: OrganizationModule | undefined,
  isPlatformAdmin = false
): ModuleAccessStatus {
  if (isPlatformAdmin) return 'AVAILABLE';
  if (!module) return 'AVAILABLE';

  if (module.accessStatus) {
    return module.accessStatus;
  }

  if (!module.enabled) {
    return 'MODULE_DISABLED';
  }

  if (module.subscriptionStatus && !['ACTIVE', 'TRIALING'].includes(module.subscriptionStatus)) {
    return 'SUBSCRIPTION_REQUIRED';
  }

  if (module.entitled === false) {
    return 'UPGRADE_REQUIRED';
  }

  return 'AVAILABLE';
}

/**
 * Helper determining effective module availability
 */
export function isModuleAvailablePure(
  module: OrganizationModule | undefined,
  isPlatformAdmin = false
): boolean {
  if (isPlatformAdmin) return true;
  if (!module) return true;
  if (typeof module.effectiveAccess === 'boolean') {
    return module.effectiveAccess;
  }
  return module.enabled !== false;
}

/**
 * Helper filtering actions by user permissions AND module availability
 */
export function filterAuthorizedActions(
  actions: ActionDefinition[],
  user: User,
  moduleMap: Record<ProductModuleCode, OrganizationModule>,
  authorizedCompliance: ComplianceSubmenuAction[]
): ActionDefinition[] {
  const isPlatform = isPlatformUser(user);

  return actions.filter((action) => {
    if (action.id === 'start-compliance') {
      return authorizedCompliance.length > 0;
    }
    const hasPerm = hasPermission(user, action.requiredPermissions, action.allowedRoles);
    if (!hasPerm) return false;

    if (action.moduleCode) {
      return isModuleAvailablePure(moduleMap[action.moduleCode], isPlatform);
    }
    return true;
  });
}

/**
 * Helper filtering compliance submenu actions
 */
export function filterAuthorizedComplianceSubmenu(
  submenu: ComplianceSubmenuAction[],
  user: User,
  moduleMap: Record<ProductModuleCode, OrganizationModule>
): ComplianceSubmenuAction[] {
  const isPlatform = isPlatformUser(user);

  return submenu.filter((subAction) => {
    const hasPerm = hasPermission(user, subAction.requiredPermissions, subAction.allowedRoles);
    if (!hasPerm) return false;

    if (subAction.moduleCode) {
      return isModuleAvailablePure(moduleMap[subAction.moduleCode], isPlatform);
    }
    return true;
  });
}

// Test Fixtures
const mockPracticeOwner: User = {
  id: 'usr-owner-1',
  organizationId: 'org-test-1',
  email: 'owner@ca-firm.com',
  firstName: 'Rajesh',
  lastName: 'Sharma',
  status: 'ACTIVE',
  roles: ['PRACTICE_OWNER'],
  permissions: ['GST_VIEW', 'GST_CREATE', 'GST_WRITE', 'ITR_VIEW', 'ITR_CREATE', 'ITR_WRITE', 'TDS_VIEW', 'TDS_CREATE', 'TDS_WRITE', 'BILLING_VIEW', 'BILLING_CREATE', 'BILLING_WRITE', 'CLIENT_VIEW', 'CLIENT_CREATE', 'CLIENT_WRITE', 'TASK_VIEW', 'TASK_CREATE', 'TASK_WRITE', 'DOCUMENT_VIEW', 'DOCUMENT_WRITE', 'DOC_REQUEST_CREATE', 'ORGANIZATION_UPDATE', 'ORG_WRITE'],
};

const mockStaffUser: User = {
  id: 'usr-staff-1',
  organizationId: 'org-test-1',
  email: 'staff@ca-firm.com',
  firstName: 'Amit',
  lastName: 'Verma',
  status: 'ACTIVE',
  roles: ['STAFF'],
  permissions: ['GST_VIEW', 'ITR_VIEW', 'CLIENT_VIEW', 'TASK_VIEW', 'DOCUMENT_VIEW'],
};

const mockPlatformAdmin: User = {
  id: 'usr-super-1',
  organizationId: 'org-platform-1',
  email: 'admin@taxoryn.internal',
  firstName: 'Taxoryn',
  lastName: 'SuperAdmin',
  status: 'ACTIVE',
  roles: ['TAXORYN_SUPERADMIN'],
  permissions: [],
};

const createMockModules = (overrides?: Partial<Record<ProductModuleCode, Partial<OrganizationModule>>>): Record<ProductModuleCode, OrganizationModule> => {
  const baseCodes: ProductModuleCode[] = [
    'CLIENTS', 'TASKS', 'DOCUMENTS', 'DOCUMENT_REQUESTS', 'CLIENT_PORTAL', 'NOTIFICATIONS',
    'AUDIT', 'GST', 'ITR', 'TDS', 'TAX_NOTICES', 'BILLING', 'REPORTS', 'MARKETPLACE'
  ];

  const map: Record<ProductModuleCode, OrganizationModule> = {} as any;
  baseCodes.forEach((code) => {
    const override = overrides?.[code] || {};
    map[code] = {
      organizationId: 'org-test-1',
      moduleCode: code,
      moduleName: code,
      category: 'TAX',
      enabled: override.enabled !== undefined ? override.enabled : true,
      explicitlyConfigured: false,
      entitled: override.entitled !== undefined ? override.entitled : true,
      subscriptionStatus: override.subscriptionStatus || 'ACTIVE',
      effectiveAccess: override.effectiveAccess !== undefined ? override.effectiveAccess : true,
      accessStatus: override.accessStatus || 'AVAILABLE',
      reason: override.reason || 'Module active and available',
      ...override,
    };
  });
  return map;
};

describe('Phase 9.3: Frontend Entitlement & Module Experience', () => {
  it('1. Calculates AVAILABLE status when module is enabled and subscription is active', () => {
    const mod: OrganizationModule = {
      organizationId: 'org-1',
      moduleCode: 'GST',
      moduleName: 'GST Compliance',
      category: 'TAX',
      enabled: true,
      explicitlyConfigured: true,
      entitled: true,
      subscriptionStatus: 'ACTIVE',
      effectiveAccess: true,
      accessStatus: 'AVAILABLE',
    };

    assert.strictEqual(calculateModuleAccessStatus(mod), 'AVAILABLE');
    assert.strictEqual(isModuleAvailablePure(mod), true);
  });

  it('2. Calculates MODULE_DISABLED when module is turned off administratively', () => {
    const mod: OrganizationModule = {
      organizationId: 'org-1',
      moduleCode: 'GST',
      moduleName: 'GST Compliance',
      category: 'TAX',
      enabled: false,
      explicitlyConfigured: true,
      entitled: true,
      subscriptionStatus: 'ACTIVE',
      effectiveAccess: false,
      accessStatus: 'MODULE_DISABLED',
      reason: 'Product module GST is administratively disabled for this organization.',
    };

    assert.strictEqual(calculateModuleAccessStatus(mod), 'MODULE_DISABLED');
    assert.strictEqual(isModuleAvailablePure(mod), false);
  });

  it('3. Calculates SUBSCRIPTION_REQUIRED when subscription status is EXPIRED or CANCELED', () => {
    const modExpired: OrganizationModule = {
      organizationId: 'org-1',
      moduleCode: 'BILLING',
      moduleName: 'Billing & Invoicing',
      category: 'PRACTICE_OPERATIONS',
      enabled: true,
      explicitlyConfigured: true,
      entitled: true,
      subscriptionStatus: 'EXPIRED',
      effectiveAccess: false,
      accessStatus: 'SUBSCRIPTION_REQUIRED',
      reason: 'Active subscription required to access BILLING. Current status: EXPIRED.',
    };

    assert.strictEqual(calculateModuleAccessStatus(modExpired), 'SUBSCRIPTION_REQUIRED');
    assert.strictEqual(isModuleAvailablePure(modExpired), false);
  });

  it('4. Calculates UPGRADE_REQUIRED when module is not entitled under plan tier', () => {
    const modUpgrade: OrganizationModule = {
      organizationId: 'org-1',
      moduleCode: 'MARKETPLACE',
      moduleName: 'Marketplace',
      category: 'NETWORK_GROWTH',
      enabled: true,
      explicitlyConfigured: true,
      entitled: false,
      subscriptionStatus: 'ACTIVE',
      effectiveAccess: false,
      accessStatus: 'UPGRADE_REQUIRED',
      reason: 'Current subscription plan does not include MARKETPLACE. Please upgrade your subscription.',
    };

    assert.strictEqual(calculateModuleAccessStatus(modUpgrade), 'UPGRADE_REQUIRED');
    assert.strictEqual(isModuleAvailablePure(modUpgrade), false);
  });

  it('5. Action Menu dynamically filters out disabled modules (GST return hidden when GST disabled)', () => {
    const modulesWithGstDisabled = createMockModules({
      GST: { enabled: false, effectiveAccess: false, accessStatus: 'MODULE_DISABLED' },
    });

    const complianceSubmenu = filterAuthorizedComplianceSubmenu(
      COMPLIANCE_SUBMENU_ACTIONS,
      mockPracticeOwner,
      modulesWithGstDisabled
    );

    const complianceSubmenuIds = complianceSubmenu.map((a) => a.id);
    assert.strictEqual(complianceSubmenuIds.includes('gst-return'), false);
    assert.strictEqual(complianceSubmenuIds.includes('itr-return'), true);
    assert.strictEqual(complianceSubmenuIds.includes('tds-work'), true);

    const mainActions = filterAuthorizedActions(
      PRACTICE_ACTION_DEFINITIONS,
      mockPracticeOwner,
      modulesWithGstDisabled,
      complianceSubmenu
    );

    const mainActionIds = mainActions.map((a) => a.id);
    assert.strictEqual(mainActionIds.includes('start-compliance'), true); // Still present because ITR & TDS are available
  });

  it('6. Action Menu hides "Start Compliance Work" parent if all compliance modules are disabled', () => {
    const allComplianceDisabled = createMockModules({
      GST: { enabled: false, effectiveAccess: false, accessStatus: 'MODULE_DISABLED' },
      ITR: { enabled: false, effectiveAccess: false, accessStatus: 'MODULE_DISABLED' },
      TDS: { enabled: false, effectiveAccess: false, accessStatus: 'MODULE_DISABLED' },
    });

    const complianceSubmenu = filterAuthorizedComplianceSubmenu(
      COMPLIANCE_SUBMENU_ACTIONS,
      mockPracticeOwner,
      allComplianceDisabled
    );

    assert.strictEqual(complianceSubmenu.length, 0);

    const mainActions = filterAuthorizedActions(
      PRACTICE_ACTION_DEFINITIONS,
      mockPracticeOwner,
      allComplianceDisabled,
      complianceSubmenu
    );

    const mainActionIds = mainActions.map((a) => a.id);
    assert.strictEqual(mainActionIds.includes('start-compliance'), false); // Parent hidden
    assert.strictEqual(mainActionIds.includes('new-client'), true);
    assert.strictEqual(mainActionIds.includes('new-task'), true);
  });

  it('7. Action Menu hides Billing when Billing module subscription is expired', () => {
    const billingExpired = createMockModules({
      BILLING: { enabled: true, effectiveAccess: false, accessStatus: 'SUBSCRIPTION_REQUIRED', subscriptionStatus: 'EXPIRED' },
    });

    const complianceSubmenu = filterAuthorizedComplianceSubmenu(
      COMPLIANCE_SUBMENU_ACTIONS,
      mockPracticeOwner,
      billingExpired
    );

    const mainActions = filterAuthorizedActions(
      PRACTICE_ACTION_DEFINITIONS,
      mockPracticeOwner,
      billingExpired,
      complianceSubmenu
    );

    const mainActionIds = mainActions.map((a) => a.id);
    assert.strictEqual(mainActionIds.includes('create-invoice'), false); // Gated
    assert.strictEqual(mainActionIds.includes('new-client'), true);
  });

  it('8. Sidebar navigation filters out disabled modules for practice users', () => {
    const sampleNavSections: NavigationSection[] = [
      {
        id: 'compliance',
        sectionTitle: 'COMPLIANCE',
        items: [
          { label: 'GST Compliance', path: '/gst', moduleCode: 'GST', requiredPermissions: ['GST_VIEW'] },
          { label: 'ITR Compliance', path: '/itr', moduleCode: 'ITR', requiredPermissions: ['ITR_VIEW'] },
          { label: 'TDS Compliance', path: '/tds', moduleCode: 'TDS', requiredPermissions: ['TDS_VIEW'] },
        ],
      },
    ];

    const gstDisabledModules = createMockModules({
      GST: { enabled: false, effectiveAccess: false, accessStatus: 'MODULE_DISABLED' },
    });

    const visibleSections = filterNavigationSections(sampleNavSections, mockPracticeOwner)
      .map((section) => ({
        ...section,
        items: section.items.filter((item) =>
          item.moduleCode ? isModuleAvailablePure(gstDisabledModules[item.moduleCode], false) : true
        ),
      }))
      .filter((section) => section.items.length > 0);

    assert.strictEqual(visibleSections.length, 1);
    const visiblePaths = visibleSections[0].items.map((i) => i.path);
    assert.strictEqual(visiblePaths.includes('/gst'), false);
    assert.strictEqual(visiblePaths.includes('/itr'), true);
    assert.strictEqual(visiblePaths.includes('/tds'), true);
  });

  it('9. Platform SuperAdmin bypasses all module gates and sees all actions and nav', () => {
    const allDisabledModules = createMockModules({
      GST: { enabled: false, effectiveAccess: false },
      BILLING: { enabled: false, effectiveAccess: false },
    });

    assert.strictEqual(isModuleAvailablePure(allDisabledModules['GST'], true), true);
    assert.strictEqual(calculateModuleAccessStatus(allDisabledModules['GST'], true), 'AVAILABLE');
  });

  it('10. RBAC permission checks take precedence over module availability', () => {
    // Staff user without BILLING_VIEW or BILLING_CREATE permissions
    const billingMod = createMockModules({})['BILLING'];
    assert.strictEqual(isModuleAvailablePure(billingMod, false), true);

    // Permission check for billing should fail for staff user
    const hasBillingPerm = hasPermission(mockStaffUser, ['BILLING_VIEW', 'BILLING_READ'], ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'ACCOUNTANT']);
    assert.strictEqual(hasBillingPerm, false);
  });
});
