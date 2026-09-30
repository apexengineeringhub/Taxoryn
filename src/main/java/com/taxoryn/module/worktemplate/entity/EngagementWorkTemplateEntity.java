package com.taxoryn.module.worktemplate.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.worktemplate.model.RecurrenceType;
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
 * Associates an active Work Template to a Client Engagement with recurrence schedule tracking.
 */
@Entity
@Table(
        name = "engagement_work_templates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_eng_work_template", columnNames = {"organization_id", "engagement_id", "template_id"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngagementWorkTemplateEntity extends TenantAuditableEntity {

    @Column(name = "engagement_id", nullable = false)
    private UUID engagementId;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false, length = 50)
    @Builder.Default
    private RecurrenceType recurrenceType = RecurrenceType.MONTHLY;

    @Column(name = "recurrence_interval", nullable = false)
    @Builder.Default
    private int recurrenceInterval = 1;

    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    @Column(name = "month_of_year")
    private Integer monthOfYear;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "last_generated_period_start")
    private LocalDate lastGeneratedPeriodStart;

    @Column(name = "last_generated_period_end")
    private LocalDate lastGeneratedPeriodEnd;
}
