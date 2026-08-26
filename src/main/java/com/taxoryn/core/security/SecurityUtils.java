package com.taxoryn.core.security;

import com.taxoryn.core.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
        return requireCurrentUser().getEmail();
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
