package com.taxoryn.module.timetracking.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.timetracking.model.TimeEntryStatus;
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
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a recorded unit of professional time logged against a client, engagement, work item, or task.
 */
@Entity
@Table(name = "time_entries")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntryEntity extends TenantAuditableEntity {

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "engagement_id")
    private UUID engagementId;

    @Column(name = "work_item_id")
    private UUID workItemId;

    @Column(name = "task_id")
    private UUID taskId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "billable", nullable = false)
    @Builder.Default
    private Boolean billable = true;

    @Column(name = "billing_rate", precision = 15, scale = 2)
    private BigDecimal billingRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private TimeEntryStatus status = TimeEntryStatus.SUBMITTED;
}
