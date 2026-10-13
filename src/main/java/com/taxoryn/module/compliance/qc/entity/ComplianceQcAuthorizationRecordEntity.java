package com.taxoryn.module.compliance.qc.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.compliance.entity.ComplianceWorkflowEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Immutable audit log record capturing compliance filing authorization grant or revocation events.
 */
@Entity
@Table(name = "compliance_qc_authorization_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceQcAuthorizationRecordEntity extends TenantAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private ComplianceWorkflowEntity workflow;

    @Column(name = "action", nullable = false, length = 30)
    private String action; // GRANTED, REVOKED

    @Column(name = "authorization_method", length = 50)
    private String authorizationMethod;

    @Column(name = "authorization_reference", length = 255)
    private String authorizationReference;

    @Column(name = "authorization_notes", columnDefinition = "TEXT")
    private String authorizationNotes;

    @Column(name = "authorized_by", length = 255)
    private String authorizedBy;

    @Column(name = "revocation_reason", columnDefinition = "TEXT")
    private String revocationReason;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;
}
