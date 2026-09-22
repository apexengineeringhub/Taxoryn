package com.taxoryn.module.moduleconfig.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tenant-scoped configuration indicating whether a product module is enabled or disabled
 * for an organization.
 */
@Entity
@Table(
        name = "organization_modules",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_org_module", columnNames = {"organization_id", "module_code"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationModuleEntity extends TenantAuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "module_code", nullable = false, length = 50)
    private ProductModuleCode moduleCode;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;
}
