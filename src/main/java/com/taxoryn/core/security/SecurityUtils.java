package com.taxoryn.core.security;

import com.taxoryn.core.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
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

    public static UUID getCurrentOrganizationId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        return requireCurrentUser().getOrganizationId();
    }

    public static boolean hasRole(String role) {
        return getCurrentUser()
                .map(user -> user.getRoles().contains(role) || user.getRoles().contains("ROLE_" + role))
                .orElse(false);
    }
}
