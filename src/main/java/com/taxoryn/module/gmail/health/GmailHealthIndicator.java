package com.taxoryn.module.gmail.health;

import com.taxoryn.module.gmail.config.GmailProperties;
import com.taxoryn.module.gmail.entity.GmailAccountStatus;
import com.taxoryn.module.gmail.repository.GmailAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Spring Boot Actuator health indicator for Gmail metadata integration.
 * Safe and production-ready: never exposes tokens or sensitive headers.
 */
@Slf4j
@Component("gmailHealthIndicator")
@RequiredArgsConstructor
public class GmailHealthIndicator implements HealthIndicator {

    private final GmailProperties properties;
    private final GmailAccountRepository accountRepository;

    @Override
    public Health health() {
        if (properties == null || !properties.isEnabled() || (properties.getHealth() != null && !properties.getHealth().isEnabled())) {
            return Health.up()
                    .withDetail("provider", "Google Gmail API")
                    .withDetail("enabled", false)
                    .withDetail("status", "DISABLED")
                    .build();
        }

        try {
            var accounts = accountRepository.findAll();
            long total = accounts.size();
            long connected = accounts.stream().filter(a -> a.getStatus() == GmailAccountStatus.CONNECTED).count();
            long expired = accounts.stream().filter(a -> a.getStatus() == GmailAccountStatus.AUTH_EXPIRED).count();
            long syncErrors = accounts.stream().filter(a -> a.getStatus() == GmailAccountStatus.SYNC_ERROR).count();
            long disconnected = accounts.stream().filter(a -> a.getStatus() == GmailAccountStatus.DISCONNECTED).count();

            Health.Builder builder = Health.up()
                    .withDetail("provider", "Google Gmail API (Metadata-only)")
                    .withDetail("enabled", true)
                    .withDetail("totalMailboxes", total)
                    .withDetail("connectedMailboxes", connected)
                    .withDetail("authExpiredMailboxes", expired)
                    .withDetail("syncErrorMailboxes", syncErrors)
                    .withDetail("disconnectedMailboxes", disconnected);

            if (expired > 0 || syncErrors > 0) {
                builder.withDetail("warning", String.format("%d mailbox(es) require attention (expired/error)", expired + syncErrors));
            }

            return builder.build();
        } catch (Exception ex) {
            log.warn("Gmail health check encountered an error: {}", ex.getMessage());
            return Health.down()
                    .withDetail("provider", "Google Gmail API")
                    .withDetail("error", "Failed to query Gmail integration state: " + ex.getMessage())
                    .build();
        }
    }
}
