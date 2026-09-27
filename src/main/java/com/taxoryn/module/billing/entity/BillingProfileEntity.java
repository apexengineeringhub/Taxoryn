package com.taxoryn.module.billing.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents client or engagement billing configuration rules and pricing defaults.
 */
@Entity
@Table(name = "billing_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingProfileEntity extends TenantAuditableEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "engagement_id")
    private UUID engagementId;

    @Column(name = "billing_frequency", nullable = false, length = 50)
    @Builder.Default
    private String billingFrequency = "MONTHLY";

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "default_rate", precision = 15, scale = 2)
    private BigDecimal defaultRate;

    @Column(name = "tax_applicable", nullable = false)
    @Builder.Default
    private Boolean taxApplicable = true;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
