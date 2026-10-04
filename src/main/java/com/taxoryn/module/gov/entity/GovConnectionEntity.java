package com.taxoryn.module.gov.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.gov.exception.GovConnectionStateTransitionException;
import com.taxoryn.module.gov.model.GovConnectionStatus;
import com.taxoryn.module.gov.model.GovProviderType;
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

import java.util.UUID;

/**
 * Tenant-scoped government connection configuration entity.
 * Represents an organization's configuration and link to a government provider (e.g., GST, ITD, TRACES).
 */
@Entity
@Table(name = "gov_connections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovConnectionEntity extends TenantAuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 50)
    private GovProviderType providerType;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "environment", nullable = false, length = 50)
    @Builder.Default
    private String environment = "PRODUCTION";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private GovConnectionStatus status = GovConnectionStatus.CREATED;

    @Column(name = "credential_reference_id")
    private UUID credentialReferenceId;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false, length = 50)
    @Builder.Default
    private com.taxoryn.module.gov.model.GovConnectionHealthStatus healthStatus = com.taxoryn.module.gov.model.GovConnectionHealthStatus.UNKNOWN;

    @Column(name = "last_health_check_at")
    private java.time.Instant lastHealthCheckAt;

    @Column(name = "health_message", length = 1000)
    private String healthMessage;

    @Column(name = "health_latency_ms")
    private Long healthLatencyMs;

    public void updateHealth(com.taxoryn.module.gov.dto.GovHandshakeResult result) {
        if (result != null) {
            this.healthStatus = result.getHealthStatus();
            this.lastHealthCheckAt = result.getTimestamp() != null ? result.getTimestamp() : java.time.Instant.now();
            this.healthMessage = result.getMessage();
            this.healthLatencyMs = result.getLatencyMs();
        }
    }

    /**
     * Validates and executes state transitions for the government connection lifecycle.
     */
    public void transitionTo(GovConnectionStatus targetStatus) {
        if (this.status == targetStatus) {
            return;
        }

        boolean valid = switch (this.status) {
            case CREATED -> targetStatus == GovConnectionStatus.ACTIVE
                    || targetStatus == GovConnectionStatus.INACTIVE
                    || targetStatus == GovConnectionStatus.AUTH_REQUIRED
                    || targetStatus == GovConnectionStatus.FAILED;
            case ACTIVE -> targetStatus == GovConnectionStatus.INACTIVE
                    || targetStatus == GovConnectionStatus.AUTH_REQUIRED
                    || targetStatus == GovConnectionStatus.FAILED;
            case INACTIVE -> targetStatus == GovConnectionStatus.ACTIVE
                    || targetStatus == GovConnectionStatus.AUTH_REQUIRED
                    || targetStatus == GovConnectionStatus.FAILED;
            case AUTH_REQUIRED -> targetStatus == GovConnectionStatus.ACTIVE
                    || targetStatus == GovConnectionStatus.INACTIVE
                    || targetStatus == GovConnectionStatus.FAILED;
            case FAILED -> targetStatus == GovConnectionStatus.ACTIVE
                    || targetStatus == GovConnectionStatus.AUTH_REQUIRED
                    || targetStatus == GovConnectionStatus.INACTIVE;
        };

        if (!valid) {
            throw new GovConnectionStateTransitionException(this.status, targetStatus);
        }

        this.status = targetStatus;
    }
}
