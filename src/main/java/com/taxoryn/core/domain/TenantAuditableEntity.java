package com.taxoryn.core.domain;

import com.taxoryn.core.exception.TenantAccessDeniedException;
import com.taxoryn.core.security.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Base entity for all multi-tenant domain models.
 * Enforces tenant isolation and guarantees non-null organization_id.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class TenantAuditableEntity extends AuditableEntity {

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @PrePersist
    protected void onTenantPrePersist() {
        UUID currentTenant = TenantContext.getTenantId();
        if (this.organizationId == null) {
            if (currentTenant == null) {
                throw new TenantAccessDeniedException("Cannot persist tenant entity without an active tenant context");
            }
            this.organizationId = currentTenant;
        } else if (currentTenant != null && !this.organizationId.equals(currentTenant)) {
            throw new TenantAccessDeniedException("Cross-tenant persistence violation: Attempted to save entity under organization " + this.organizationId + " but active tenant is " + currentTenant);
        }
    }

    @PreUpdate
    protected void onTenantPreUpdate() {
        UUID currentTenant = TenantContext.getTenantId();
        if (currentTenant != null && this.organizationId != null && !this.organizationId.equals(currentTenant)) {
            throw new TenantAccessDeniedException("Cross-tenant update violation: Attempted to modify entity belonging to organization " + this.organizationId);
        }
    }
}
