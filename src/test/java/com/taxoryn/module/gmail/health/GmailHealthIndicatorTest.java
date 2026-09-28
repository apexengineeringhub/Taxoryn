package com.taxoryn.module.gmail.health;

import com.taxoryn.module.gmail.config.GmailProperties;
import com.taxoryn.module.gmail.entity.GmailAccountEntity;
import com.taxoryn.module.gmail.entity.GmailAccountStatus;
import com.taxoryn.module.gmail.repository.GmailAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GmailHealthIndicatorTest {

    @Mock
    private GmailAccountRepository accountRepository;

    private GmailProperties properties;
    private GmailHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        properties = new GmailProperties();
        properties.setEnabled(true);
        healthIndicator = new GmailHealthIndicator(properties, accountRepository);
    }

    @Test
    @DisplayName("Health is UP (disabled) when Gmail integration is toggled off")
    void testHealthWhenDisabled() {
        properties.setEnabled(false);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("enabled", false);
        assertThat(health.getDetails()).containsEntry("status", "DISABLED");
    }

    @Test
    @DisplayName("Health is UP with mailbox telemetry when enabled and healthy")
    void testHealthWhenEnabledAndHealthy() {
        GmailAccountEntity a1 = GmailAccountEntity.builder()
                .emailAddress("acc1@taxfirm.com")
                .status(GmailAccountStatus.CONNECTED)
                .build();
        a1.setId(UUID.randomUUID());

        GmailAccountEntity a2 = GmailAccountEntity.builder()
                .emailAddress("acc2@taxfirm.com")
                .status(GmailAccountStatus.CONNECTED)
                .build();
        a2.setId(UUID.randomUUID());

        when(accountRepository.findAll()).thenReturn(List.of(a1, a2));

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("enabled", true);
        assertThat(health.getDetails()).containsEntry("totalMailboxes", 2L);
        assertThat(health.getDetails()).containsEntry("connectedMailboxes", 2L);
        assertThat(health.getDetails()).containsEntry("authExpiredMailboxes", 0L);
        assertThat(health.getDetails()).doesNotContainKey("warning");
    }

    @Test
    @DisplayName("Health is UP with warning when mailboxes have AUTH_EXPIRED status")
    void testHealthWithAuthExpiredWarning() {
        GmailAccountEntity a1 = GmailAccountEntity.builder()
                .emailAddress("acc1@taxfirm.com")
                .status(GmailAccountStatus.CONNECTED)
                .build();
        a1.setId(UUID.randomUUID());

        GmailAccountEntity a2 = GmailAccountEntity.builder()
                .emailAddress("acc2@taxfirm.com")
                .status(GmailAccountStatus.AUTH_EXPIRED)
                .build();
        a2.setId(UUID.randomUUID());

        when(accountRepository.findAll()).thenReturn(List.of(a1, a2));

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("totalMailboxes", 2L);
        assertThat(health.getDetails()).containsEntry("connectedMailboxes", 1L);
        assertThat(health.getDetails()).containsEntry("authExpiredMailboxes", 1L);
        assertThat(health.getDetails()).containsKey("warning");
    }

    @Test
    @DisplayName("Health is DOWN when repository query throws exception")
    void testHealthDownOnError() {
        when(accountRepository.findAll()).thenThrow(new RuntimeException("Database connection timed out"));

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
    }
}
