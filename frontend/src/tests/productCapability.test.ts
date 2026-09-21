import { describe, it } from 'node:test';
import assert from 'node:assert';

export type OrganizationType =
  | 'UNKNOWN'
  | 'SOLO_PRACTITIONER'
  | 'SMALL_TAX_FIRM'
  | 'GROWING_PRACTICE'
  | 'BUSINESS';

export type ProductCapability =
  | 'CLIENT_MANAGEMENT'
  | 'GST_COMPLIANCE'
  | 'ITR_COMPLIANCE'
  | 'TDS_COMPLIANCE'
  | 'COMPLIANCE_CALENDAR'
  | 'TASK_MANAGEMENT'
  | 'DOCUMENT_MANAGEMENT'
  | 'DOCUMENT_REQUESTS'
  | 'TAX_NOTICE_MANAGEMENT'
  | 'BILLING_INVOICING'
  | 'CENTRAL_REPORTING'
  | 'TEAM_MANAGEMENT'
  | 'CLIENT_PORTAL'
  | 'ADVANCED_ANALYTICS';

export interface OrganizationCapabilities {
  organizationId: string;
  organizationType: OrganizationType;
  subscriptionPlan: 'STARTER' | 'PROFESSIONAL' | 'BUSINESS' | 'ENTERPRISE';
  enabledCapabilities: ProductCapability[];
  recommendedModules: string[];
  defaultDashboardView: string;
  onboardingProfile: string;
  multiUserPractice: boolean;
  clientPortalSupported: boolean;
  noticeCenterSupported: boolean;
  customInvoicingSupported: boolean;
}

export function hasCapability(
  capabilities: OrganizationCapabilities | null | undefined,
  capability: ProductCapability
): boolean {
  if (!capabilities || !capabilities.enabledCapabilities) {
    return false;
  }
  return capabilities.enabledCapabilities.includes(capability);
}

export function isModuleRecommended(
  capabilities: OrganizationCapabilities | null | undefined,
  moduleKey: string
): boolean {
  if (!capabilities || !capabilities.recommendedModules) {
    return true;
  }
  return capabilities.recommendedModules.includes(moduleKey.toUpperCase().trim());
}

export function getDefaultDashboardRoute(
  capabilities: OrganizationCapabilities | null | undefined
): string {
  if (!capabilities) {
    return '/dashboard';
  }

  switch (capabilities.defaultDashboardView) {
    case 'SOLO_WORKLIST':
      return '/tasks?tab=WORKLIST&scope=MY_WORK';
    case 'TEAM_PRACTICE':
      return '/dashboard';
    case 'GROWING_PRACTICE':
      return '/dashboard';
    case 'IN_HOUSE_COMPLIANCE':
      return '/compliance';
    case 'STANDARD_PRACTICE':
    default:
      return '/dashboard';
  }
}

export function resolveFallbackCapabilities(
  orgType: OrganizationType = 'UNKNOWN'
): OrganizationCapabilities {
  const isSolo = orgType === 'SOLO_PRACTITIONER';
  const isBusiness = orgType === 'BUSINESS';
  const isGrowing = orgType === 'GROWING_PRACTICE';

  let capabilities: ProductCapability[] = [
    'CLIENT_MANAGEMENT',
    'GST_COMPLIANCE',
    'ITR_COMPLIANCE',
    'TDS_COMPLIANCE',
    'COMPLIANCE_CALENDAR',
    'TASK_MANAGEMENT',
    'DOCUMENT_MANAGEMENT',
    'DOCUMENT_REQUESTS',
    'BILLING_INVOICING',
    'CLIENT_PORTAL',
  ];

  if (isBusiness) {
    capabilities = [
      'GST_COMPLIANCE',
      'ITR_COMPLIANCE',
      'TDS_COMPLIANCE',
      'COMPLIANCE_CALENDAR',
      'TASK_MANAGEMENT',
      'DOCUMENT_MANAGEMENT',
      'TEAM_MANAGEMENT',
    ];
  } else if (!isSolo) {
    capabilities.push('TEAM_MANAGEMENT', 'CENTRAL_REPORTING');
    if (isGrowing) {
      capabilities.push('TAX_NOTICE_MANAGEMENT', 'ADVANCED_ANALYTICS');
    }
  }

  let defaultDashboardView = 'STANDARD_PRACTICE';
  if (isSolo) defaultDashboardView = 'SOLO_WORKLIST';
  else if (isBusiness) defaultDashboardView = 'IN_HOUSE_COMPLIANCE';
  else if (isGrowing) defaultDashboardView = 'GROWING_PRACTICE';
  else if (orgType === 'SMALL_TAX_FIRM') defaultDashboardView = 'TEAM_PRACTICE';

  return {
    organizationId: '',
    organizationType: orgType,
    subscriptionPlan: 'STARTER',
    enabledCapabilities: capabilities,
    recommendedModules: isBusiness
      ? ['GST', 'ITR', 'TDS', 'COMPLIANCE_CALENDAR', 'DOCUMENTS', 'TASKS', 'TEAM']
      : isSolo
      ? ['CLIENTS', 'GST', 'ITR', 'TDS', 'TASKS', 'BILLING']
      : isGrowing
      ? ['CLIENTS', 'GST', 'ITR', 'TDS', 'NOTICES', 'TASKS', 'TEAM', 'BILLING', 'PORTAL', 'REPORTS']
      : ['CLIENTS', 'GST', 'ITR', 'TDS', 'TASKS', 'TEAM', 'BILLING', 'PORTAL', 'REPORTS'],
    defaultDashboardView,
    onboardingProfile: `${orgType}_PROFILE`,
    multiUserPractice: !isSolo,
    clientPortalSupported: !isBusiness,
    noticeCenterSupported: isGrowing || orgType === 'UNKNOWN',
    customInvoicingSupported: !isBusiness,
  };
}

describe('Product Capability Framework & Experience Defaults', () => {
  it('1. UNKNOWN organization resolves standard baseline with all core capabilities', () => {
    const caps = resolveFallbackCapabilities('UNKNOWN');
    assert.strictEqual(caps.organizationType, 'UNKNOWN');
    assert.strictEqual(caps.defaultDashboardView, 'STANDARD_PRACTICE');
    assert.strictEqual(hasCapability(caps, 'CLIENT_MANAGEMENT'), true);
    assert.strictEqual(hasCapability(caps, 'GST_COMPLIANCE'), true);
    assert.strictEqual(hasCapability(caps, 'ITR_COMPLIANCE'), true);
    assert.strictEqual(hasCapability(caps, 'TDS_COMPLIANCE'), true);
    assert.strictEqual(hasCapability(caps, 'TASK_MANAGEMENT'), true);
    assert.strictEqual(hasCapability(caps, 'BILLING_INVOICING'), true);
    assert.strictEqual(hasCapability(caps, 'CLIENT_PORTAL'), true);
    assert.strictEqual(getDefaultDashboardRoute(caps), '/dashboard');
  });

  it('2. SOLO_PRACTITIONER focuses on solo worklist without team overhead', () => {
    const caps = resolveFallbackCapabilities('SOLO_PRACTITIONER');
    assert.strictEqual(caps.organizationType, 'SOLO_PRACTITIONER');
    assert.strictEqual(caps.multiUserPractice, false);
    assert.strictEqual(caps.defaultDashboardView, 'SOLO_WORKLIST');
    assert.strictEqual(getDefaultDashboardRoute(caps), '/tasks?tab=WORKLIST&scope=MY_WORK');
    assert.strictEqual(hasCapability(caps, 'CLIENT_MANAGEMENT'), true);
    assert.strictEqual(hasCapability(caps, 'GST_COMPLIANCE'), true);
    assert.strictEqual(hasCapability(caps, 'TEAM_MANAGEMENT'), false);
    assert.strictEqual(isModuleRecommended(caps, 'TEAM'), false);
    assert.strictEqual(isModuleRecommended(caps, 'GST'), true);
  });

  it('3. SMALL_TAX_FIRM enables team management, client portal, and team practice dashboard', () => {
    const caps = resolveFallbackCapabilities('SMALL_TAX_FIRM');
    assert.strictEqual(caps.organizationType, 'SMALL_TAX_FIRM');
    assert.strictEqual(caps.multiUserPractice, true);
    assert.strictEqual(caps.clientPortalSupported, true);
    assert.strictEqual(caps.defaultDashboardView, 'TEAM_PRACTICE');
    assert.strictEqual(hasCapability(caps, 'TEAM_MANAGEMENT'), true);
    assert.strictEqual(hasCapability(caps, 'CENTRAL_REPORTING'), true);
    assert.strictEqual(isModuleRecommended(caps, 'TEAM'), true);
    assert.strictEqual(isModuleRecommended(caps, 'PORTAL'), true);
  });

  it('4. GROWING_PRACTICE enables notice management and advanced analytics', () => {
    const caps = resolveFallbackCapabilities('GROWING_PRACTICE');
    assert.strictEqual(caps.organizationType, 'GROWING_PRACTICE');
    assert.strictEqual(caps.noticeCenterSupported, true);
    assert.strictEqual(hasCapability(caps, 'TAX_NOTICE_MANAGEMENT'), true);
    assert.strictEqual(hasCapability(caps, 'ADVANCED_ANALYTICS'), true);
    assert.strictEqual(isModuleRecommended(caps, 'NOTICES'), true);
  });

  it('5. BUSINESS focuses on corporate in-house compliance and calendar without client billing', () => {
    const caps = resolveFallbackCapabilities('BUSINESS');
    assert.strictEqual(caps.organizationType, 'BUSINESS');
    assert.strictEqual(caps.customInvoicingSupported, false);
    assert.strictEqual(caps.defaultDashboardView, 'IN_HOUSE_COMPLIANCE');
    assert.strictEqual(getDefaultDashboardRoute(caps), '/compliance');
    assert.strictEqual(hasCapability(caps, 'GST_COMPLIANCE'), true);
    assert.strictEqual(hasCapability(caps, 'TDS_COMPLIANCE'), true);
    assert.strictEqual(hasCapability(caps, 'COMPLIANCE_CALENDAR'), true);
    assert.strictEqual(hasCapability(caps, 'CLIENT_MANAGEMENT'), false);
    assert.strictEqual(hasCapability(caps, 'BILLING_INVOICING'), false);
    assert.strictEqual(isModuleRecommended(caps, 'CLIENTS'), false);
    assert.strictEqual(isModuleRecommended(caps, 'GST'), true);
  });

  it('6. hasCapability safely handles null/undefined configuration', () => {
    assert.strictEqual(hasCapability(null, 'GST_COMPLIANCE'), false);
    assert.strictEqual(hasCapability(undefined, 'GST_COMPLIANCE'), false);
    assert.strictEqual(isModuleRecommended(null, 'GST'), true);
    assert.strictEqual(getDefaultDashboardRoute(null), '/dashboard');
  });
});
