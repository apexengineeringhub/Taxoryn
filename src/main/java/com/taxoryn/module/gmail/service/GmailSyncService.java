package com.taxoryn.module.gmail.service;

import com.taxoryn.module.gmail.entity.GmailSyncStatus;
import com.taxoryn.module.gmail.entity.GmailSyncType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

public interface GmailSyncService {

    GmailSyncResult syncAccount(UUID organizationId, UUID accountId, GmailSyncType syncType);

    int syncAllActiveAccounts();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class GmailSyncResult {
        private UUID accountId;
        private GmailSyncType syncType;
        private int threadsSynced;
        private GmailSyncStatus status;
        private String errorMessage;
        private Instant startedAt;
        private Instant completedAt;
    }
}
