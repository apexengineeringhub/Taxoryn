package com.taxoryn.module.engagement.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.engagement.model.EngagementStatus;
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
 * Represents a professional service engagement relationship between the practice and a client.
 */
@Entity
@Table(name = "engagements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngagementEntity extends TenantAuditableEntity {

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "client_service_id")
    private UUID clientServiceId;

    @Column(name = "engagement_code", length = 100)
    private String engagementCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private EngagementStatus status = EngagementStatus.ACTIVE;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "assigned_user_id")
    private UUID assignedUserId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
