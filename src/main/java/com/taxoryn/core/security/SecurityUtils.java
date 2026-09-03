package com.taxoryn.core.security;

import com.taxoryn.core.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<SecurityUser> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser securityUser) {
            return Optional.of(securityUser);
        }
        return Optional.empty();
    }

    public static SecurityUser requireCurrentUser() {
        return getCurrentUser().orElseThrow(() -> new UnauthorizedException("User is not authenticated"));
    }

    public static UUID getCurrentUserId() {
        return requireCurrentUser().getUserId();
    }

    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            if (authentication.getPrincipal() instanceof SecurityUser securityUser) {
                return securityUser.getEmail();
            }
            return authentication.getName();
        }
        return "system";
    }

    public static UUID getCurrentOrganizationId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        return requireCurrentUser().getOrganizationId();
    }

    public static Optional<UUID> getCurrentClientId() {
        return getCurrentUser().map(SecurityUser::getClientId);
    }

    public static UUID requireCurrentClientId() {
        return getCurrentClientId().orElseThrow(() ->
                new UnauthorizedException("Authenticated user is not linked to any client record"));
    }

    public static boolean isClientPortalUser() {
        return hasRole("CLIENT_ADMIN") || hasRole("CLIENT_USER") || getCurrentClientId().isPresent();
    }

    public static boolean isMarketplaceCustomer() {
        return hasRole("MARKETPLACE_CUSTOMER");
    }

    public static boolean isTaxorynPlatformUser() {
        return isTaxorynSuperAdmin()
                || hasRole("TAXORYN_OPERATIONS_ADMIN")
                || hasRole("TAXORYN_SUPPORT_ADMIN")
                || hasRole("TAXORYN_FINANCE_ADMIN")
                || hasRole("TAXORYN_MARKETPLACE_ADMIN")
                || hasRole("TAXORYN_CONTENT_ADMIN")
                || hasRole("TAXORYN_SECURITY_ADMIN")
                || hasRole("TAXORYN_ENGINEERING_ADMIN");
    }

    public static boolean isTaxorynSuperAdmin() {
        return hasRole("TAXORYN_SUPERADMIN") || hasRole("SUPER_ADMIN");
    }

    public static boolean isTaxorynOperationsAdmin() {
        return hasRole("TAXORYN_OPERATIONS_ADMIN");
    }

    public static boolean isTaxorynSupportAdmin() {
        return hasRole("TAXORYN_SUPPORT_ADMIN");
    }

    public static boolean isTaxorynFinanceAdmin() {
        return hasRole("TAXORYN_FINANCE_ADMIN");
    }

    public static boolean isTaxorynMarketplaceAdmin() {
        return hasRole("TAXORYN_MARKETPLACE_ADMIN");
    }

    public static boolean isTaxorynContentAdmin() {
        return hasRole("TAXORYN_CONTENT_ADMIN");
    }

    public static boolean isTaxorynSecurityAdmin() {
        return hasRole("TAXORYN_SECURITY_ADMIN");
    }

    public static boolean isTaxorynEngineeringAdmin() {
        return hasRole("TAXORYN_ENGINEERING_ADMIN");
    }

    /**
     * Prevents privilege escalation by verifying if the caller is authorized to assign a target platform role.
     */
    public static boolean canAssignPlatformRole(String targetRoleCode) {
        if (targetRoleCode == null) return false;
        String cleanCode = targetRoleCode.trim().toUpperCase();

        if (isTaxorynSuperAdmin()) {
            return true; // SuperAdmin can assign all platform roles
        }

        if (isTaxorynOperationsAdmin()) {
            // Operations Admin can assign standard operational platform roles, but NEVER SuperAdmin, Security Admin, or Engineering Admin
            return switch (cleanCode) {
                case "TAXORYN_OPERATIONS_ADMIN", "TAXORYN_SUPPORT_ADMIN", "TAXORYN_MARKETPLACE_ADMIN",
                     "TAXORYN_FINANCE_ADMIN", "TAXORYN_CONTENT_ADMIN" -> true;
                default -> false;
            };
        }

        return false;
    }

    public static final Set<String> PLATFORM_ROLE_CODES = Set.of(
            "SUPER_ADMIN",
            "TAXORYN_SUPERADMIN",
            "TAXORYN_OPERATIONS_ADMIN",
            "TAXORYN_SUPPORT_ADMIN",
            "TAXORYN_MARKETPLACE_ADMIN",
            "TAXORYN_FINANCE_ADMIN",
            "TAXORYN_CONTENT_ADMIN",
            "TAXORYN_SECURITY_ADMIN",
            "TAXORYN_ENGINEERING_ADMIN"
    );

    public static final Set<String> PLATFORM_PERMISSION_CODES = Set.of(
            "PLATFORM_USER_VIEW",
            "PLATFORM_USER_CREATE",
            "PLATFORM_USER_UPDATE",
            "PLATFORM_USER_DELETE",
            "SYSTEM_STATUS_VIEW",
            "TECHNICAL_INCIDENT_VIEW",
            "TECHNICAL_INCIDENT_MANAGE"
    );

    public static boolean isPlatformRole(String roleCode) {
        if (roleCode == null) return false;
        String clean = roleCode.trim().toUpperCase();
        if (clean.startsWith("ROLE_")) {
            clean = clean.substring(5);
        }
        return PLATFORM_ROLE_CODES.contains(clean);
    }

    public static boolean isPlatformPermission(String permissionCode) {
        if (permissionCode == null) return false;
        return PLATFORM_PERMISSION_CODES.contains(permissionCode.trim().toUpperCase());
    }

    /**
     * Verifies that the caller has authority to assign or delegate the given roles.
     * Prevents tenant users from assigning platform roles, and non-superadmins from escalating privileges.
     */
    public static void validateRoleDelegation(Set<String> targetRoleCodes, UUID targetUserId) {
        if (targetRoleCodes == null || targetRoleCodes.isEmpty()) {
            return;
        }

        boolean isSuperAdmin = isTaxorynSuperAdmin();
        UUID currentUserId = getCurrentUser().map(SecurityUser::getUserId).orElse(null);

        // 1. Prevent non-superadmins from assigning any platform administrative roles
        for (String roleCode : targetRoleCodes) {
            if (isPlatformRole(roleCode) && !isSuperAdmin) {
                throw new com.taxoryn.core.exception.ForbiddenException(
                        "Privilege escalation denied: Platform role '" + roleCode + "' cannot be assigned by tenant users"
                );
            }
        }

        // 2. Prevent self-escalation (caller modifying their own roles unless they are a SuperAdmin)
        if (!isSuperAdmin && currentUserId != null && currentUserId.equals(targetUserId)) {
            Set<String> callerRoles = getCurrentRoles().stream()
                    .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                    .collect(Collectors.toSet());

            for (String targetRole : targetRoleCodes) {
                String cleanTarget = targetRole.startsWith("ROLE_") ? targetRole.substring(5) : targetRole;
                if (!callerRoles.contains(cleanTarget) && !callerRoles.contains("ORG_ADMIN")) {
                    throw new com.taxoryn.core.exception.ForbiddenException(
                            "Self-privilege escalation denied: You cannot grant yourself the '" + targetRole + "' role"
                    );
                }
            }
        }

        // 3. Non-Org-Admins / Non-SuperAdmins cannot delegate roles they do not possess
        if (!isSuperAdmin && !hasRole("ORG_ADMIN")) {
            Set<String> callerRoles = getCurrentRoles().stream()
                    .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                    .collect(Collectors.toSet());

            for (String targetRole : targetRoleCodes) {
                String cleanTarget = targetRole.startsWith("ROLE_") ? targetRole.substring(5) : targetRole;
                if (!callerRoles.contains(cleanTarget)) {
                    throw new com.taxoryn.core.exception.ForbiddenException(
                            "Role delegation boundary violation: You cannot assign role '" + targetRole + "' because you do not hold this role"
                    );
                }
            }
        }
    }

    /**
     * Verifies that the caller has authority to include target permissions in a custom role.
     */
    public static void validatePermissionDelegation(Set<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return;
        }

        boolean isSuperAdmin = isTaxorynSuperAdmin();

        // 1. Block platform permissions from being added to tenant custom roles by non-superadmins
        for (String permCode : permissionCodes) {
            if (isPlatformPermission(permCode) && !isSuperAdmin) {
                throw new com.taxoryn.core.exception.ForbiddenException(
                        "Permission delegation denied: Platform permission '" + permCode + "' cannot be assigned to tenant custom roles"
                );
            }
        }

        // 2. If caller is not Org Admin or Super Admin, they cannot create a role with permissions they don't have
        if (!isSuperAdmin && !hasRole("ORG_ADMIN")) {
            Set<String> callerPerms = getCurrentUser()
                    .map(SecurityUser::getPermissions)
                    .orElse(Collections.emptySet());

            for (String permCode : permissionCodes) {
                if (!callerPerms.contains(permCode)) {
                    throw new com.taxoryn.core.exception.ForbiddenException(
                            "Permission delegation boundary violation: You cannot grant permission '" + permCode + "' which you do not possess"
                    );
                }
            }
        }
    }

    public static boolean hasAuthority(String authority) {
        return getCurrentUser()
                .map(user -> (user.getPermissions() != null && user.getPermissions().contains(authority))
                        || user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(authority)))
                .orElse(false);
    }

    public static boolean hasRole(String role) {
        return getCurrentUser()
                .map(user -> user.getRoles() != null && (user.getRoles().contains(role) || user.getRoles().contains("ROLE_" + role)))
                .orElse(false);
    }

    public static Set<String> getCurrentRoles() {
        return getCurrentUser()
                .map(SecurityUser::getRoles)
                .orElse(Collections.emptySet());
    }
}
