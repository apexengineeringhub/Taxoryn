package com.taxoryn.module.audit.entity;

import com.taxoryn.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable Enterprise Audit Log Entity.
 * Represents an immutable audit trail record capturing system actions and state transitions.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEntity extends BaseEntity {

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "user_id", updatable = false)
    private UUID userId;

    @Column(name = "action", nullable = false, length = 100, updatable = false)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 100, updatable = false)
    private String entityType;

    @Column(name = "entity_name", length = 100, updatable = false)
    private String entityName;

    @Column(name = "entity_id", length = 255, updatable = false)
    private String entityId;

    @Column(name = "old_value", columnDefinition = "TEXT", updatable = false)
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT", updatable = false)
    private String newValue;

    @Column(name = "ip_address", length = 100, updatable = false)
    private String ipAddress;

    @Column(name = "request_id", length = 100, updatable = false)
    private String requestId;

    @Column(name = "user_agent", length = 500, updatable = false)
    private String userAgent;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    public void prePersistAudit() {
        if (!StringUtils.hasText(entityType) && StringUtils.hasText(entityName)) {
            entityType = entityName;
        } else if (StringUtils.hasText(entityType) && !StringUtils.hasText(entityName)) {
            entityName = entityType;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
