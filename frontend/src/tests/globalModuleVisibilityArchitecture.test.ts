import { describe, it } from 'node:test';
import assert from 'node:assert';
import type {
  ProductModuleCode,
  OrganizationModule,
  ModuleAccessStatus,
  User,
  Organization,
} from '../types/index.ts';
import {
  COMPLIANCE_SUBMENU_ACTIONS,
  PRACTICE_ACTION_DEFINITIONS,
  CLIENT_ACTION_DEFINITIONS,
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

// Helper to construct a mock module
const createMockModule = (
  code: ProductModuleCode,
  name: string,
  enabled: boolean,
  entitled = true,
  accessStatus?: ModuleAccessStatus
): OrganizationModule => ({
  organizationId: 'org-firm-1',
  moduleCode: code,
  moduleName: name,
  moduleDescription: `${name} description`,
  category: 'TAX',
  enabled,
  explicitlyConfigured: true,
  entitled,
  effectiveAccess: enabled && entitled,
  accessStatus: accessStatus || (enabled ? (entitled ? 'AVAILABLE' : 'UPGRADE_REQUIRED') : 'MODULE_DISABLED'),
});

// Helper to generate full module registry with custom enabled flags
const createModuleRegistry = (
  overrides?: Partial<Record<ProductModuleCode, boolean>>
): Record<ProductModuleCode, OrganizationModule> => {
  const codes: { code: ProductModuleCode; name: string }[] = [
    { code: 'CLIENTS', name: 'Client Management' },
    { code: 'TASKS', name: 'Tasks & Workflow' },
    { code: 'DOCUMENTS', name: 'Document Vault' },
    { code: 'DOCUMENT_REQUESTS', name: 'Document Requests' },
    { code: 'CLIENT_PORTAL', name: 'Client Portal' },
    { code: 'NOTIFICATIONS', name: 'Notification Center' },
    { code: 'AUDIT', name: 'Audit & Activity' },
    { code: 'GST', name: 'GST Compliance' },
    { code: 'ITR', name: 'ITR Compliance' },
    { code: 'TDS', name: 'TDS Compliance' },
    { code: 'TAX_NOTICES', name: 'Notice Center' },
    { code: 'BILLING', name: 'Billing & Invoicing' },
    { code: 'REPORTS', name: 'Reports & Analytics' },
    { code: 'MARKETPLACE', name: 'Marketplace' },
  ];

  const map = {} as Record<ProductModuleCode, OrganizationModule>;
  codes.forEach(({ code, name }) => {
    const isEnabled = overrides && overrides[code] !== undefined ? overrides[code]! : true;
    map[code] = createMockModule(code, name, isEnabled);
  });
  return map;
};

// Simulated mock users
const mockPracticeAdmin: User = {
  id: 'usr-admin-1',
  email: 'admin@firm.com',
  firstName: 'Admin',
  lastName: 'User',
  status: 'ACTIVE',
  organizationId: 'org-firm-1',
  roles: [{ id: '1', name: 'Practice Admin', code: 'PRACTICE_ADMIN' }],
  permissions: [
    'CLIENT_VIEW', 'CLIENT_CREATE', 'CLIENT_UPDATE',
    'TASK_VIEW', 'TASK_CREATE', 'TASK_WRITE',
    'DOCUMENT_VIEW', 'DOCUMENT_WRITE', 'DOC_REQUEST_CREATE',
    'GST_VIEW', 'GST_CREATE', 'GST_WRITE',
    'ITR_VIEW', 'ITR_CREATE', 'ITR_WRITE',
    'TDS_VIEW', 'TDS_CREATE', 'TDS_WRITE',
    'NOTICE_VIEW', 'TAX_NOTICE_VIEW',
    'BILLING_VIEW', 'BILLING_READ', 'BILLING_CREATE', 'BILLING_WRITE',
    'REPORT_VIEW', 'REPORTS_VIEW',
    'MARKETPLACE_LEAD_VIEW', 'MARKETPLACE_LEAD_MANAGE', 'MARKETPLACE_ONBOARDING_MANAGE', 'MARKETPLACE_MANAGE',
    'NOTIFICATION_VIEW', 'NOTIFICATION_CREATE', 'NOTIFICATION_MANAGE',
    'USER_VIEW', 'USER_CREATE', 'EMPLOYEE_CREATE', 'ROLE_READ',
    'ORGANIZATION_UPDATE', 'ORG_WRITE',
    'AUDIT_VIEW', 'AUDIT_READ', 'COMMUNICATION_MANAGE', 'SUBSCRIPTION_VIEW',
  ],
};

const mockClientUser: User = {
  id: 'usr-client-1',
  email: 'client@company.com',
  firstName: 'Client',
  lastName: 'User',
  status: 'ACTIVE',
  organizationId: 'org-firm-1',
  roles: [{ id: '2', name: 'Client User', code: 'CLIENT_USER' }],
  permissions: [],
};

// Pure visibility resolution functions matching frontend components
const isModuleAvailable = (
  moduleMap: Record<ProductModuleCode, OrganizationModule>,
  code?: ProductModuleCode
): boolean => {
  if (!code) return true;
  const mod = moduleMap[code];
  if (!mod) return true;
  return mod.enabled !== false && mod.entitled !== false;
};

const resolveSidebarSections = (
  sections: NavigationSection[],
  user: User,
  moduleMap: Record<ProductModuleCode, OrganizationModule>
): NavigationSection[] => {
  return filterNavigationSections(sections, user)
    .map((section) => ({
      ...section,
      items: section.items.filter((item) => isModuleAvailable(moduleMap, item.moduleCode)),
    }))
    .filter((section) => section.items.length > 0);
};

const resolveAuthorizedActions = (
  actions: ActionDefinition[],
  user: User,
  moduleMap: Record<ProductModuleCode, OrganizationModule>,
  complianceSubmenu: ComplianceSubmenuAction[],
  isSolo = false
): ActionDefinition[] => {
  const authorizedCompliance = complianceSubmenu.filter(
    (sub) =>
      hasPermission(user, sub.requiredPermissions, sub.allowedRoles) &&
      isModuleAvailable(moduleMap, sub.moduleCode)
  );

  return actions.filter((action) => {
    if (isSolo && action.id === 'add-employee') return false;
    if (action.id === 'start-compliance') return authorizedCompliance.length > 0;
    return (
      hasPermission(user, action.requiredPermissions, action.allowedRoles) &&
      isModuleAvailable(moduleMap, action.moduleCode)
    );
  });
};

const resolveClientPortalTabs = (
  tabs: { id: string; label: string; moduleCode?: ProductModuleCode }[],
  moduleMap: Record<ProductModuleCode, OrganizationModule>
) => {
  return tabs.filter((tab) => !tab.moduleCode || isModuleAvailable(moduleMap, tab.moduleCode));
};

describe('Taxoryn Global Disabled Module UI Visibility Architecture', () => {
  const practiceNavSections: NavigationSection[] = [
    {
      id: 'work',
      sectionTitle: 'WORK',
      items: [
        { label: 'Dashboard', path: '/dashboard' },
        { label: 'Clients 360°', path: '/clients', requiredPermissions: ['CLIENT_VIEW'], moduleCode: 'CLIENTS' },
        { label: 'Tasks & Workflow', path: '/tasks', requiredPermissions: ['TASK_VIEW'], moduleCode: 'TASKS' },
      ],
    },
    {
      id: 'compliance',
      sectionTitle: 'COMPLIANCE',
      items: [
        { label: 'GST Compliance', path: '/gst', requiredPermissions: ['GST_VIEW'], moduleCode: 'GST' },
        { label: 'ITR Compliance', path: '/itr', requiredPermissions: ['ITR_VIEW'], moduleCode: 'ITR' },
        { label: 'TDS Compliance', path: '/tds', requiredPermissions: ['ITR_VIEW', 'GST_VIEW', 'TASK_VIEW'], moduleCode: 'TDS' },
        { label: 'Notice Center', path: '/tax-notices', requiredPermissions: ['NOTICE_VIEW'], moduleCode: 'TAX_NOTICES' },
      ],
    },
    {
      id: 'documents',
      sectionTitle: 'DOCUMENTS',
      items: [
        { label: 'Document Vault', path: '/documents', requiredPermissions: ['DOCUMENT_VIEW'], moduleCode: 'DOCUMENTS' },
      ],
    },
    {
      id: 'practice',
      sectionTitle: 'PRACTICE',
      items: [
        { label: 'Client Portal Hub', path: '/portal', requiredPermissions: ['CLIENT_VIEW', 'CLIENT_UPDATE'], allowedRoles: ['PRACTICE_ADMIN'], moduleCode: 'CLIENT_PORTAL' },
        { label: 'Reports', path: '/reports', requiredPermissions: ['REPORT_VIEW'], allowedRoles: ['PRACTICE_ADMIN'], moduleCode: 'REPORTS' },
        { label: 'Inbound Leads (CRM)', path: '/marketplace/leads', requiredPermissions: ['MARKETPLACE_LEAD_VIEW'], allowedRoles: ['PRACTICE_ADMIN'], moduleCode: 'MARKETPLACE' },
        { label: 'Notification Center', path: '/notifications', requiredPermissions: ['NOTIFICATION_VIEW'], allowedRoles: ['PRACTICE_ADMIN'], moduleCode: 'NOTIFICATIONS' },
      ],
    },
    {
      id: 'administration',
      sectionTitle: 'ADMINISTRATION',
      items: [
        { label: 'Modules & Features', path: '/settings/modules', requiredPermissions: ['ORGANIZATION_UPDATE'], allowedRoles: ['PRACTICE_ADMIN'] },
        { label: 'Notice Operations', path: '/settings/tax-notices', requiredPermissions: ['ORGANIZATION_UPDATE'], allowedRoles: ['PRACTICE_ADMIN'], moduleCode: 'TAX_NOTICES' },
        { label: 'Billing & Invoices', path: '/billing', requiredPermissions: ['BILLING_VIEW'], allowedRoles: ['PRACTICE_ADMIN'], moduleCode: 'BILLING' },
        { label: 'Activity & Audit', path: '/audit-logs', requiredPermissions: ['AUDIT_VIEW'], allowedRoles: ['PRACTICE_ADMIN'], moduleCode: 'AUDIT' },
      ],
    },
  ];

  const clientNavSections: NavigationSection[] = [
    {
      id: 'my-tax',
      sectionTitle: 'MY TAX',
      items: [
        { label: 'Portal Dashboard', path: '/portal', moduleCode: 'CLIENT_PORTAL' },
        { label: 'GST Returns', path: '/portal?tab=gst', moduleCode: 'GST' },
        { label: 'ITR Returns', path: '/portal?tab=itr', moduleCode: 'ITR' },
        { label: 'TDS Statements', path: '/portal?tab=tds', moduleCode: 'TDS' },
        { label: 'Invoices & Due Bills', path: '/portal?tab=invoices', moduleCode: 'BILLING' },
        { label: 'Document Vault', path: '/portal?tab=documents', moduleCode: 'DOCUMENTS' },
      ],
    },
    {
      id: 'explore',
      sectionTitle: 'EXPLORE',
      items: [
        { label: 'Find a Tax Professional', path: '/marketplace/explore', moduleCode: 'MARKETPLACE' },
      ],
    },
  ];

  const clientTabs = [
    { id: 'overview', label: 'Overview' },
    { id: 'gst', label: 'GST', moduleCode: 'GST' as ProductModuleCode },
    { id: 'itr', label: 'ITR', moduleCode: 'ITR' as ProductModuleCode },
    { id: 'tds', label: 'TDS', moduleCode: 'TDS' as ProductModuleCode },
    { id: 'invoices', label: 'Bills', moduleCode: 'BILLING' as ProductModuleCode },
    { id: 'documents', label: 'Documents', moduleCode: 'DOCUMENTS' as ProductModuleCode },
    { id: 'messages', label: 'Messages', moduleCode: 'CLIENT_PORTAL' as ProductModuleCode },
  ];

  it('1. ALL modules ENABLED: Sidebar, Action Menu, and Client Portal expose all features', () => {
    const allEnabled = createModuleRegistry();

    // Practice Sidebar
    const practiceSections = resolveSidebarSections(practiceNavSections, mockPracticeAdmin, allEnabled);
    const allLabels = practiceSections.flatMap((s) => s.items.map((i) => i.label));
    assert.ok(allLabels.includes('Billing & Invoices'));
    assert.ok(allLabels.includes('GST Compliance'));
    assert.ok(allLabels.includes('ITR Compliance'));
    assert.ok(allLabels.includes('TDS Compliance'));
    assert.ok(allLabels.includes('Notice Center'));
    assert.ok(allLabels.includes('Document Vault'));
    assert.ok(allLabels.includes('Modules & Features'));

    // Practice Actions
    const actions = resolveAuthorizedActions(
      PRACTICE_ACTION_DEFINITIONS,
      mockPracticeAdmin,
      allEnabled,
      COMPLIANCE_SUBMENU_ACTIONS
    );
    const actionIds = actions.map((a) => a.id);
    assert.ok(actionIds.includes('create-invoice'));
    assert.ok(actionIds.includes('start-compliance'));
    assert.ok(actionIds.includes('request-documents'));
    assert.ok(actionIds.includes('new-client'));

    // Client Sidebar
    const clientSections = resolveSidebarSections(clientNavSections, mockClientUser, allEnabled);
    const clientLabels = clientSections.flatMap((s) => s.items.map((i) => i.label));
    assert.ok(clientLabels.includes('GST Returns'));
    assert.ok(clientLabels.includes('ITR Returns'));
    assert.ok(clientLabels.includes('Invoices & Due Bills'));
    assert.ok(clientLabels.includes('Document Vault'));

    // Client Portal Tabs
    const visibleTabs = resolveClientPortalTabs(clientTabs, allEnabled);
    assert.strictEqual(visibleTabs.length, 7);
  });

  it('2. BILLING DISABLED: Billing completely absent from Sidebar, Actions, and Client Portal', () => {
    const billingDisabled = createModuleRegistry({ BILLING: false });

    // Practice Sidebar
    const practiceSections = resolveSidebarSections(practiceNavSections, mockPracticeAdmin, billingDisabled);
    const allLabels = practiceSections.flatMap((s) => s.items.map((i) => i.label));
    assert.strictEqual(allLabels.includes('Billing & Invoices'), false, 'Billing must be hidden from sidebar');
    assert.ok(allLabels.includes('GST Compliance'), 'GST must remain visible');
    assert.ok(allLabels.includes('Modules & Features'), 'Admin must still access Modules & Features');

    // Practice Actions
    const actions = resolveAuthorizedActions(
      PRACTICE_ACTION_DEFINITIONS,
      mockPracticeAdmin,
      billingDisabled,
      COMPLIANCE_SUBMENU_ACTIONS
    );
    const actionIds = actions.map((a) => a.id);
    assert.strictEqual(actionIds.includes('create-invoice'), false, 'Create invoice action must be absent');
    assert.ok(actionIds.includes('new-client'), 'New client action must remain');

    // Client Sidebar
    const clientSections = resolveSidebarSections(clientNavSections, mockClientUser, billingDisabled);
    const clientLabels = clientSections.flatMap((s) => s.items.map((i) => i.label));
    assert.strictEqual(clientLabels.includes('Invoices & Due Bills'), false, 'Client invoices must be hidden');
    assert.ok(clientLabels.includes('GST Returns'), 'Client GST must remain');

    // Client Portal Tabs
    const visibleTabs = resolveClientPortalTabs(clientTabs, billingDisabled);
    const tabIds = visibleTabs.map((t) => t.id);
    assert.strictEqual(tabIds.includes('invoices'), false, 'Invoices tab must be absent');
    assert.ok(tabIds.includes('gst'));
    assert.ok(tabIds.includes('overview'));
  });

  it('3. MULTIPLE MODULES DISABLED (GST, TDS, TAX_NOTICES, BILLING): Independent absence across all UI', () => {
    const multiDisabled = createModuleRegistry({
      GST: false,
      TDS: false,
      TAX_NOTICES: false,
      BILLING: false,
    });

    // Practice Sidebar
    const practiceSections = resolveSidebarSections(practiceNavSections, mockPracticeAdmin, multiDisabled);
    const allLabels = practiceSections.flatMap((s) => s.items.map((i) => i.label));
    assert.strictEqual(allLabels.includes('GST Compliance'), false);
    assert.strictEqual(allLabels.includes('TDS Compliance'), false);
    assert.strictEqual(allLabels.includes('Notice Center'), false);
    assert.strictEqual(allLabels.includes('Notice Operations'), false);
    assert.strictEqual(allLabels.includes('Billing & Invoices'), false);

    // Remaining enabled modules must remain visible
    assert.ok(allLabels.includes('ITR Compliance'));
    assert.ok(allLabels.includes('Document Vault'));
    assert.ok(allLabels.includes('Clients 360°'));
    assert.ok(allLabels.includes('Modules & Features'));

    // Compliance parent action should still appear because ITR is enabled
    const actions = resolveAuthorizedActions(
      PRACTICE_ACTION_DEFINITIONS,
      mockPracticeAdmin,
      multiDisabled,
      COMPLIANCE_SUBMENU_ACTIONS
    );
    const actionIds = actions.map((a) => a.id);
    assert.ok(actionIds.includes('start-compliance'), 'Start compliance remains because ITR is enabled');

    // Client Portal Tabs
    const visibleTabs = resolveClientPortalTabs(clientTabs, multiDisabled);
    const tabIds = visibleTabs.map((t) => t.id);
    assert.strictEqual(tabIds.includes('gst'), false);
    assert.strictEqual(tabIds.includes('tds'), false);
    assert.strictEqual(tabIds.includes('invoices'), false);
    assert.ok(tabIds.includes('itr'));
    assert.ok(tabIds.includes('documents'));
    assert.ok(tabIds.includes('overview'));
  });

  it('4. ALL COMPLIANCE DISABLED (GST, ITR, TDS): Start Compliance parent action disappears from menu', () => {
    const noCompliance = createModuleRegistry({
      GST: false,
      ITR: false,
      TDS: false,
    });

    const actions = resolveAuthorizedActions(
      PRACTICE_ACTION_DEFINITIONS,
      mockPracticeAdmin,
      noCompliance,
      COMPLIANCE_SUBMENU_ACTIONS
    );
    const actionIds = actions.map((a) => a.id);
    assert.strictEqual(
      actionIds.includes('start-compliance'),
      false,
      'Start compliance parent action must disappear when all compliance modules are disabled'
    );
    assert.ok(actionIds.includes('new-client'));
    assert.ok(actionIds.includes('request-documents'));
  });

  it('5. RE-ENABLING MODULE: Immediately restores visibility', () => {
    const disabledMap = createModuleRegistry({ BILLING: false, GST: false });
    let visibleTabs = resolveClientPortalTabs(clientTabs, disabledMap);
    assert.strictEqual(visibleTabs.map((t) => t.id).includes('gst'), false);
    assert.strictEqual(visibleTabs.map((t) => t.id).includes('invoices'), false);

    // Re-enable GST
    const restoredMap = createModuleRegistry({ BILLING: false, GST: true });
    visibleTabs = resolveClientPortalTabs(clientTabs, restoredMap);
    assert.ok(visibleTabs.map((t) => t.id).includes('gst'), 'GST tab must immediately reappear');
    assert.strictEqual(visibleTabs.map((t) => t.id).includes('invoices'), false, 'Billing remains hidden');
  });

  it('6. ADMIN ACCESS: Modules & Features (/settings/modules) is NEVER filtered out by moduleCode', () => {
    // Disable ALL configurable modules
    const allDisabled = createModuleRegistry({
      CLIENTS: false,
      TASKS: false,
      DOCUMENTS: false,
      DOCUMENT_REQUESTS: false,
      CLIENT_PORTAL: false,
      NOTIFICATIONS: false,
      AUDIT: false,
      GST: false,
      ITR: false,
      TDS: false,
      TAX_NOTICES: false,
      BILLING: false,
      REPORTS: false,
      MARKETPLACE: false,
    });

    const practiceSections = resolveSidebarSections(practiceNavSections, mockPracticeAdmin, allDisabled);
    const allLabels = practiceSections.flatMap((s) => s.items.map((i) => i.label));

    assert.ok(
      allLabels.includes('Modules & Features'),
      'Modules & Features configuration must ALWAYS remain accessible to Practice Admins'
    );
  });
});
