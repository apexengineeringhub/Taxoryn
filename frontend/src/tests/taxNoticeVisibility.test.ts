import { describe, it } from 'node:test';
import assert from 'node:assert';
import type { User, ProductCapability } from '../types/index.ts';
import {
  hasCapability,
  isModuleRecommended,
  resolveFallbackCapabilities,
} from '../utils/capabilityUtils.ts';
import {
  hasPermission,
  canAccessNavigationItem,
  type NavigationItem,
} from '../utils/permissionUtils.ts';

describe('Phase 8.1: Tax Notice Management Visibility & Navigation Tests', () => {
  describe('1. Capability Resolution across Personas', () => {
    it('should include TAX_NOTICE_MANAGEMENT across practice models', () => {
      const solo = resolveFallbackCapabilities('SOLO_PRACTITIONER');
      const small = resolveFallbackCapabilities('SMALL_TAX_FIRM');
      const growing = resolveFallbackCapabilities('GROWING_PRACTICE');
      const business = resolveFallbackCapabilities('BUSINESS');
      const unknown = resolveFallbackCapabilities('UNKNOWN');

      assert.strictEqual(hasCapability(solo, 'TAX_NOTICE_MANAGEMENT'), true);
      assert.strictEqual(hasCapability(small, 'TAX_NOTICE_MANAGEMENT'), true);
      assert.strictEqual(hasCapability(growing, 'TAX_NOTICE_MANAGEMENT'), true);
      assert.strictEqual(hasCapability(business, 'TAX_NOTICE_MANAGEMENT'), true);
      assert.strictEqual(hasCapability(unknown, 'TAX_NOTICE_MANAGEMENT'), true);
    });

    it('should recommend NOTICES module across practice personas', () => {
      const solo = resolveFallbackCapabilities('SOLO_PRACTITIONER');
      const small = resolveFallbackCapabilities('SMALL_TAX_FIRM');
      const growing = resolveFallbackCapabilities('GROWING_PRACTICE');

      assert.strictEqual(isModuleRecommended(solo, 'NOTICES'), true);
      assert.strictEqual(isModuleRecommended(small, 'NOTICES'), true);
      assert.strictEqual(isModuleRecommended(growing, 'NOTICES'), true);
    });
  });

  describe('2. Navigation Item Access & Role Evaluation', () => {
    const noticeNavItem: NavigationItem = {
      label: 'Notice Center',
      path: '/tax-notices',
      requiredPermissions: ['NOTICE_VIEW', 'TAX_NOTICE_VIEW'],
      allowedRoles: [
        'PRACTICE_OWNER',
        'PRACTICE_ADMIN',
        'ORG_ADMIN',
        'PARTNER',
        'MANAGER',
        'TAX_PROFESSIONAL',
        'PRACTITIONER',
        'STAFF',
        'ARTICLE_ASSISTANT',
        'ACCOUNTANT',
      ],
    };

    it('should grant access to SuperAdmin without explicit permissions', () => {
      const superUser: User = {
        id: 'u1',
        email: 'super@taxoryn.com',
        firstName: 'Super',
        lastName: 'Admin',
        roles: ['TAXORYN_SUPERADMIN'],
        permissions: [],
        organizationId: 'org1',
        status: 'ACTIVE',
      };
      assert.strictEqual(canAccessNavigationItem(noticeNavItem, superUser), true);
    });

    it('should grant access to Practice Admin via role match even if token permissions array is empty', () => {
      const adminUser: User = {
        id: 'u2',
        email: 'admin@firm.com',
        firstName: 'Practice',
        lastName: 'Admin',
        roles: ['PRACTICE_ADMIN'],
        permissions: [],
        organizationId: 'org1',
        status: 'ACTIVE',
      };
      assert.strictEqual(canAccessNavigationItem(noticeNavItem, adminUser), true);
    });

    it('should grant access to Staff / Article Assistant via role match', () => {
      const staffUser: User = {
        id: 'u3',
        email: 'staff@firm.com',
        firstName: 'Staff',
        lastName: 'Accountant',
        roles: ['STAFF'],
        permissions: [],
        organizationId: 'org1',
        status: 'ACTIVE',
      };
      assert.strictEqual(canAccessNavigationItem(noticeNavItem, staffUser), true);
    });

    it('should grant access to any user with explicit NOTICE_VIEW permission', () => {
      const customUser: User = {
        id: 'u4',
        email: 'custom@firm.com',
        firstName: 'Custom',
        lastName: 'User',
        roles: ['CUSTOM_ROLE'],
        permissions: ['NOTICE_VIEW'],
        organizationId: 'org1',
        status: 'ACTIVE',
      };
      assert.strictEqual(canAccessNavigationItem(noticeNavItem, customUser), true);
    });

    it('should deny access to Client Portal user without practice role or notice permission', () => {
      const clientUser: User = {
        id: 'u5',
        email: 'client@portal.com',
        firstName: 'Taxpayer',
        lastName: 'Client',
        roles: ['CLIENT_USER'],
        permissions: ['PORTAL_VIEW'],
        organizationId: 'org1',
        status: 'ACTIVE',
      };
      assert.strictEqual(canAccessNavigationItem(noticeNavItem, clientUser), false);
    });
  });
});
