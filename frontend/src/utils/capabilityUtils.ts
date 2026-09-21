import type {
  DashboardProfile,
  ModuleRecommendationStatus,
  OnboardingStep,
  OrganizationCapabilities,
  OrganizationType,
  ProductCapability,
} from '../types/index.ts';

/**
 * Checks if a specific product capability is enabled in the organization configuration.
 *
 * NOTE: Capability checks are used for UI experience, dashboard tailoring,
 * and feature discovery. They are NOT a security boundary.
 */
export function hasCapability(
  capabilities: OrganizationCapabilities | null | undefined,
  capability: ProductCapability
): boolean {
  if (!capabilities || !capabilities.enabledCapabilities) {
    return false;
  }
  return capabilities.enabledCapabilities.includes(capability);
}

/**
 * Checks if a specific module is recommended for the organization profile.
 */
export function isModuleRecommended(
  capabilities: OrganizationCapabilities | null | undefined,
  moduleKey: string
): boolean {
  if (!capabilities || !capabilities.recommendedModules) {
    return true; // Default to showing if unconfigured
  }
  return capabilities.recommendedModules.includes(moduleKey.toUpperCase().trim());
}

/**
 * Gets the recommendation status of a specific capability.
 */
export function getModuleRecommendationStatus(
  capabilities: OrganizationCapabilities | null | undefined,
  capability: ProductCapability
): ModuleRecommendationStatus {
  if (!capabilities) {
    return 'RECOMMENDED';
  }
  if (capabilities.moduleStatuses && capabilities.moduleStatuses[capability]) {
    return capabilities.moduleStatuses[capability];
  }
  if (capabilities.enabledCapabilities && capabilities.enabledCapabilities.includes(capability)) {
    return 'ACTIVE';
  }
  return 'RECOMMENDED';
}

/**
 * Gets the onboarding checklist steps for the organization profile.
 */
export function getOnboardingSteps(
  capabilities: OrganizationCapabilities | null | undefined
): OnboardingStep[] {
  if (!capabilities || !capabilities.onboardingChecklist) {
    return [];
  }
  return [...capabilities.onboardingChecklist].sort((a, b) => a.sortOrder - b.sortOrder);
}

/**
 * Gets the dashboard profile configuration for the organization.
 */
export function getDashboardProfile(
  capabilities: OrganizationCapabilities | null | undefined
): DashboardProfile | null {
  if (!capabilities || !capabilities.dashboardProfile) {
    return null;
  }
  return capabilities.dashboardProfile;
}

/**
 * Resolves the default dashboard route based on the organization's experience profile.
 */
export function getDefaultDashboardRoute(
  capabilities: OrganizationCapabilities | null | undefined
): string {
  if (!capabilities) {
    return '/dashboard';
  }

  if (capabilities.dashboardProfile && capabilities.dashboardProfile.defaultRoute) {
    return capabilities.dashboardProfile.defaultRoute;
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

/**
 * Safe fallback capabilities resolver when offline or during initial hydration.
 */
export function resolveFallbackCapabilities(
  orgType: OrganizationType = 'UNKNOWN'
): OrganizationCapabilities {
  const isSolo = orgType === 'SOLO_PRACTITIONER';
  const isBusiness = orgType === 'BUSINESS';
  const isGrowing = orgType === 'GROWING_PRACTICE';
  const isSmall = orgType === 'SMALL_TAX_FIRM';

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
  let defaultRoute = '/dashboard';
  let title = 'Standard Practice Dashboard';
  let description = 'Standard general tax practice command center.';
  let primaryMetrics = ['TOTAL_CLIENTS', 'ACTIVE_TASKS', 'UPCOMING_DEADLINES', 'COMPLIANCE_HEALTH'];
  let quickActions = ['ADD_CLIENT', 'NEW_TASK', 'FILE_RETURN', 'CREATE_INVOICE'];
  let recommendedWidgets = ['PRACTICE_SUMMARY', 'UPCOMING_COMPLIANCE', 'TASK_WORKLIST', 'DOCUMENT_VAULT'];

  if (isSolo) {
    defaultDashboardView = 'SOLO_WORKLIST';
    defaultRoute = '/tasks?tab=WORKLIST&scope=MY_WORK';
    title = 'Solo Practitioner Worklist';
    description = 'Single-user task, client compliance, and invoicing command center.';
    primaryMetrics = ['ACTIVE_CLIENTS', 'MY_PENDING_TASKS', 'UPCOMING_DEADLINES', 'UNPAID_INVOICES'];
    quickActions = ['ADD_CLIENT', 'NEW_TASK', 'NEW_INVOICE', 'FILE_RETURN'];
    recommendedWidgets = ['MY_WORKLIST', 'UPCOMING_COMPLIANCE_CALENDAR', 'RECENT_CLIENT_VAULT', 'INVOICING_SUMMARY'];
  } else if (isSmall) {
    defaultDashboardView = 'TEAM_PRACTICE';
    defaultRoute = '/dashboard';
    title = 'Small Firm Practice Dashboard';
    description = 'Multi-staff workload distribution, client portfolio tracking, and firm compliance overview.';
    primaryMetrics = ['TOTAL_CLIENTS', 'TEAM_PENDING_TASKS', 'OVERDUE_COMPLIANCES', 'COLLECTION_REALIZATION'];
    quickActions = ['INVITE_STAFF', 'BULK_ASSIGN_CLIENTS', 'CREATE_TASK', 'GENERATE_REPORT'];
    recommendedWidgets = ['FIRM_OVERVIEW_METRICS', 'STAFF_WORKLOAD_DISTRIBUTION', 'PORTFOLIO_COMPLIANCE_HEALTH', 'CLIENT_PORTAL_ACTIVITY', 'FEE_COLLECTIONS'];
  } else if (isGrowing) {
    defaultDashboardView = 'GROWING_PRACTICE';
    defaultRoute = '/dashboard';
    title = 'Growing Practice Executive Cockpit';
    description = 'Department-level performance, manager portfolio oversight, and assessment notice control.';
    primaryMetrics = ['ACTIVE_PORTFOLIOS', 'DEPARTMENT_UTILIZATION', 'CRITICAL_NOTICES', 'PRACTICE_REVENUE_RUN_RATE'];
    quickActions = ['DISPATCH_TASK_BATCH', 'ALLOCATE_PORTFOLIO', 'LOG_ASSESSMENT_NOTICE', 'EXPORT_PRACTICE_AUDIT'];
    recommendedWidgets = ['EXECUTIVE_PRACTICE_KPI', 'MANAGER_PORTFOLIO_HEALTH', 'NOTICE_DISPUTE_TRACKER', 'REALIZATION_ANALYTICS', 'TEAM_CAPACITY_HEATMAP'];
  } else if (isBusiness) {
    defaultDashboardView = 'IN_HOUSE_COMPLIANCE';
    defaultRoute = '/compliance';
    title = 'Corporate In-House Tax Dashboard';
    description = 'Internal statutory filings, tax notice dispute tracking, and corporate document archive.';
    primaryMetrics = ['UPCOMING_STATUTORY_DEADLINES', 'FILING_COMPLIANCE_RATE', 'ACTIVE_NOTICES_HEARINGS', 'PENDING_INTERNAL_TASKS'];
    quickActions = ['LOG_TAX_CHALLAN', 'FILE_GST_3B', 'LOG_INCOME_TAX_NOTICE', 'UPLOAD_ANNUAL_REPORT'];
    recommendedWidgets = ['CORPORATE_COMPLIANCE_CALENDAR', 'GST_TDS_FILING_PIPELINE', 'TAX_ASSESSMENT_NOTICE_BOARD', 'INTERNAL_AUDIT_VAULT'];
  }

  const moduleStatuses: Record<ProductCapability, ModuleRecommendationStatus> = {
    CLIENT_MANAGEMENT: isBusiness ? 'NOT_RECOMMENDED' : 'ACTIVE',
    GST_COMPLIANCE: 'ACTIVE',
    ITR_COMPLIANCE: 'ACTIVE',
    TDS_COMPLIANCE: 'ACTIVE',
    COMPLIANCE_CALENDAR: 'ACTIVE',
    TASK_MANAGEMENT: 'ACTIVE',
    DOCUMENT_MANAGEMENT: 'ACTIVE',
    DOCUMENT_REQUESTS: isBusiness ? 'NOT_RECOMMENDED' : 'ACTIVE',
    TAX_NOTICE_MANAGEMENT: isGrowing ? 'ACTIVE' : isSolo || isBusiness || isSmall ? 'RECOMMENDED' : 'ACTIVE',
    BILLING_INVOICING: isBusiness ? 'NOT_RECOMMENDED' : 'ACTIVE',
    CENTRAL_REPORTING: isSolo ? 'RECOMMENDED' : isBusiness ? 'RECOMMENDED' : 'ACTIVE',
    TEAM_MANAGEMENT: isSolo ? 'NOT_RECOMMENDED' : 'ACTIVE',
    CLIENT_PORTAL: isBusiness ? 'NOT_RECOMMENDED' : 'ACTIVE',
    ADVANCED_ANALYTICS: isGrowing ? 'ACTIVE' : 'UPGRADE_REQUIRED',
  };

  const dashboardProfile: DashboardProfile = {
    profileKey: defaultDashboardView,
    title,
    description,
    defaultRoute,
    primaryMetrics,
    quickActions,
    recommendedWidgets,
  };

  const onboardingChecklist: OnboardingStep[] = isSolo
    ? [
        {
          stepKey: 'PRACTICE_PROFILE',
          title: 'Complete Practice Profile',
          description: 'Setup your practice contact details, letterhead, and tax registrations.',
          targetRoute: '/settings/profile',
          sortOrder: 1,
          mandatory: true,
          targetCapability: null,
        },
        {
          stepKey: 'FIRST_CLIENT',
          title: 'Add First Client',
          description: 'Create your first client profile with PAN, GSTIN, and contact details.',
          targetRoute: '/clients/new',
          sortOrder: 2,
          mandatory: true,
          targetCapability: 'CLIENT_MANAGEMENT',
        },
        {
          stepKey: 'COMPLIANCE_SETUP',
          title: 'Configure Tax Compliance',
          description: 'Enable GST, ITR, and TDS return tracking for your clients.',
          targetRoute: '/compliance',
          sortOrder: 3,
          mandatory: true,
          targetCapability: 'GST_COMPLIANCE',
        },
      ]
    : isBusiness
    ? [
        {
          stepKey: 'CORPORATE_PROFILE',
          title: 'Corporate Profile & Registrations',
          description: 'Configure company legal name, PAN, CIN, TAN, and GSTIN registrations.',
          targetRoute: '/settings/profile',
          sortOrder: 1,
          mandatory: true,
          targetCapability: null,
        },
        {
          stepKey: 'INVITE_TAX_TEAM',
          title: 'Invite In-House Tax Team',
          description: 'Add finance managers, accounts officers, and internal auditors.',
          targetRoute: '/team',
          sortOrder: 2,
          mandatory: true,
          targetCapability: 'TEAM_MANAGEMENT',
        },
        {
          stepKey: 'CORPORATE_COMPLIANCE',
          title: 'Configure Corporate Tax Obligations',
          description: 'Setup GST 3B/1, TDS 24Q/26Q, and Corporate ITR-6 filing trackers.',
          targetRoute: '/compliance',
          sortOrder: 3,
          mandatory: true,
          targetCapability: 'GST_COMPLIANCE',
        },
      ]
    : [
        {
          stepKey: 'FIRM_PROFILE',
          title: 'Firm Profile & Letterhead',
          description: 'Configure firm credentials, partners, and office details.',
          targetRoute: '/settings/profile',
          sortOrder: 1,
          mandatory: true,
          targetCapability: null,
        },
        {
          stepKey: 'INVITE_STAFF',
          title: 'Invite Team Members',
          description: 'Add staff accountants, assistants, and assign system roles.',
          targetRoute: '/team',
          sortOrder: 2,
          mandatory: true,
          targetCapability: 'TEAM_MANAGEMENT',
        },
      ];

  return {
    organizationId: '',
    organizationType: orgType,
    subscriptionPlan: 'STARTER',
    enabledCapabilities: capabilities,
    moduleStatuses,
    recommendedModules: isBusiness
      ? ['GST', 'ITR', 'TDS', 'COMPLIANCE_CALENDAR', 'DOCUMENTS', 'TASKS', 'TEAM']
      : isSolo
      ? ['CLIENTS', 'GST', 'ITR', 'TDS', 'TASKS', 'BILLING']
      : isGrowing
      ? ['CLIENTS', 'GST', 'ITR', 'TDS', 'NOTICES', 'TASKS', 'TEAM', 'BILLING', 'PORTAL', 'REPORTS']
      : ['CLIENTS', 'GST', 'ITR', 'TDS', 'TASKS', 'TEAM', 'BILLING', 'PORTAL', 'REPORTS'],
    defaultDashboardView,
    dashboardProfile,
    onboardingProfile: `${orgType}_PROFILE`,
    onboardingChecklist,
    multiUserPractice: !isSolo,
    clientPortalSupported: !isBusiness,
    noticeCenterSupported: isGrowing || orgType === 'UNKNOWN',
    customInvoicingSupported: !isBusiness,
  };
}
