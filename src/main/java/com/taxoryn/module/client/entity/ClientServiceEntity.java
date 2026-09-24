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

    @Column(name = "billing_frequency", length = 50)
    @Builder.Default
    private String billingFrequency = "MONTHLY";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
