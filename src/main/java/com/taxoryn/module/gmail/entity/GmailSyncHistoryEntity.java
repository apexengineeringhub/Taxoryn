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
@Table(name = "gmail_sync_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GmailSyncHistoryEntity extends TenantAuditableEntity {

    @Column(name = "gmail_account_id", nullable = false)
    private UUID gmailAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_type", nullable = false, length = 50)
    @Builder.Default
    private GmailSyncType syncType = GmailSyncType.INCREMENTAL;

    @Column(name = "threads_synced", nullable = false)
    @Builder.Default
    private Integer threadsSynced = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private GmailSyncStatus status = GmailSyncStatus.SUCCESS;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;
}
