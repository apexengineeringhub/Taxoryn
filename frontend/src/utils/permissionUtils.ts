import { User } from '../types';

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
const SUPERADMIN_ROLES = [
  'TAXORYN_SUPERADMIN',
  'SUPER_ADMIN',
];

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
