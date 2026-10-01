package com.taxoryn.module.service.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.service.model.ServicePricingMode;
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
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "practice_service_pricing", uniqueConstraints = @UniqueConstraint(name = "uk_practice_service_pricing", columnNames = {"organization_id", "tax_service_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PracticeServicePricingEntity extends TenantAuditableEntity {
    @Column(name = "tax_service_id", nullable = false)
    private UUID taxServiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_mode", nullable = false, length = 20)
    @Builder.Default
    private ServicePricingMode pricingMode = ServicePricingMode.DEFAULT;

    @Column(name = "custom_price", precision = 15, scale = 2)
    private BigDecimal customPrice;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;
}
