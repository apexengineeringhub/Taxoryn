package com.taxoryn.core.domain;

import com.taxoryn.core.exception.TenantAccessDeniedException;
import com.taxoryn.core.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantAuditableEntityTest {

    static class SampleTenantEntity extends TenantAuditableEntity {
        public void triggerPrePersist() {
            onTenantPrePersist();
        }

        public void triggerPreUpdate() {
            onTenantPreUpdate();
        }
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("PrePersist automatically attaches active tenant ID if null")
    void testPrePersistAttachesTenantId() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        SampleTenantEntity entity = new SampleTenantEntity();
        entity.triggerPrePersist();

        assertEquals(tenantId, entity.getOrganizationId());
    }

    @Test
    @DisplayName("PrePersist throws when attempting to persist entity for different tenant")
    void testPrePersistCrossTenantThrows() {
        UUID activeTenant = UUID.randomUUID();
        UUID otherTenant = UUID.randomUUID();
        TenantContext.setTenantId(activeTenant);

        SampleTenantEntity entity = new SampleTenantEntity();
        entity.setOrganizationId(otherTenant);

        assertThrows(TenantAccessDeniedException.class, entity::triggerPrePersist);
    }

    @Test
    @DisplayName("PreUpdate throws when attempting to update entity for different tenant")
    void testPreUpdateCrossTenantThrows() {
        UUID activeTenant = UUID.randomUUID();
        UUID otherTenant = UUID.randomUUID();
        TenantContext.setTenantId(activeTenant);

        SampleTenantEntity entity = new SampleTenantEntity();
        entity.setOrganizationId(otherTenant);

        assertThrows(TenantAccessDeniedException.class, entity::triggerPreUpdate);
    }

    @Test
    @DisplayName("PreUpdate succeeds when active tenant matches entity organization ID")
    void testPreUpdateSucceeds() {
        UUID activeTenant = UUID.randomUUID();
        TenantContext.setTenantId(activeTenant);

        SampleTenantEntity entity = new SampleTenantEntity();
        entity.setOrganizationId(activeTenant);

        assertDoesNotThrow(entity::triggerPreUpdate);
    }
}
