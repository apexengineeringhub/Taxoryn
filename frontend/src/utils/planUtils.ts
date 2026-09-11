export type SubscriptionPlanTier = 'STARTER' | 'PROFESSIONAL' | 'BUSINESS' | 'ENTERPRISE';

export const PLAN_DISPLAY_NAMES: Record<string, string> = {
  STARTER: 'Starter Practice',
  PROFESSIONAL: 'Professional Practice',
  BUSINESS: 'Business Firm',
  ENTERPRISE: 'Enterprise Organization',
};

export const PLAN_SHORT_NAMES: Record<string, string> = {
  STARTER: 'Starter',
  PROFESSIONAL: 'Professional',
  BUSINESS: 'Business',
  ENTERPRISE: 'Enterprise',
};

/**
 * Returns the human-readable canonical display name for a subscription plan tier.
 * Example: 'PROFESSIONAL' -> 'Professional Practice'
 */
export function formatPlanDisplayName(planCode?: string | null): string {
  if (!planCode) return 'Plan unavailable';
  const upper = planCode.toUpperCase().trim();
  return PLAN_DISPLAY_NAMES[upper] || `${upper.charAt(0) + upper.slice(1).toLowerCase()} Plan`;
}

/**
 * Returns a concise title/short name for a subscription plan tier.
 * Example: 'PROFESSIONAL' -> 'Professional'
 */
export function formatPlanShortName(planCode?: string | null): string {
  if (!planCode) return 'Plan unavailable';
  const upper = planCode.toUpperCase().trim();
  return PLAN_SHORT_NAMES[upper] || upper;
}
