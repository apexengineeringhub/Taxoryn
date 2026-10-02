package com.taxoryn.module.review.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_actions", indexes = {
        @Index(name = "idx_review_action_org_request", columnList = "organization_id,review_request_id")
})
@Getter
@Setter
@NoArgsConstructor
public class ReviewActionEntity extends TenantAuditableEntity {
    @Column(name = "review_request_id", nullable = false)
    private UUID reviewRequestId;
    @Column(name = "action", nullable = false, length = 32)
    private String action;
    @Column(name = "actor_id", nullable = false)
    private UUID actorId;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;
}
