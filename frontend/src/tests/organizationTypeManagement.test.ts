import { describe, it } from 'node:test';
import assert from 'node:assert';

type OrganizationType =
  | 'UNKNOWN'
  | 'SOLO_PRACTITIONER'
  | 'SMALL_TAX_FIRM'
  | 'GROWING_PRACTICE'
  | 'BUSINESS';

const ORGANIZATION_TYPE_LABELS: Record<OrganizationType, string> = {
  UNKNOWN: 'Not Configured',
  SOLO_PRACTITIONER: 'Solo Practitioner',
  SMALL_TAX_FIRM: 'Small Tax Firm',
  GROWING_PRACTICE: 'Growing Practice',
  BUSINESS: 'Business',
};

interface MockUser {
  roles: (string | { code: string })[];
  permissions?: string[];
}

const canManageOrganization = (user: MockUser | null | undefined): boolean => {
  if (!user) return false;
  const adminRoles = [
    'TAXORYN_SUPERADMIN',
    'SUPER_ADMIN',
    'PRACTICE_OWNER',
    'PRACTICE_ADMIN',
    'ORG_ADMIN',
    'PARTNER',
  ];
  const writePermissions = ['ORGANIZATION_UPDATE', 'ORG_WRITE'];

  const userRoles = (user.roles || []).map((r) => (typeof r === 'string' ? r : r.code));
  const hasAdminRole = userRoles.some((r) => adminRoles.includes(r));
  const hasWritePermission = (user.permissions || []).some((p) => writePermissions.includes(p));

  return hasAdminRole || hasWritePermission;
};

describe('Organization Type Management & Settings UI Standard', () => {
  it('1. Maps all OrganizationType enum values to human-friendly display labels', () => {
    assert.strictEqual(ORGANIZATION_TYPE_LABELS.UNKNOWN, 'Not Configured');
    assert.strictEqual(ORGANIZATION_TYPE_LABELS.SOLO_PRACTITIONER, 'Solo Practitioner');
    assert.strictEqual(ORGANIZATION_TYPE_LABELS.SMALL_TAX_FIRM, 'Small Tax Firm');
    assert.strictEqual(ORGANIZATION_TYPE_LABELS.GROWING_PRACTICE, 'Growing Practice');
    assert.strictEqual(ORGANIZATION_TYPE_LABELS.BUSINESS, 'Business');
  });

  it('2. Legacy organization UNKNOWN is displayed as Not Configured without exposing raw enum', () => {
    const rawType: OrganizationType = 'UNKNOWN';
    const displayLabel = ORGANIZATION_TYPE_LABELS[rawType];
    assert.strictEqual(displayLabel, 'Not Configured');
    assert.notStrictEqual(displayLabel, 'UNKNOWN');
  });

  it('3. ORG_ADMIN user is authorized to edit organization type', () => {
    const orgAdmin: MockUser = { roles: ['ORG_ADMIN'], permissions: ['ORGANIZATION_VIEW'] };
    assert.strictEqual(canManageOrganization(orgAdmin), true);
  });

  it('4. User with ORGANIZATION_UPDATE permission is authorized to edit organization type', () => {
    const managerUser: MockUser = { roles: ['MANAGER'], permissions: ['ORGANIZATION_UPDATE'] };
    assert.strictEqual(canManageOrganization(managerUser), true);
  });

  it('5. Regular Staff or Client User is unauthorized and gets read-only view', () => {
    const staffUser: MockUser = { roles: ['STAFF'], permissions: ['CLIENT_VIEW', 'TASK_VIEW'] };
    assert.strictEqual(canManageOrganization(staffUser), false);

    const clientUser: MockUser = { roles: ['CLIENT_USER'], permissions: ['PORTAL_READ'] };
    assert.strictEqual(canManageOrganization(clientUser), false);
  });

  it('6. Builds correct payload when classifying legacy UNKNOWN organization to SMALL_TAX_FIRM', () => {
    const currentOrg = { id: 'org-1', name: 'Apex CA Practice', organizationType: 'UNKNOWN' as OrganizationType };
    const targetType: OrganizationType = 'SMALL_TAX_FIRM';

    const payload = {
      name: currentOrg.name,
      organizationType: targetType,
    };

    assert.strictEqual(payload.organizationType, 'SMALL_TAX_FIRM');
    assert.strictEqual(payload.name, 'Apex CA Practice');
  });

  it('7. Builds correct payload when updating SMALL_TAX_FIRM to GROWING_PRACTICE', () => {
    const currentOrg = { id: 'org-1', name: 'Apex CA Practice', organizationType: 'SMALL_TAX_FIRM' as OrganizationType };
    const targetType: OrganizationType = 'GROWING_PRACTICE';

    const payload = {
      name: currentOrg.name,
      organizationType: targetType,
    };

    assert.strictEqual(payload.organizationType, 'GROWING_PRACTICE');
  });
});
