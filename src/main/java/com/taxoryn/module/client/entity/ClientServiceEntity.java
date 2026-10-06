package com.taxoryn.module.client.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
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

/**
 * Represents a client-service engagement association within a practice tenant.
 */
@Entity
@Table(name = "client_services")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientServiceEntity extends TenantAuditableEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "service_offering_id")
    private UUID serviceOfferingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 100)
    private ClientServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ClientServiceStatus status = ClientServiceStatus.ACTIVE;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "assigned_employee_id")
    private UUID assignedEmployeeId;

    @Column(name = "responsible_user_id")
    private UUID responsibleUserId;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "agreed_price", precision = 15, scale = 2)
    private BigDecimal agreedPrice;

    @Column(name = "billing_frequency", length = 50)
    @Builder.Default
    private String billingFrequency = "MONTHLY";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "status_changed_at")
    private Instant statusChangedAt;

    @Column(name = "status_changed_by")
    private UUID statusChangedBy;

    @Column(name = "status_change_reason", length = 500)
    private String statusChangeReason;
}
