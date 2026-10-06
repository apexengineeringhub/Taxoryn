package com.taxoryn.module.gov.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.gov.model.GovCredentialStatus;
import com.taxoryn.module.gov.model.GovCredentialType;
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
 * Tenant-scoped government credential reference entity.
 * Holds non-sensitive credential metadata and encrypted secret payloads at rest.
 */
@Entity
@Table(name = "gov_credential_references")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovCredentialReferenceEntity extends TenantAuditableEntity {

    @Column(name = "connection_id", nullable = false)
    private UUID connectionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "credential_type", nullable = false, length = 50)
    private GovCredentialType credentialType;

    @Enumerated(EnumType.STRING)
    @Column(name = "credential_status", nullable = false, length = 50)
    @Builder.Default
    private GovCredentialStatus credentialStatus = GovCredentialStatus.VALID;

    @Column(name = "masked_identifier", length = 255)
    private String maskedIdentifier;

    @Column(name = "encrypted_secret", nullable = false, columnDefinition = "TEXT")
    private String encryptedSecret;

    @Column(name = "secret_storage_provider", nullable = false, length = 50)
    @Builder.Default
    private String secretStorageProvider = "LOCAL_AES_GCM";

    @Column(name = "last_validated_at")
    private Instant lastValidatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public void invalidate() {
        this.credentialStatus = GovCredentialStatus.REVOKED;
    }
}
