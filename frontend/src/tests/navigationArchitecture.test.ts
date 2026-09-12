import { describe, it } from 'node:test';
import assert from 'node:assert';
import {
  filterNavigationSections,
  filterRoleNavigationItems,
  isGlobalSidebarItem,
  GLOBAL_SIDEBAR_PATHS,
  GLOBAL_SIDEBAR_LABELS,
} from '../utils/permissionUtils.ts';
import type { NavigationSection, NavigationItem } from '../utils/permissionUtils.ts';


describe('Taxoryn Sidebar Information Architecture Standard', () => {
  const adminUser = {
    id: 'admin-1',
    email: 'admin@taxpractice.com',
    firstName: 'Admin',
    lastName: 'User',
    roles: ['PRACTICE_ADMIN', 'PRACTICE_OWNER'],
    permissions: ['CLIENT_VIEW', 'TASK_VIEW', 'GST_VIEW', 'ITR_VIEW', 'DOCUMENT_VIEW', 'REPORT_VIEW', 'USER_VIEW', 'BILLING_VIEW', 'AUDIT_VIEW', 'ORGANIZATION_UPDATE', 'SUBSCRIPTION_VIEW', 'COMMUNICATION_MANAGE', 'MARKETPLACE_MANAGE'],
  };

  const staffUser = {
    id: 'staff-1',
    email: 'staff@taxpractice.com',
    firstName: 'Staff',
    lastName: 'Member',
    roles: ['PRACTICE_EMPLOYEE', 'STAFF'],
    permissions: ['CLIENT_VIEW', 'TASK_VIEW', 'GST_VIEW', 'ITR_VIEW', 'DOCUMENT_VIEW'],
  };

  const clientUser = {
    id: 'client-1',
    email: 'client@clientcorp.com',
    firstName: 'Client',
    lastName: 'Taxpayer',
    roles: ['CLIENT_USER', 'PRACTICE_CLIENT'],
    permissions: [],
  };

  const practiceNavSections: NavigationSection[] = [
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
      ],
    },
    {
      id: 'documents',
      sectionTitle: 'DOCUMENTS',
      items: [
        { label: 'Document Vault', path: '/documents', requiredPermissions: ['DOCUMENT_VIEW'] },
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

  it('Practitioner/Admin Portal: Security & Password and Give Feedback are outside sections', () => {
    const visibleSections = filterNavigationSections(practiceNavSections, adminUser as any);

    for (const section of visibleSections) {
      assert.notStrictEqual(section.sectionTitle, 'ACCOUNT');
      for (const item of section.items) {
        assert.notStrictEqual(item.path, '/settings/security', 'Security & Password must not be inside any section');
        assert.notStrictEqual(item.path, '/feedback', 'Give Feedback must not be inside any section');
        assert.notStrictEqual(item.label, 'Security & Password');
        assert.notStrictEqual(item.label, 'Give Feedback');
      }
    }
  });

  it('Employee/Staff Portal: Security & Password and Give Feedback are outside sections', () => {
    const visibleSections = filterNavigationSections(practiceNavSections, staffUser as any);

    for (const section of visibleSections) {
      assert.notStrictEqual(section.sectionTitle, 'ACCOUNT');
      for (const item of section.items) {
        assert.notStrictEqual(item.path, '/settings/security');
        assert.notStrictEqual(item.path, '/feedback');
        assert.notStrictEqual(item.label, 'Security & Password');
        assert.notStrictEqual(item.label, 'Give Feedback');
      }
    }
  });

  it('Client Portal: Contains MY TAX and EXPLORE without duplicate ACCOUNT section', () => {
    const visibleSections = filterNavigationSections(clientNavSections, clientUser as any);

    const sectionTitles = visibleSections.map((s) => s.sectionTitle);
    assert.deepStrictEqual(sectionTitles, ['MY TAX', 'EXPLORE']);

    // Assert no section contains Security & Password or Give Feedback
    for (const section of visibleSections) {
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
});
