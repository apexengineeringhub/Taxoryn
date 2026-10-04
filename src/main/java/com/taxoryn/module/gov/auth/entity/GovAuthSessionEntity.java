package com.taxoryn.module.gov.auth.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.gov.auth.model.GovAuthMethod;
import com.taxoryn.module.gov.auth.model.GovAuthStatus;
import com.taxoryn.module.gov.exception.GovIntegrationException;
import com.taxoryn.module.gov.model.GovErrorCode;
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

import java.time.Instant;
import java.util.UUID;

/**
 * Tenant-scoped government authentication session entity.
 * Tracks session state, expiration, method, and failure diagnostic metadata.
 * Does NOT persist raw credentials, passwords, tokens, OTPs, or private keys.
 */
@Entity
@Table(name = "gov_auth_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovAuthSessionEntity extends TenantAuditableEntity {

    @Column(name = "connection_id", nullable = false)
    private UUID connectionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 50)
    private GovProviderType providerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_method", nullable = false, length = 50)
    private GovAuthMethod authMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private GovAuthStatus status = GovAuthStatus.AUTHENTICATION_STARTED;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "authenticated_at")
    private Instant authenticatedAt;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "safe_failure_message", length = 1000)
    private String safeFailureMessage;

    @Column(name = "correlation_id", length = 255)
    private String correlationId;

    @Column(name = "requires_user_action", nullable = false)
    @Builder.Default
    private boolean requiresUserAction = false;

    @Column(name = "action_prompt", length = 1000)
    private String actionPrompt;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public void transitionTo(GovAuthStatus targetStatus) {
        if (this.status == targetStatus) {
            return;
        }

        boolean valid = switch (this.status) {
            case AUTHENTICATION_STARTED -> targetStatus == GovAuthStatus.AUTHENTICATION_PENDING
                    || targetStatus == GovAuthStatus.AUTHENTICATED
                    || targetStatus == GovAuthStatus.AUTHENTICATION_FAILED
                    || targetStatus == GovAuthStatus.EXPIRED;
            case AUTHENTICATION_PENDING -> targetStatus == GovAuthStatus.AUTHENTICATED
                    || targetStatus == GovAuthStatus.AUTHENTICATION_FAILED
                    || targetStatus == GovAuthStatus.EXPIRED;
            case AUTHENTICATED -> targetStatus == GovAuthStatus.EXPIRED
                    || targetStatus == GovAuthStatus.REVOKED
                    || targetStatus == GovAuthStatus.AUTHENTICATION_STARTED; // Re-authentication
            case AUTHENTICATION_FAILED -> targetStatus == GovAuthStatus.AUTHENTICATION_STARTED; // Retry
            case EXPIRED -> targetStatus == GovAuthStatus.AUTHENTICATION_STARTED; // Re-auth
            case REVOKED -> false; // Terminal
        };

        if (!valid) {
            throw new GovIntegrationException(GovErrorCode.VALIDATION_FAILED, "Cannot transition GovAuthSession from " + this.status + " to " + targetStatus);
        }

        this.status = targetStatus;
        this.lastActivityAt = Instant.now();
    }

    public void revoke() {
        if (this.status == GovAuthStatus.REVOKED) {
            return;
        }
        this.status = GovAuthStatus.REVOKED;
        this.lastActivityAt = Instant.now();
    }
}
