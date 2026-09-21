import { describe, it } from 'node:test';
import assert from 'node:assert';
import type {
  OrganizationCapabilities,
  OrganizationType,
  ProductCapability,
} from '../types/index.ts';
import {
  getDashboardProfile,
  getDefaultDashboardRoute,
  getModuleRecommendationStatus,
  getOnboardingSteps,
  hasCapability,
  isModuleRecommended,
  resolveFallbackCapabilities,
} from '../utils/capabilityUtils.ts';

describe('Phase 7.2: OrganizationType Persona Workflows & Capabilities Tests', () => {
  describe('1. SOLO_PRACTITIONER Persona', () => {
    const soloCaps = resolveFallbackCapabilities('SOLO_PRACTITIONER');

    it('should configure single-user practice and solo worklist dashboard', () => {
      assert.strictEqual(soloCaps.organizationType, 'SOLO_PRACTITIONER');
      assert.strictEqual(soloCaps.multiUserPractice, false);
      assert.strictEqual(soloCaps.defaultDashboardView, 'SOLO_WORKLIST');
      assert.strictEqual(getDefaultDashboardRoute(soloCaps), '/tasks?tab=WORKLIST&scope=MY_WORK');
    });

    it('should enable core tax practice capabilities and mark TEAM_MANAGEMENT as NOT_RECOMMENDED', () => {
      assert.strictEqual(hasCapability(soloCaps, 'CLIENT_MANAGEMENT'), true);
      assert.strictEqual(hasCapability(soloCaps, 'GST_COMPLIANCE'), true);
      assert.strictEqual(hasCapability(soloCaps, 'BILLING_INVOICING'), true);
      assert.strictEqual(hasCapability(soloCaps, 'TEAM_MANAGEMENT'), false);

      assert.strictEqual(getModuleRecommendationStatus(soloCaps, 'CLIENT_MANAGEMENT'), 'ACTIVE');
      assert.strictEqual(getModuleRecommendationStatus(soloCaps, 'BILLING_INVOICING'), 'ACTIVE');
      assert.strictEqual(getModuleRecommendationStatus(soloCaps, 'TEAM_MANAGEMENT'), 'NOT_RECOMMENDED');
      assert.strictEqual(getModuleRecommendationStatus(soloCaps, 'ADVANCED_ANALYTICS'), 'UPGRADE_REQUIRED');
    });

    it('should provide structured onboarding checklist', () => {
      const steps = getOnboardingSteps(soloCaps);
      assert.strictEqual(steps.length >= 3, true);
      assert.strictEqual(steps[0].stepKey, 'PRACTICE_PROFILE');
      assert.strictEqual(steps[0].mandatory, true);
      assert.strictEqual(steps[1].stepKey, 'FIRST_CLIENT');
      assert.strictEqual(steps[1].targetRoute, '/clients/new');
    });

    it('should provide solo dashboard profile with primary metrics and widgets', () => {
      const profile = getDashboardProfile(soloCaps);
      assert.notStrictEqual(profile, null);
      assert.strictEqual(profile?.profileKey, 'SOLO_WORKLIST');
      assert.strictEqual(profile?.primaryMetrics.includes('MY_PENDING_TASKS'), true);
      assert.strictEqual(profile?.quickActions.includes('ADD_CLIENT'), true);
      assert.strictEqual(profile?.recommendedWidgets.includes('MY_WORKLIST'), true);
    });
  });

  describe('2. SMALL_TAX_FIRM Persona', () => {
    const smallCaps = resolveFallbackCapabilities('SMALL_TAX_FIRM');

    it('should configure multi-user practice and team practice dashboard', () => {
      assert.strictEqual(smallCaps.organizationType, 'SMALL_TAX_FIRM');
      assert.strictEqual(smallCaps.multiUserPractice, true);
      assert.strictEqual(smallCaps.defaultDashboardView, 'TEAM_PRACTICE');
      assert.strictEqual(getDefaultDashboardRoute(smallCaps), '/dashboard');
    });

    it('should enable team management and client portal', () => {
      assert.strictEqual(hasCapability(smallCaps, 'TEAM_MANAGEMENT'), true);
      assert.strictEqual(hasCapability(smallCaps, 'CLIENT_PORTAL'), true);
      assert.strictEqual(hasCapability(smallCaps, 'CENTRAL_REPORTING'), true);

      assert.strictEqual(getModuleRecommendationStatus(smallCaps, 'TEAM_MANAGEMENT'), 'ACTIVE');
      assert.strictEqual(getModuleRecommendationStatus(smallCaps, 'CLIENT_PORTAL'), 'ACTIVE');
      assert.strictEqual(getModuleRecommendationStatus(smallCaps, 'TAX_NOTICE_MANAGEMENT'), 'RECOMMENDED');
    });

    it('should provide team onboarding checklist', () => {
      const steps = getOnboardingSteps(smallCaps);
      assert.strictEqual(steps.length >= 2, true);
      assert.strictEqual(steps[0].stepKey, 'FIRM_PROFILE');
      assert.strictEqual(steps[1].stepKey, 'INVITE_STAFF');
    });

    it('should provide team practice dashboard profile', () => {
      const profile = getDashboardProfile(smallCaps);
      assert.notStrictEqual(profile, null);
      assert.strictEqual(profile?.profileKey, 'TEAM_PRACTICE');
      assert.strictEqual(profile?.primaryMetrics.includes('TOTAL_CLIENTS'), true);
      assert.strictEqual(profile?.recommendedWidgets.includes('STAFF_WORKLOAD_DISTRIBUTION'), true);
    });
  });

  describe('3. GROWING_PRACTICE Persona', () => {
    const growingCaps = resolveFallbackCapabilities('GROWING_PRACTICE');

    it('should configure full multi-department capabilities including tax notices and analytics', () => {
      assert.strictEqual(growingCaps.organizationType, 'GROWING_PRACTICE');
      assert.strictEqual(growingCaps.multiUserPractice, true);
      assert.strictEqual(growingCaps.defaultDashboardView, 'GROWING_PRACTICE');
      assert.strictEqual(growingCaps.noticeCenterSupported, true);

      assert.strictEqual(hasCapability(growingCaps, 'TAX_NOTICE_MANAGEMENT'), true);
      assert.strictEqual(hasCapability(growingCaps, 'ADVANCED_ANALYTICS'), true);
      assert.strictEqual(getModuleRecommendationStatus(growingCaps, 'TAX_NOTICE_MANAGEMENT'), 'ACTIVE');
      assert.strictEqual(getModuleRecommendationStatus(growingCaps, 'ADVANCED_ANALYTICS'), 'ACTIVE');
    });

    it('should provide growing practice dashboard profile with dispute tracker', () => {
      const profile = getDashboardProfile(growingCaps);
      assert.notStrictEqual(profile, null);
      assert.strictEqual(profile?.profileKey, 'GROWING_PRACTICE');
      assert.strictEqual(profile?.recommendedWidgets.includes('NOTICE_DISPUTE_TRACKER'), true);
      assert.strictEqual(profile?.recommendedWidgets.includes('REALIZATION_ANALYTICS'), true);
    });
  });

  describe('4. BUSINESS Persona', () => {
    const businessCaps = resolveFallbackCapabilities('BUSINESS');

    it('should configure in-house compliance dashboard and direct compliance route', () => {
      assert.strictEqual(businessCaps.organizationType, 'BUSINESS');
      assert.strictEqual(businessCaps.defaultDashboardView, 'IN_HOUSE_COMPLIANCE');
      assert.strictEqual(getDefaultDashboardRoute(businessCaps), '/compliance');
      assert.strictEqual(businessCaps.clientPortalSupported, false);
      assert.strictEqual(businessCaps.customInvoicingSupported, false);
    });

    it('should mark external practice features as NOT_RECOMMENDED', () => {
      assert.strictEqual(hasCapability(businessCaps, 'GST_COMPLIANCE'), true);
      assert.strictEqual(hasCapability(businessCaps, 'TDS_COMPLIANCE'), true);
      assert.strictEqual(hasCapability(businessCaps, 'COMPLIANCE_CALENDAR'), true);

      assert.strictEqual(getModuleRecommendationStatus(businessCaps, 'GST_COMPLIANCE'), 'ACTIVE');
      assert.strictEqual(getModuleRecommendationStatus(businessCaps, 'CLIENT_MANAGEMENT'), 'NOT_RECOMMENDED');
      assert.strictEqual(getModuleRecommendationStatus(businessCaps, 'CLIENT_PORTAL'), 'NOT_RECOMMENDED');
      assert.strictEqual(getModuleRecommendationStatus(businessCaps, 'BILLING_INVOICING'), 'NOT_RECOMMENDED');
    });

    it('should provide corporate in-house tax onboarding checklist', () => {
      const steps = getOnboardingSteps(businessCaps);
      assert.strictEqual(steps.length >= 3, true);
      assert.strictEqual(steps[0].stepKey, 'CORPORATE_PROFILE');
      assert.strictEqual(steps[1].stepKey, 'INVITE_TAX_TEAM');
      assert.strictEqual(steps[2].stepKey, 'CORPORATE_COMPLIANCE');
    });

    it('should provide in-house compliance dashboard profile', () => {
      const profile = getDashboardProfile(businessCaps);
      assert.notStrictEqual(profile, null);
      assert.strictEqual(profile?.profileKey, 'IN_HOUSE_COMPLIANCE');
      assert.strictEqual(profile?.primaryMetrics.includes('UPCOMING_STATUTORY_DEADLINES'), true);
      assert.strictEqual(profile?.recommendedWidgets.includes('CORPORATE_COMPLIANCE_CALENDAR'), true);
    });
  });

  describe('5. UNKNOWN Persona (Safe Baseline Fallback)', () => {
    const unknownCaps = resolveFallbackCapabilities('UNKNOWN');

    it('should provide safe default practice configuration', () => {
      assert.strictEqual(unknownCaps.organizationType, 'UNKNOWN');
      assert.strictEqual(unknownCaps.defaultDashboardView, 'STANDARD_PRACTICE');
      assert.strictEqual(getDefaultDashboardRoute(unknownCaps), '/dashboard');
      assert.strictEqual(hasCapability(unknownCaps, 'CLIENT_MANAGEMENT'), true);
      assert.strictEqual(hasCapability(unknownCaps, 'GST_COMPLIANCE'), true);
    });

    it('should fallback gracefully when null or undefined capabilities are passed', () => {
      assert.strictEqual(hasCapability(null, 'CLIENT_MANAGEMENT'), false);
      assert.strictEqual(isModuleRecommended(null, 'CLIENTS'), true);
      assert.strictEqual(getModuleRecommendationStatus(null, 'CLIENT_MANAGEMENT'), 'RECOMMENDED');
      assert.deepStrictEqual(getOnboardingSteps(null), []);
      assert.strictEqual(getDashboardProfile(null), null);
      assert.strictEqual(getDefaultDashboardRoute(null), '/dashboard');
    });
  });
});
