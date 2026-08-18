package com.taxoryn.core.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Set and get TenantId in same thread")
    void testSetAndGetTenantId() {
        UUID orgId = UUID.randomUUID();
        TenantContext.setTenantId(orgId);

        assertEquals(orgId, TenantContext.getTenantId());
        assertEquals(orgId, TenantContext.requireTenantId());
    }

    @Test
    @DisplayName("requireTenantId throws when context is empty")
    void testRequireTenantIdThrows() {
        assertNull(TenantContext.getTenantId());
        assertThrows(IllegalStateException.class, TenantContext::requireTenantId);
    }

    @Test
    @DisplayName("TenantContext is isolated between different threads")
    void testThreadIsolation() throws InterruptedException {
        UUID mainTenant = UUID.randomUUID();
        UUID asyncTenant = UUID.randomUUID();

        TenantContext.setTenantId(mainTenant);

        AtomicReference<UUID> otherThreadTenant = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            TenantContext.setTenantId(asyncTenant);
            otherThreadTenant.set(TenantContext.getTenantId());
            TenantContext.clear();
        });

        thread.start();
        thread.join();

        assertEquals(asyncTenant, otherThreadTenant.get());
        assertEquals(mainTenant, TenantContext.getTenantId());
    }
}
