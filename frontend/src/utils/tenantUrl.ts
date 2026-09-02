/**
 * Taxoryn Multi-Tenant Subdomain & URL Utilities
 * Handles tenant subdomain extraction, URL construction, and dynamic routing
 * for practitioner-specific branded domains (e.g., https://apex.taxoryn.com).
 */

const RESERVED_SUBDOMAINS = new Set([
  'www',
  'app',
  'admin',
  'api',
  'platform',
  'localhost',
  'staging',
  'dev',
  'test',
  'mail',
  'smtp',
  'status',
  'learn',
]);

const ROOT_DOMAINS = [
  'taxoryn.com',
  'taxoryan.com',
  'taxoryn.in',
  'taxoryn.dev',
  'localhost',
];

/**
 * Extracts the tenant subdomain from the current window location or hostname.
 * Examples:
 * - apex.taxoryn.com -> "apex"
 * - apex.localhost -> "apex"
 * - www.taxoryn.com -> null (reserved)
 * - taxoryn.com -> null
 * - localhost:5173?tenant=apex -> "apex" (development fallback)
 */
export function getTenantSubdomain(hostname: string = typeof window !== 'undefined' ? window.location.hostname : ''): string | null {
  // 1. Check development query parameter fallback (?tenant=apex)
  if (typeof window !== 'undefined' && window.location.search) {
    const params = new URLSearchParams(window.location.search);
    const tenantParam = params.get('tenant') || params.get('practice');
    if (tenantParam) {
      return tenantParam.trim().toLowerCase();
    }
  }

  if (!hostname) return null;

  const cleanHost = hostname.toLowerCase().split(':')[0]; // remove port if present

  // 2. Handle localhost or IP
  if (cleanHost === 'localhost' || cleanHost === '127.0.0.1') {
    return null;
  }

  // 3. Handle *.localhost (e.g., apex.localhost)
  if (cleanHost.endsWith('.localhost')) {
    const sub = cleanHost.replace('.localhost', '');
    return RESERVED_SUBDOMAINS.has(sub) ? null : sub;
  }

  // 4. Handle standard production domains (e.g. apex.taxoryn.com)
  for (const root of ROOT_DOMAINS) {
    if (cleanHost.endsWith(`.${root}`)) {
      const sub = cleanHost.slice(0, cleanHost.length - root.length - 1);
      // If there are multiple levels (e.g. foo.bar.taxoryn.com), take the innermost
      const parts = sub.split('.');
      const candidate = parts[parts.length - 1];
      if (candidate && !RESERVED_SUBDOMAINS.has(candidate)) {
        return candidate;
      }
    }
  }

  // 5. Fallback for custom domains (e.g. sub.domain.com with 3+ parts)
  const parts = cleanHost.split('.');
  if (parts.length >= 3) {
    const candidate = parts[0];
    if (candidate && !RESERVED_SUBDOMAINS.has(candidate)) {
      return candidate;
    }
  }

  return null;
}

/**
 * Constructs a fully qualified practitioner tenant URL.
 * In production: https://{slug}.taxoryn.com{path}
 * In local/dev:  http://localhost:5173/?tenant={slug} or http://{slug}.localhost:5173{path}
 */
export function buildTenantSubdomainUrl(slug?: string | null, path: string = ''): string {
  if (!slug) {
    return typeof window !== 'undefined' ? window.location.origin : 'https://taxoryn.com';
  }

  const cleanSlug = slug.trim().toLowerCase();
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;

  if (typeof window === 'undefined') {
    return `https://${cleanSlug}.taxoryn.com${normalizedPath === '/' ? '' : normalizedPath}`;
  }

  const hostname = window.location.hostname;
  const protocol = window.location.protocol;
  const port = window.location.port ? `:${window.location.port}` : '';

  // Local development: provide working link
  if (hostname === 'localhost' || hostname === '127.0.0.1') {
    if (normalizedPath && normalizedPath !== '/') {
      return `${protocol}//${hostname}${port}${normalizedPath}?tenant=${cleanSlug}`;
    }
    return `${protocol}//${hostname}${port}/?tenant=${cleanSlug}`;
  }

  if (hostname.endsWith('.localhost')) {
    return `${protocol}//${cleanSlug}.localhost${port}${normalizedPath === '/' ? '' : normalizedPath}`;
  }

  // Production or staging domain
  for (const root of ROOT_DOMAINS) {
    if (hostname.includes(root)) {
      return `https://${cleanSlug}.${root}${normalizedPath === '/' ? '' : normalizedPath}`;
    }
  }

  // Default Taxoryn Cloud URL
  return `https://${cleanSlug}.taxoryn.com${normalizedPath === '/' ? '' : normalizedPath}`;
}

/**
 * Returns the exact production subdomain URL regardless of current environment.
 * E.g. "https://apex.taxoryn.com"
 */
export function getProductionSubdomainUrl(slug?: string | null, path: string = ''): string {
  if (!slug) return 'https://taxoryn.com';
  const cleanSlug = slug.trim().toLowerCase();
  const normalizedPath = path.startsWith('/') ? path : (path ? `/${path}` : '');
  return `https://${cleanSlug}.taxoryn.com${normalizedPath}`;
}

/**
 * Formats a clean public display URL for practitioner branding.
 * E.g. "apex.taxoryn.com"
 */
export function formatTenantDisplayUrl(slug?: string | null): string {
  if (!slug) return 'yourpractice.taxoryn.com';
  const cleanSlug = slug.trim().toLowerCase();
  return `${cleanSlug}.taxoryn.com`;
}

/**
 * Builds standard public route fallback path.
 * E.g. "/practice/apex"
 */
export function buildPracticePathUrl(slug?: string | null, queryParams?: Record<string, string>): string {
  if (!slug) return '/marketplace';
  const target = `/practice/${slug.trim().toLowerCase()}`;
  if (!queryParams || Object.keys(queryParams).length === 0) {
    return target;
  }
  const sp = new URLSearchParams(queryParams);
  return `${target}?${sp.toString()}`;
}

