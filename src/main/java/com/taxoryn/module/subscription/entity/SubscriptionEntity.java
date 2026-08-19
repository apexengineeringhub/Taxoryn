package com.taxoryn.module.subscription.entity;

import com.taxoryn.core.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionEntity extends AuditableEntity {

    @Column(name = "organization_id", nullable = false, unique = true)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 50)
    @Builder.Default
    private SubscriptionPlan plan = SubscriptionPlan.STARTER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", nullable = false, length = 20)
    @Builder.Default
    private BillingInterval billingInterval = BillingInterval.MONTHLY;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "renewal_date", nullable = false)
    private LocalDate renewalDate;

    @Column(name = "max_users", nullable = false)
    @Builder.Default
    private int maxUsers = 5;

    @Column(name = "max_clients", nullable = false)
    @Builder.Default
    private int maxClients = 25;

    @Column(name = "max_storage_bytes", nullable = false)
    @Builder.Default
    private long maxStorageBytes = 5368709120L; // 5 GB

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal price = new BigDecimal("999.00");

    @Column(name = "auto_renew", nullable = false)
    @Builder.Default
    private boolean autoRenew = true;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    public enum SubscriptionPlan {
        STARTER,
        PROFESSIONAL,
        BUSINESS,
        ENTERPRISE
    }

    public enum SubscriptionStatus {
        TRIALING,
        ACTIVE,
        PAST_DUE,
        CANCELED,
        EXPIRED
    }

    public enum BillingInterval {
        MONTHLY,
        YEARLY
    }
}
