package com.taxoryn.module.client.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "client_location_assignments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientLocationAssignmentEntity extends TenantAuditableEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(name = "primary_location", nullable = false)
    @Builder.Default
    private boolean primaryLocation = false;

    @Column(name = "assigned_at", nullable = false)
    @Builder.Default
    private Instant assignedAt = Instant.now();

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
