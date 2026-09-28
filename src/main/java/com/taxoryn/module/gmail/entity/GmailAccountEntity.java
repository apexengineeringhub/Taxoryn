package com.taxoryn.module.gmail.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
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

@Entity
@Table(name = "gmail_accounts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GmailAccountEntity extends TenantAuditableEntity {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "email_address", nullable = false)
    private String emailAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 50)
    @Builder.Default
    private GmailAccountType accountType = GmailAccountType.PRACTICE_SHARED;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private GmailAccountStatus status = GmailAccountStatus.CONNECTED;

    @Column(name = "encrypted_access_token", columnDefinition = "TEXT")
    private String encryptedAccessToken;

    @Column(name = "encrypted_refresh_token", columnDefinition = "TEXT")
    private String encryptedRefreshToken;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @Column(name = "last_history_id", length = 100)
    private String lastHistoryId;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "sync_error_message", columnDefinition = "TEXT")
    private String syncErrorMessage;
}
