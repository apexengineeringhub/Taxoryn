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

    public static boolean hasRole(String role) {
        return getCurrentUser()
                .map(user -> user.getRoles().contains(role) || user.getRoles().contains("ROLE_" + role))
                .orElse(false);
    }

    public static Set<String> getCurrentRoles() {
        return getCurrentUser()
                .map(SecurityUser::getRoles)
                .orElse(Collections.emptySet());
    }
}
