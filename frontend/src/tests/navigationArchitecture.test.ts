import { describe, it } from 'node:test';
import assert from 'node:assert';
import {
  filterNavigationSections,
  filterRoleNavigationItems,
  filterNavigationByPermissions,
  canAccessNavigationItem,
  hasPermission,
  isGlobalSidebarItem,
  GLOBAL_SIDEBAR_PATHS,
  GLOBAL_SIDEBAR_LABELS,
  NOTIFICATION_PERMISSIONS,
  NOTIFICATION_ADMIN_ROLES,
} from '../utils/permissionUtils.ts';
import type { NavigationSection, NavigationItem } from '../utils/permissionUtils.ts';

describe('Taxoryn Sidebar Information Architecture & RBAC Visibility Standard', () => {
  const adminUser = {
    id: 'admin-1',
    email: 'admin@taxpractice.com',
    firstName: 'Admin',
    lastName: 'User',
    roles: ['PRACTICE_ADMIN', 'PRACTICE_OWNER'],
    permissions: [
      'CLIENT_VIEW', 'TASK_VIEW', 'GST_VIEW', 'ITR_VIEW', 'DOCUMENT_VIEW',
      'REPORT_VIEW', 'USER_VIEW', 'ROLE_READ', 'BILLING_VIEW', 'BILLING_READ',
      'AUDIT_VIEW', 'AUDIT_READ', 'ORGANIZATION_UPDATE', 'SUBSCRIPTION_VIEW',
      'COMMUNICATION_MANAGE', 'MARKETPLACE_MANAGE', 'ORGANIZATION_VIEW'
    ],
  };

  const staffUser = {
    id: 'staff-1',
    email: 'staff@taxpractice.com',
    firstName: 'Staff',
    lastName: 'Member',
    roles: ['PRACTICE_EMPLOYEE', 'STAFF'],
    permissions: [
      'CLIENT_VIEW', 'TASK_VIEW', 'GST_VIEW', 'ITR_VIEW', 'DOCUMENT_VIEW',
      'EMPLOYEE_VIEW', 'ORGANIZATION_VIEW'
    ],
  };

  const accountantUser = {
    id: 'accountant-1',
    email: 'accountant@taxpractice.com',
    firstName: 'Accountant',
    lastName: 'User',
    roles: ['ACCOUNTANT'],
    permissions: [
      'CLIENT_VIEW', 'TASK_VIEW', 'GST_VIEW', 'ITR_VIEW', 'DOCUMENT_VIEW',
      'BILLING_VIEW', 'BILLING_READ', 'BILLING_CREATE', 'ORGANIZATION_VIEW'
    ],
  };

  const managerUser = {
    id: 'manager-1',
    email: 'manager@taxpractice.com',
    firstName: 'Manager',
    lastName: 'User',
    roles: ['MANAGER'],
    permissions: [
      'CLIENT_VIEW', 'TASK_VIEW', 'GST_VIEW', 'ITR_VIEW', 'DOCUMENT_VIEW',
      'REPORT_VIEW', 'AUDIT_VIEW', 'AUDIT_READ', 'ORGANIZATION_VIEW'
    ],
  };

  const clientUser = {
    id: 'client-1',
    email: 'client@clientcorp.com',
    firstName: 'Client',
    lastName: 'Taxpayer',
    roles: ['CLIENT_USER', 'PRACTICE_CLIENT'],
    permissions: [],
  };

  const fullPracticeNavSections: NavigationSection[] = [
    {
      id: 'work',
      sectionTitle: 'WORK',
      items: [
        { label: 'Dashboard', path: '/dashboard' },
        { label: 'Clients 360°', path: '/clients', requiredPermissions: ['CLIENT_VIEW'] },
        { label: 'Tasks & Workflow', path: '/tasks', requiredPermissions: ['TASK_VIEW'] },
      ],
    },
    {
      id: 'compliance',
      sectionTitle: 'COMPLIANCE',
      items: [
        { label: 'GST Compliance', path: '/gst', requiredPermissions: ['GST_VIEW'] },
        { label: 'ITR Compliance', path: '/itr', requiredPermissions: ['ITR_VIEW'] },
        { label: 'TDS Compliance', path: '/tds', requiredPermissions: ['ITR_VIEW', 'GST_VIEW', 'TASK_VIEW'] },
        { label: 'Notice Center', path: '/notices', requiredPermissions: ['NOTICE_VIEW'] },
        { label: 'Tax Calendar', path: '/calendar', requiredPermissions: ['TASK_VIEW', 'GST_VIEW', 'ITR_VIEW'] },
      ],
    },
    {
      id: 'documents',
      sectionTitle: 'DOCUMENTS',
      items: [
        { label: 'Document Vault', path: '/documents', requiredPermissions: ['DOCUMENT_VIEW'] },
      ],
    },
    {
      id: 'practice',
      sectionTitle: 'PRACTICE',
      items: [
        { label: 'Client Portal Hub', path: '/portal', requiredPermissions: ['CLIENT_VIEW', 'CLIENT_UPDATE'], allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER'] },
        { label: 'Reports', path: '/reports', requiredPermissions: ['REPORT_VIEW', 'REPORTS_VIEW'], allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'MANAGER'] },
        { label: 'Inbound Leads (CRM)', path: '/marketplace/leads', requiredPermissions: ['MARKETPLACE_LEAD_VIEW', 'MARKETPLACE_LEAD_MANAGE'], allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER'] },
        { label: 'Client Onboarding', path: '/marketplace/onboarding', requiredPermissions: ['MARKETPLACE_ONBOARDING_MANAGE', 'CLIENT_CREATE'], allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER'] },
        { label: 'Notification Center', path: '/notifications', requiredPermissions: NOTIFICATION_PERMISSIONS, allowedRoles: NOTIFICATION_ADMIN_ROLES },
      ],
    },
    {
      id: 'administration',
      sectionTitle: 'ADMINISTRATION',
      isCollapsible: true,
      items: [
        { label: 'Team & RBAC', path: '/team', requiredPermissions: ['USER_VIEW', 'ROLE_READ'], allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER'] },
        { label: 'Billing & Invoices', path: '/billing', requiredPermissions: ['BILLING_VIEW', 'BILLING_READ'], allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'ACCOUNTANT'] },
        { label: 'Activity & Audit', path: '/audit-logs', requiredPermissions: ['AUDIT_VIEW', 'AUDIT_READ'], allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'MANAGER', 'TAX_PROFESSIONAL', 'PRACTITIONER', 'ACCOUNTANT'] },
        { label: 'Branding & Themes', path: '/settings/branding', requiredPermissions: ['ORGANIZATION_UPDATE', 'ORG_WRITE'], allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER'] },
        { label: 'Subscription', path: '/settings/subscription', requiredPermissions: ['SUBSCRIPTION_VIEW', 'ORGANIZATION_UPDATE', 'ORG_WRITE'], allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER'] },
        { label: 'WhatsApp Alerts', path: '/settings/whatsapp', requiredPermissions: ['COMMUNICATION_MANAGE'], allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER'] },
      ],
    },
    {
      id: 'growth',
      sectionTitle: 'GROWTH',
      items: [
        { label: 'Marketplace', path: '/settings/marketplace', requiredPermissions: ['MARKETPLACE_MANAGE', 'ORGANIZATION_UPDATE', 'ORG_WRITE'], allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER'] },
      ],
    },
  ];

  const clientNavSections: NavigationSection[] = [
    {
      id: 'my-tax',
      sectionTitle: 'MY TAX',
      items: [
        { label: 'Portal Dashboard', path: '/portal' },
        { label: 'GST Returns', path: '/portal?tab=gst' },
        { label: 'ITR Returns', path: '/portal?tab=itr' },
        { label: 'TDS Statements', path: '/portal?tab=tds' },
        { label: 'Invoices & Due Bills', path: '/portal?tab=invoices' },
        { label: 'Document Vault', path: '/portal?tab=documents' },
      ],
    },
    {
      id: 'explore',
      sectionTitle: 'EXPLORE',
      items: [
        { label: 'Find a Tax Professional', path: '/marketplace/explore' },
      ],
    },
  ];

  it('Standard: isGlobalSidebarItem correctly identifies global lower sidebar items', () => {
    assert.strictEqual(isGlobalSidebarItem({ label: 'Security & Password', path: '/settings/security' }), true);
    assert.strictEqual(isGlobalSidebarItem({ label: 'Give Feedback', path: '/feedback' }), true);
    assert.strictEqual(isGlobalSidebarItem({ label: 'Security', path: '/profile/security' }), true);
    assert.strictEqual(isGlobalSidebarItem({ label: 'Portal Dashboard', path: '/portal' }), false);
    assert.strictEqual(isGlobalSidebarItem({ label: 'Clients 360°', path: '/clients' }), false);
  });

  it('RBAC Navigation Visibility: Practice Staff does NOT see Billing & Invoices', () => {
    const visibleSections = filterNavigationSections(fullPracticeNavSections, staffUser as any);
    const allVisiblePaths = visibleSections.flatMap((s) => s.items.map((i) => i.path));
    const allVisibleLabels = visibleSections.flatMap((s) => s.items.map((i) => i.label));

    assert.strictEqual(allVisiblePaths.includes('/billing'), false, 'Practice Staff must NOT see /billing');
    assert.strictEqual(allVisibleLabels.includes('Billing & Invoices'), false, 'Practice Staff must NOT see Billing & Invoices');
  });

  it('RBAC Navigation Visibility: Practice Staff does NOT see Activity & Audit', () => {
    const visibleSections = filterNavigationSections(fullPracticeNavSections, staffUser as any);
    const allVisiblePaths = visibleSections.flatMap((s) => s.items.map((i) => i.path));
    const allVisibleLabels = visibleSections.flatMap((s) => s.items.map((i) => i.label));

    assert.strictEqual(allVisiblePaths.includes('/audit-logs'), false, 'Practice Staff must NOT see /audit-logs');
    assert.strictEqual(allVisibleLabels.includes('Activity & Audit'), false, 'Practice Staff must NOT see Activity & Audit');
  });

  it('RBAC Navigation Visibility: Authorized billing user (Accountant / Admin) sees Billing & Invoices', () => {
    const accountantSections = filterNavigationSections(fullPracticeNavSections, accountantUser as any);
    const accountantPaths = accountantSections.flatMap((s) => s.items.map((i) => i.path));
    assert.strictEqual(accountantPaths.includes('/billing'), true, 'Accountant user must see /billing');

    const adminSections = filterNavigationSections(fullPracticeNavSections, adminUser as any);
    const adminPaths = adminSections.flatMap((s) => s.items.map((i) => i.path));
    assert.strictEqual(adminPaths.includes('/billing'), true, 'Admin user must see /billing');
  });

  it('RBAC Navigation Visibility: Authorized audit user (Manager / Admin) sees Activity & Audit', () => {
    const managerSections = filterNavigationSections(fullPracticeNavSections, managerUser as any);
    const managerPaths = managerSections.flatMap((s) => s.items.map((i) => i.path));
    assert.strictEqual(managerPaths.includes('/audit-logs'), true, 'Manager user must see /audit-logs');

    const adminSections = filterNavigationSections(fullPracticeNavSections, adminUser as any);
    const adminPaths = adminSections.flatMap((s) => s.items.map((i) => i.path));
    assert.strictEqual(adminPaths.includes('/audit-logs'), true, 'Admin user must see /audit-logs');
  });

  it('Group Visibility: Administration group disappears when it has no authorized children', () => {
    const visibleSections = filterNavigationSections(fullPracticeNavSections, staffUser as any);
    const sectionIds = visibleSections.map((s) => s.id);

    // Practice Staff has no permissions for any item in Administration (Team, Billing, Audit, Branding, Subscription, WhatsApp)
    assert.strictEqual(sectionIds.includes('administration'), false, 'Administration section must be pruned when empty');
  });

  it('Group Visibility: Administration section remains when at least one child is authorized', () => {
    const billingOnlyUser = {
      id: 'billing-staff-1',
      email: 'billingstaff@taxpractice.com',
      firstName: 'Billing',
      lastName: 'Staff',
      roles: ['PRACTICE_EMPLOYEE'],
      permissions: ['CLIENT_VIEW', 'TASK_VIEW', 'BILLING_VIEW'],
    };

    const billingSections = filterNavigationSections(fullPracticeNavSections, billingOnlyUser as any);
    const adminSection = billingSections.find((s) => s.id === 'administration');

    assert.ok(adminSection, 'Administration section must exist for user with Billing permission');
    assert.strictEqual(adminSection.items.length, 1, 'Administration section should only contain authorized child items');
    assert.strictEqual(adminSection.items[0].path, '/billing');

    const auditOnlyUser = {
      id: 'audit-staff-1',
      email: 'auditstaff@taxpractice.com',
      firstName: 'Audit',
      lastName: 'Staff',
      roles: ['PRACTICE_EMPLOYEE'],
      permissions: ['CLIENT_VIEW', 'TASK_VIEW', 'AUDIT_VIEW'],
    };

    const auditSections = filterNavigationSections(fullPracticeNavSections, auditOnlyUser as any);
    const auditAdminSection = auditSections.find((s) => s.id === 'administration');
    assert.ok(auditAdminSection, 'Administration section must exist for user with Audit permission');
    assert.strictEqual(auditAdminSection.items.length, 1);
    assert.strictEqual(auditAdminSection.items[0].path, '/audit-logs');
  });

  it('Route Guard & Backend Defense-in-Depth: Direct unauthorized route navigation is rejected', () => {
    // Staff user attempting direct access to /billing route guard permissions
    const billingAuthorizedForStaff = hasPermission(
      staffUser as any,
      ['BILLING_VIEW', 'BILLING_READ'],
      ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'ACCOUNTANT']
    );
    assert.strictEqual(billingAuthorizedForStaff, false, 'Route Guard must block Staff from /billing');

    // Staff user attempting direct access to /audit-logs route guard permissions
    const auditAuthorizedForStaff = hasPermission(
      staffUser as any,
      ['AUDIT_VIEW', 'AUDIT_READ'],
      ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'MANAGER', 'TAX_PROFESSIONAL', 'PRACTITIONER', 'ACCOUNTANT']
    );
    assert.strictEqual(auditAuthorizedForStaff, false, 'Route Guard must block Staff from /audit-logs');

    // Staff user attempting direct access to /reports
    const reportsAuthorizedForStaff = hasPermission(
      staffUser as any,
      ['REPORT_VIEW', 'REPORTS_VIEW'],
      ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'MANAGER']
    );
    assert.strictEqual(reportsAuthorizedForStaff, false, 'Route Guard must block Staff from /reports');

    // Authorized Accountant accessing /billing
    const billingAuthorizedForAccountant = hasPermission(
      accountantUser as any,
      ['BILLING_VIEW', 'BILLING_READ'],
      ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'ACCOUNTANT']
    );
    assert.strictEqual(billingAuthorizedForAccountant, true, 'Route Guard must allow Accountant to /billing');
  });

  it('Global Items: Security & Password and Give Feedback remain global across all roles', () => {
    const practiceVisible = filterNavigationSections(fullPracticeNavSections, staffUser as any);
    for (const section of practiceVisible) {
      for (const item of section.items) {
        assert.notStrictEqual(item.path, '/settings/security');
        assert.notStrictEqual(item.path, '/feedback');
      }
    }

    const clientVisible = filterNavigationSections(clientNavSections, clientUser as any);
    for (const section of clientVisible) {
      for (const item of section.items) {
        assert.notStrictEqual(item.path, '/settings/security');
        assert.notStrictEqual(item.path, '/feedback');
      }
    }
  });

  it('Duplicate Protection: Injected Security and Feedback inside any section are filtered out', () => {
    const corruptedSections: NavigationSection[] = [
      {
        id: 'account',
        sectionTitle: 'ACCOUNT',
        items: [
          { label: 'Security & Password', path: '/settings/security' },
          { label: 'Give Feedback', path: '/feedback' },
        ],
      },
      {
        id: 'work',
        sectionTitle: 'WORK',
        items: [
          { label: 'Dashboard', path: '/dashboard' },
          { label: 'Security & Password', path: '/settings/security' },
        ],
      },
    ];

    const sanitizedSections = filterNavigationSections(corruptedSections, adminUser as any);

    // Corrupted ACCOUNT section had only global items, so it must be pruned completely
    assert.strictEqual(sanitizedSections.some((s) => s.id === 'account'), false);

    // WORK section should only contain Dashboard
    const workSection = sanitizedSections.find((s) => s.id === 'work');
    assert.ok(workSection);
    assert.strictEqual(workSection.items.length, 1);
    assert.strictEqual(workSection.items[0].path, '/dashboard');
  });

  it('Duplicate Protection: Flat role navigation items prune global sidebar items', () => {
    const flatItems: NavigationItem[] = [
      { label: 'Platform Overview', path: '/admin/overview' },
      { label: 'Security & Password', path: '/settings/security' },
      { label: 'Give Feedback', path: '/feedback' },
    ];

    const sanitizedItems = filterRoleNavigationItems(flatItems, adminUser as any);
    assert.strictEqual(sanitizedItems.length, 1);
    assert.strictEqual(sanitizedItems[0].path, '/admin/overview');
  });

  it('Canonical routes: Global paths strictly resolve to /settings/security and /feedback', () => {
    assert.ok(GLOBAL_SIDEBAR_PATHS.has('/settings/security'));
    assert.ok(GLOBAL_SIDEBAR_PATHS.has('/feedback'));
    assert.ok(GLOBAL_SIDEBAR_LABELS.has('Security & Password'));
    assert.ok(GLOBAL_SIDEBAR_LABELS.has('Give Feedback'));
  });

  it('Notice Center Navigation Visibility: User with NOTICE_VIEW sees Notice Center', () => {
    const userWithNoticeView = {
      id: 'notice-user-1',
      email: 'notice@taxpractice.com',
      firstName: 'Notice',
      lastName: 'User',
      roles: ['PRACTICE_EMPLOYEE'],
      permissions: ['NOTICE_VIEW'],
    };

    const sections = filterNavigationSections(fullPracticeNavSections, userWithNoticeView as any);
    const visiblePaths = sections.flatMap((s) => s.items.map((i) => i.path));
    const visibleLabels = sections.flatMap((s) => s.items.map((i) => i.label));

    assert.strictEqual(visiblePaths.includes('/notices'), true, 'User with NOTICE_VIEW must see /notices');
    assert.strictEqual(visibleLabels.includes('Notice Center'), true, 'User with NOTICE_VIEW must see Notice Center');
  });

  it('Notice Center Navigation Visibility: User without NOTICE_VIEW does NOT see Notice Center', () => {
    const userWithoutNoticeView = {
      id: 'no-notice-user-1',
      email: 'nonotice@taxpractice.com',
      firstName: 'NoNotice',
      lastName: 'User',
      roles: ['PRACTICE_EMPLOYEE'],
      permissions: ['CLIENT_VIEW', 'TASK_VIEW'], // No NOTICE_VIEW
    };

    const sections = filterNavigationSections(fullPracticeNavSections, userWithoutNoticeView as any);
    const visiblePaths = sections.flatMap((s) => s.items.map((i) => i.path));
    const visibleLabels = sections.flatMap((s) => s.items.map((i) => i.label));

    assert.strictEqual(visiblePaths.includes('/notices'), false, 'User without NOTICE_VIEW must NOT see /notices');
    assert.strictEqual(visibleLabels.includes('Notice Center'), false, 'User without NOTICE_VIEW must NOT see Notice Center');
  });

  it('Notice Center Navigation Visibility: Practitioner with NOTICE_VIEW sees Notice Center', () => {
    const practitionerUser = {
      id: 'practitioner-1',
      email: 'pooja@taxpractice.com',
      firstName: 'Pooja',
      lastName: 'Practitioner',
      roles: ['PRACTITIONER'],
      permissions: ['NOTICE_VIEW', 'CLIENT_VIEW'],
    };

    const sections = filterNavigationSections(fullPracticeNavSections, practitionerUser as any);
    const visiblePaths = sections.flatMap((s) => s.items.map((i) => i.path));
    assert.strictEqual(visiblePaths.includes('/notices'), true, 'Practitioner with NOTICE_VIEW must see /notices');
  });

  it('Notice Center Navigation Visibility: Staff with NOTICE_VIEW sees Notice Center', () => {
    const staffWithNotice = {
      id: 'staff-notice-1',
      email: 'staff@taxpractice.com',
      firstName: 'Staff',
      lastName: 'Notice',
      roles: ['STAFF', 'PRACTICE_EMPLOYEE'],
      permissions: ['NOTICE_VIEW', 'TASK_VIEW'],
    };

    const sections = filterNavigationSections(fullPracticeNavSections, staffWithNotice as any);
    const visiblePaths = sections.flatMap((s) => s.items.map((i) => i.path));
    assert.strictEqual(visiblePaths.includes('/notices'), true, 'Staff with NOTICE_VIEW must see /notices');
  });

  it('Notice Center Navigation Visibility: Tax Professional with NOTICE_VIEW sees Notice Center', () => {
    const taxProfUser = {
      id: 'tax-prof-1',
      email: 'taxprof@taxpractice.com',
      firstName: 'Tax',
      lastName: 'Professional',
      roles: ['TAX_PROFESSIONAL'],
      permissions: ['NOTICE_VIEW', 'GST_VIEW', 'ITR_VIEW'],
    };

    const sections = filterNavigationSections(fullPracticeNavSections, taxProfUser as any);
    const visiblePaths = sections.flatMap((s) => s.items.map((i) => i.path));
    assert.strictEqual(visiblePaths.includes('/notices'), true, 'Tax Professional with NOTICE_VIEW must see /notices');
  });

  it('Notice Center Navigation Visibility: Manager with NOTICE_VIEW sees Notice Center', () => {
    const managerWithNotice = {
      id: 'manager-notice-1',
      email: 'manager@taxpractice.com',
      firstName: 'Manager',
      lastName: 'Notice',
      roles: ['MANAGER'],
      permissions: ['NOTICE_VIEW', 'REPORT_VIEW'],
    };

    const sections = filterNavigationSections(fullPracticeNavSections, managerWithNotice as any);
    const visiblePaths = sections.flatMap((s) => s.items.map((i) => i.path));
    assert.strictEqual(visiblePaths.includes('/notices'), true, 'Manager with NOTICE_VIEW must see /notices');
  });

  it('Notice Center Navigation Visibility: Org Admin / Partner with NOTICE_VIEW sees Notice Center', () => {
    const orgAdminWithNotice = {
      id: 'orgadmin-notice-1',
      email: 'admin@taxpractice.com',
      firstName: 'Admin',
      lastName: 'Notice',
      roles: ['ORG_ADMIN', 'PARTNER'],
      permissions: ['NOTICE_VIEW', 'CLIENT_VIEW', 'USER_VIEW'],
    };

    const sections = filterNavigationSections(fullPracticeNavSections, orgAdminWithNotice as any);
    const visiblePaths = sections.flatMap((s) => s.items.map((i) => i.path));
    assert.strictEqual(visiblePaths.includes('/notices'), true, 'Org Admin / Partner with NOTICE_VIEW must see /notices');
  });

  it('Notice Center Navigation Visibility: Notice Center does NOT require TASK_VIEW or CLIENT_VIEW', () => {
    const noticeOnlyUser = {
      id: 'notice-only-1',
      email: 'noticeonly@taxpractice.com',
      firstName: 'Notice',
      lastName: 'Only',
      roles: ['PRACTICE_EMPLOYEE'],
      permissions: ['NOTICE_VIEW'], // ONLY NOTICE_VIEW, no TASK_VIEW or CLIENT_VIEW
    };

    const sections = filterNavigationSections(fullPracticeNavSections, noticeOnlyUser as any);
    const visiblePaths = sections.flatMap((s) => s.items.map((i) => i.path));
    assert.strictEqual(visiblePaths.includes('/notices'), true, 'Notice Center must be visible with only NOTICE_VIEW without requiring TASK_VIEW or CLIENT_VIEW');
  });
});
