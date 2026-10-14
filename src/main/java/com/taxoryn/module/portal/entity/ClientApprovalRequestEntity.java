package com.taxoryn.module.portal.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Tenant-scoped client approval and consent request entity.
 * Represents an explicit sign-off, consent, or confirmation request presented to a client
 * for tax computations, filing consents, engagement terms, or document delivery.
 */
@Entity
@Table(name = "client_approval_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientApprovalRequestEntity extends TenantAuditableEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_type", nullable = false, length = 50)
    private ClientApprovalType approvalType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ClientApprovalStatus status = ClientApprovalStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", nullable = false, length = 50)
    @Builder.Default
    private MessageContextType contextType = MessageContextType.GENERAL;

    @Column(name = "context_id")
    private UUID contextId;

    @Column(name = "context_title", length = 255)
    private String contextTitle;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "content_version", length = 50)
    private String contentVersion;

    @Column(name = "content_hash", length = 128)
    private String contentHash;

    @Column(name = "requested_by_user_id")
    private UUID requestedByUserId;

    @Column(name = "requested_by_name", length = 255)
    private String requestedByName;

    @Column(name = "requested_at", nullable = false)
    @Builder.Default
    private Instant requestedAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "responded_by_user_id")
    private UUID respondedByUserId;

    @Column(name = "responded_by_name", length = 255)
    private String respondedByName;

    @Column(name = "responded_by_email", length = 255)
    private String respondedByEmail;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "decision_notes", columnDefinition = "TEXT")
    private String decisionNotes;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "revoked_by_user_id")
    private UUID revokedByUserId;

    @Column(name = "revoked_by_name", length = 255)
    private String revokedByName;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revocation_reason", columnDefinition = "TEXT")
    private String revocationReason;

    public boolean isExpired() {
        return status == ClientApprovalStatus.PENDING && expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean canDecide() {
        return status == ClientApprovalStatus.PENDING && !isExpired();
    }

    public void approve(UUID clientUserId, String clientUserName, String clientUserEmail, String notes) {
        if (!canDecide()) {
            throw new IllegalStateException("Approval request cannot be approved in state " + status);
        }
        this.status = ClientApprovalStatus.APPROVED;
        this.respondedByUserId = clientUserId;
        this.respondedByName = clientUserName;
        this.respondedByEmail = clientUserEmail;
        this.respondedAt = Instant.now();
        this.decisionNotes = notes;
    }

    public void reject(UUID clientUserId, String clientUserName, String clientUserEmail, String reason) {
        if (!canDecide()) {
            throw new IllegalStateException("Approval request cannot be rejected in state " + status);
        }
        this.status = ClientApprovalStatus.REJECTED;
        this.respondedByUserId = clientUserId;
        this.respondedByName = clientUserName;
        this.respondedByEmail = clientUserEmail;
        this.respondedAt = Instant.now();
        this.rejectionReason = reason;
    }

    public void revoke(UUID practiceUserId, String practiceUserName, String reason) {
        if (this.status != ClientApprovalStatus.PENDING) {
            throw new IllegalStateException("Only PENDING approval requests can be revoked/cancelled");
        }
        this.status = ClientApprovalStatus.REVOKED;
        this.revokedByUserId = practiceUserId;
        this.revokedByName = practiceUserName;
        this.revokedAt = Instant.now();
        this.revocationReason = reason;
    }
}
