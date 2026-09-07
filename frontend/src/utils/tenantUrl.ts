/**
 * Taxoryn Centralized Public Profile & Route Utilities
 * Provides standard URL helpers for centralized practice discovery (e.g. /practice/:slug).
 * Tenant subdomain routing is disabled in favor of centralized SaaS routing at app.taxoryn.com.
 */

/**
 * Builds standard public practice route path.
 * E.g. "/practice/apex-tax"
 */
export function buildPracticePathUrl(slug?: string | null, queryParams?: Record<string, string>): string {
  if (!slug) return '/marketplace';
  const cleanSlug = slug.trim().toLowerCase();
  const target = `/practice/${cleanSlug}`;
  if (!queryParams || Object.keys(queryParams).length === 0) {
    return target;
  }
  const sp = new URLSearchParams(queryParams);
  return `${target}?${sp.toString()}`;
}

/**
 * Returns the fully qualified public practice profile URL on the centralized marketing/marketplace domain.
 * E.g. "https://taxoryn.com/practice/apex-tax"
 */
export function getPracticePublicUrl(slug?: string | null, path: string = ''): string {
  if (!slug) return 'https://taxoryn.com/marketplace';
  const cleanSlug = slug.trim().toLowerCase();
  const normalizedPath = path.startsWith('/') ? path : (path ? `/${path}` : '');
  return `https://taxoryn.com/practice/${cleanSlug}${normalizedPath}`;
}

/**
 * Formats a clean public display path/URL for practice directory branding.
 * E.g. "taxoryn.com/practice/apex-tax"
 */
export function formatPracticeDisplayUrl(slug?: string | null): string {
  if (!slug) return 'taxoryn.com/practice/your-practice';
  const cleanSlug = slug.trim().toLowerCase();
  return `taxoryn.com/practice/${cleanSlug}`;
}
