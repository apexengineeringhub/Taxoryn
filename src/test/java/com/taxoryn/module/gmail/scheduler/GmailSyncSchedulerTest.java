package com.taxoryn.module.gmail.scheduler;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.gmail.config.GmailProperties;
import com.taxoryn.module.gmail.entity.GmailAccountEntity;
import com.taxoryn.module.gmail.entity.GmailAccountStatus;
import com.taxoryn.module.gmail.entity.GmailSyncStatus;
import com.taxoryn.module.gmail.entity.GmailSyncType;
import com.taxoryn.module.gmail.repository.GmailAccountRepository;
import com.taxoryn.module.gmail.service.GmailSyncService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GmailSyncSchedulerTest {

    @Mock
    private GmailSyncService syncService;

    @Mock
    private GmailAccountRepository accountRepository;

    private GmailProperties properties;
    private GmailSyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        properties = new GmailProperties();
        properties.setEnabled(true);
        properties.getSync().setEnabled(true);
        scheduler = new GmailSyncScheduler(properties, syncService, accountRepository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Periodic sync runs across all CONNECTED accounts with proper tenant switching")
    void testPeriodicSyncRunsForConnectedAccounts() {
        UUID org1 = UUID.randomUUID();
        UUID org2 = UUID.randomUUID();
        UUID acc1 = UUID.randomUUID();
        UUID acc2 = UUID.randomUUID();

        GmailAccountEntity a1 = GmailAccountEntity.builder()
                .emailAddress("acc1@taxfirm.com")
                .status(GmailAccountStatus.CONNECTED)
                .build();
        a1.setId(acc1);
        a1.setOrganizationId(org1);

        GmailAccountEntity a2 = GmailAccountEntity.builder()
                .emailAddress("acc2@taxfirm.com")
                .status(GmailAccountStatus.CONNECTED)
                .build();
        a2.setId(acc2);
        a2.setOrganizationId(org2);

        GmailAccountEntity a3Disconnected = GmailAccountEntity.builder()
                .emailAddress("acc3@taxfirm.com")
                .status(GmailAccountStatus.DISCONNECTED)
                .build();
        a3Disconnected.setId(UUID.randomUUID());
        a3Disconnected.setOrganizationId(org1);

        when(accountRepository.findAll()).thenReturn(List.of(a1, a2, a3Disconnected));

        when(syncService.syncAccount(eq(org1), eq(acc1), eq(GmailSyncType.INCREMENTAL)))
                .thenReturn(GmailSyncService.GmailSyncResult.builder()
                        .accountId(acc1)
                        .syncType(GmailSyncType.INCREMENTAL)
                        .threadsSynced(5)
                        .status(GmailSyncStatus.SUCCESS)
                        .startedAt(Instant.now())
                        .completedAt(Instant.now())
                        .build());

        when(syncService.syncAccount(eq(org2), eq(acc2), eq(GmailSyncType.INCREMENTAL)))
                .thenReturn(GmailSyncService.GmailSyncResult.builder()
                        .accountId(acc2)
                        .syncType(GmailSyncType.INCREMENTAL)
                        .threadsSynced(2)
                        .status(GmailSyncStatus.SUCCESS)
                        .startedAt(Instant.now())
                        .completedAt(Instant.now())
                        .build());

        scheduler.runPeriodicSync();

        verify(syncService).syncAccount(org1, acc1, GmailSyncType.INCREMENTAL);
        verify(syncService).syncAccount(org2, acc2, GmailSyncType.INCREMENTAL);
        verify(syncService, never()).syncAccount(eq(org1), eq(a3Disconnected.getId()), any());
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Periodic sync skips execution when disabled in configuration")
    void testPeriodicSyncSkipsWhenDisabled() {
        properties.setEnabled(false);

        scheduler.runPeriodicSync();

        verifyNoInteractions(accountRepository);
        verifyNoInteractions(syncService);

        properties.setEnabled(true);
        properties.getSync().setEnabled(false);

        scheduler.runPeriodicSync();

        verifyNoInteractions(accountRepository);
        verifyNoInteractions(syncService);
    }

    @Test
    @DisplayName("Periodic sync handles exceptions on single account and continues processing other accounts")
    void testPeriodicSyncHandlesFailureGracefully() {
        UUID org1 = UUID.randomUUID();
        UUID org2 = UUID.randomUUID();
        UUID acc1 = UUID.randomUUID();
        UUID acc2 = UUID.randomUUID();

        GmailAccountEntity a1 = GmailAccountEntity.builder()
                .emailAddress("acc1@taxfirm.com")
                .status(GmailAccountStatus.CONNECTED)
                .build();
        a1.setId(acc1);
        a1.setOrganizationId(org1);

        GmailAccountEntity a2 = GmailAccountEntity.builder()
                .emailAddress("acc2@taxfirm.com")
                .status(GmailAccountStatus.CONNECTED)
                .build();
        a2.setId(acc2);
        a2.setOrganizationId(org2);

        when(accountRepository.findAll()).thenReturn(List.of(a1, a2));

        when(syncService.syncAccount(eq(org1), eq(acc1), eq(GmailSyncType.INCREMENTAL)))
                .thenThrow(new RuntimeException("Google API 503 Service Unavailable"));

        when(syncService.syncAccount(eq(org2), eq(acc2), eq(GmailSyncType.INCREMENTAL)))
                .thenReturn(GmailSyncService.GmailSyncResult.builder()
                        .accountId(acc2)
                        .syncType(GmailSyncType.INCREMENTAL)
                        .threadsSynced(3)
                        .status(GmailSyncStatus.SUCCESS)
                        .startedAt(Instant.now())
                        .completedAt(Instant.now())
                        .build());

        scheduler.runPeriodicSync();

        verify(syncService).syncAccount(org1, acc1, GmailSyncType.INCREMENTAL);
        verify(syncService).syncAccount(org2, acc2, GmailSyncType.INCREMENTAL);
        assertThat(TenantContext.getTenantId()).isNull();
    }
}
