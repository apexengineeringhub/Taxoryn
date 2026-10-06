package com.taxoryn.module.consent.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.consent.model.ConsentMethod;
import com.taxoryn.module.consent.model.ConsentScope;
import com.taxoryn.module.consent.model.ConsentStatus;
import com.taxoryn.module.consent.model.DelegationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tenant-scoped Taxpayer Consent and Delegation entity.
 * Represents legal authorization given by a taxpayer/client to a practitioner or delegate.
 * Does NOT store authentication credentials or private keys.
 */
@Entity
@Table(name = "taxpayer_consents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxpayerConsentEntity extends TenantAuditableEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "consenting_user_id")
    private UUID consentingUserId;

    @Column(name = "delegate_user_id", nullable = false)
    private UUID delegateUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "delegation_type", nullable = false, length = 50)
    private DelegationType delegationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ConsentStatus status = ConsentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_method", nullable = false, length = 50)
    @Builder.Default
    private ConsentMethod consentMethod = ConsentMethod.IN_APP;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "taxpayer_consent_scopes", joinColumns = @JoinColumn(name = "consent_id"))
    @Column(name = "scope", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<ConsentScope> scopes = new HashSet<>();

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by")
    private UUID revokedBy;

    @Column(name = "revocation_reason", length = 1000)
    private String revocationReason;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "consent_reference", nullable = false, length = 255)
    private String consentReference;

    @Column(name = "correlation_id", length = 255)
    private String correlationId;

    public boolean isExpired() {
        return validUntil != null && Instant.now().isAfter(validUntil);
    }

    public boolean isUsableAt(Instant atTime) {
        Instant check = atTime != null ? atTime : Instant.now();
        if (status != ConsentStatus.ACTIVE) {
            return false;
        }
        if (validFrom != null && check.isBefore(validFrom)) {
            return false;
        }
        return validUntil == null || !check.isAfter(validUntil);
    }

    public boolean hasScope(ConsentScope requiredScope) {
        return ConsentScope.satisfiesAny(this.scopes, requiredScope);
    }

    public void revoke(UUID actorId, String reason) {
        this.status = ConsentStatus.REVOKED;
        this.revokedAt = Instant.now();
        this.revokedBy = actorId;
        this.revocationReason = reason;
    }

    public void approve(UUID consentingUser) {
        this.status = ConsentStatus.ACTIVE;
        this.consentingUserId = consentingUser;
    }

    public void reject(String reason) {
        this.status = ConsentStatus.REJECTED;
        this.rejectionReason = reason;
    }
}
