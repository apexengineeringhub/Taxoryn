import type { User } from '../types/index.ts';

export interface NavigationItem {
  label: string;
  path: string;
  icon?: any;
  requiredPermissions?: string[];
  allowedRoles?: string[];
  visible?: boolean;
}

/**
 * SuperAdmin / Platform roles that have global administrative bypass.
 */
export const SUPERADMIN_ROLES = [
  'TAXORYN_SUPERADMIN',
  'SUPER_ADMIN',
];

/**
 * Standard Notification permissions supported by Taxoryn backend.
 */
export const NOTIFICATION_PERMISSIONS = [
  'NOTIFICATION_READ',
  'NOTIFICATION_VIEW',
  'NOTIFICATIONS_VIEW',
];

/**
 * Platform and administrative roles with automatic notification management authorization.
 * Non-administrative practice users (Practitioners, Tax Professionals, Staff, Accountants)
 * require explicit NOTIFICATION_PERMISSIONS (e.g. NOTIFICATION_READ / NOTIFICATION_VIEW)
 * to access the Notification Center.
 */
export const NOTIFICATION_ADMIN_ROLES = [
  'TAXORYN_SUPERADMIN',
  'SUPER_ADMIN',
  'TAXORYN_OPERATIONS_ADMIN',
  'TAXORYN_SUPPORT_ADMIN',
  'TAXORYN_FINANCE_ADMIN',
  'TAXORYN_MARKETPLACE_ADMIN',
  'TAXORYN_CONTENT_ADMIN',
  'TAXORYN_SECURITY_ADMIN',
  'TAXORYN_ENGINEERING_ADMIN',
  'PRACTICE_OWNER',
  'PRACTICE_ADMIN',
  'ORG_ADMIN',
];

/**
 * Alias for backwards compatibility with existing route guards.
 */
export const NOTIFICATION_ALLOWED_ROLES = NOTIFICATION_ADMIN_ROLES;

/**
 * Canonical helper to check if a user has access to the internal Notification Center.
 */
export const hasNotificationAccess = (user: User | null | undefined): boolean => {
  return hasPermission(user, NOTIFICATION_PERMISSIONS, NOTIFICATION_ADMIN_ROLES);
};

/**
 * Checks if a user has specific permissions or allowed roles.
 *
 * Evaluation rules:
 * 1. SuperAdmins automatically pass all checks.
 * 2. If both `allowedRoles` and `requiredPermissions` are provided, passing EITHER is sufficient (roleMatches || permissionMatches).
 * 3. If only `allowedRoles` is provided, user must have at least one allowed role.
 * 4. If only `requiredPermissions` is provided, user must have at least one required permission.
 * 5. If neither is specified, item is considered public to the active persona.
 */
export const hasPermission = (
  user: User | null | undefined,
  requiredPermissions?: string[],
  allowedRoles?: string[]
): boolean => {
  if (!user) {
    return false;
  }

  const userRoleCodes = (user.roles || []).map((r: any) =>
    typeof r === 'string' ? r : r.code || ''
  );
  const userPermissions = user.permissions || [];

  // SuperAdmin bypass
  if (SUPERADMIN_ROLES.some((sr) => userRoleCodes.includes(sr))) {
    return true;
  }

  const hasRolesFilter = !!(allowedRoles && allowedRoles.length > 0);
  const hasPermissionsFilter = !!(requiredPermissions && requiredPermissions.length > 0);

  // If no restrictions are specified, access is granted
  if (!hasRolesFilter && !hasPermissionsFilter) {
    return true;
  }

  const roleMatches = hasRolesFilter
    ? allowedRoles!.some((r) => userRoleCodes.includes(r))
    : false;

  const permissionMatches = hasPermissionsFilter
    ? requiredPermissions!.some((p) => userPermissions.includes(p))
    : false;

  if (hasRolesFilter && hasPermissionsFilter) {
    return roleMatches || permissionMatches;
  }

  if (hasRolesFilter) {
    return roleMatches;
  }

  return permissionMatches;
};

/**
 * Evaluates whether a navigation item should be visible to the user.
 */
export const canAccessNavigationItem = (
  item: NavigationItem,
  user: User | null | undefined
): boolean => {
  // If explicitly flagged as not visible, hide it
  if (item.visible === false) {
    return false;
  }

  return hasPermission(user, item.requiredPermissions, item.allowedRoles);
};

/**
 * Filters a list of navigation items against the user's roles and permissions.
 */
export const filterNavigationByPermissions = (
  items: NavigationItem[],
  user: User | null | undefined
): NavigationItem[] => {
  return items.filter((item) => canAccessNavigationItem(item, user));
};

/**
 * Canonical paths and labels for global navigation items rendered in the lower
 * sidebar area across all authenticated portal types.
 */
export const GLOBAL_SIDEBAR_PATHS = new Set(['/settings/security', '/feedback', '/profile/security']);
export const GLOBAL_SIDEBAR_LABELS = new Set(['Security & Password', 'Give Feedback']);

/**
 * Checks if a navigation item is a global sidebar item.
 */
export const isGlobalSidebarItem = (item: { label?: string; path?: string } | null | undefined): boolean => {
  if (!item) return false;
  if (item.path && GLOBAL_SIDEBAR_PATHS.has(item.path)) return true;
  if (item.label && GLOBAL_SIDEBAR_LABELS.has(item.label)) return true;
  return false;
};

export interface NavigationSection {
  id: string;
  sectionTitle?: string;
  isCollapsible?: boolean;
  defaultExpanded?: boolean;
  items: NavigationItem[];
}

/**
 * Filters a list of navigation sections. Any section that contains 0 accessible items
 * after permission evaluation and global duplicate exclusion is completely excluded.
 */
export const filterNavigationSections = (
  sections: NavigationSection[],
  user: User | null | undefined
): NavigationSection[] => {
  return sections
    .map((section) => ({
      ...section,
      items: filterNavigationByPermissions(section.items, user).filter(
        (item) => !isGlobalSidebarItem(item)
      ),
    }))
    .filter((section) => section.items.length > 0);
};

/**
 * Filters flat role navigation items, excluding global sidebar items to prevent duplicate rendering.
 */
export const filterRoleNavigationItems = (
  items: NavigationItem[],
  user: User | null | undefined
): NavigationItem[] => {
  return filterNavigationByPermissions(items, user).filter(
    (item) => !isGlobalSidebarItem(item)
  );
};
