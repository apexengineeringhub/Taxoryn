package com.taxoryn.core.security;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * ThreadLocal holder for current tenant (Organization) ID.
 * Guarantees zero leakage across async threads when properly cleared.
 */
@Slf4j
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(UUID tenantId) {
        log.trace("Setting TenantContext to organizationId={}", tenantId);
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static UUID requireTenantId() {
        UUID tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new IllegalStateException("TenantContext is missing. Ensure the request is authenticated with an organization context.");
        }
        return tenantId;
    }

    public static void clear() {
        log.trace("Clearing TenantContext");
        CURRENT_TENANT.remove();
    }
}
