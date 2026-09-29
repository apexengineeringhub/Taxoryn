package com.taxoryn.module.subscription.entity;

import com.taxoryn.core.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "subscription_plans")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanEntity extends AuditableEntity {

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "monthly_price", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal monthlyPrice = BigDecimal.ZERO;

    @Column(name = "yearly_price", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal yearlyPrice = BigDecimal.ZERO;

    @Column(name = "max_users", nullable = false)
    @Builder.Default
    private int maxUsers = 1;

    @Column(name = "max_clients", nullable = false)
    @Builder.Default
    private int maxClients = 25;

    @Column(name = "max_locations", nullable = false)
    @Builder.Default
    private int maxLocations = 1;

    @Column(name = "multi_location_enabled", nullable = false)
    @Builder.Default
    private boolean multiLocationEnabled = false;

    @Column(name = "max_storage_bytes", nullable = false)
    @Builder.Default
    private long maxStorageBytes = 5368709120L;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "ACTIVE";
}
