package com.taxoryn.module.workflow.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.workflow.model.ServicePeriodStatus;
import com.taxoryn.module.workflow.model.ServicePeriodType;
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

import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a concrete recurring or event-driven compliance period for a client service engagement.
 */
@Entity
@Table(
        name = "client_service_periods",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_csp_tenant_service_label", columnNames = {"organization_id", "client_service_id", "period_label"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientServicePeriodEntity extends TenantAuditableEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "client_service_id", nullable = false)
    private UUID clientServiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 50)
    private ServicePeriodType periodType;

    @Column(name = "period_label", nullable = false, length = 100)
    private String periodLabel;

    @Column(name = "financial_year", length = 20)
    private String financialYear;

    @Column(name = "assessment_year", length = 20)
    private String assessmentYear;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ServicePeriodStatus status = ServicePeriodStatus.PLANNED;
}
