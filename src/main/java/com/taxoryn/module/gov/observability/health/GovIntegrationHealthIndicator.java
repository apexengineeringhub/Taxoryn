package com.taxoryn.module.gov.observability.health;

import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.outbox.model.GovOutboxStatus;
import com.taxoryn.module.gov.outbox.repository.GovOutboxEventRepository;
import com.taxoryn.module.gov.registry.GovernmentProviderRegistry;
import com.taxoryn.module.gov.reliability.dto.GovReliabilityMetricsSnapshot;
import com.taxoryn.module.gov.reliability.metrics.GovReliabilityMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Boot Actuator Health Indicator for Government Integration Framework.
 * Reports provider availability and outbox queue backlog safely without leaking secrets or PII.
 */
@Slf4j
@Component("govIntegration")
@RequiredArgsConstructor
public class GovIntegrationHealthIndicator implements HealthIndicator {

    private final GovernmentProviderRegistry providerRegistry;
    private final GovOutboxEventRepository outboxRepository;
    private final GovReliabilityMetrics reliabilityMetrics;

    @Override
    public Health health() {
        try {
            Map<String, Boolean> providerStatus = new HashMap<>();
            boolean allProvidersAvailable = true;

            for (GovProviderType type : GovProviderType.values()) {
                boolean supported = providerRegistry.isProviderSupported(type);
                providerStatus.put(type.name(), supported);
                if (!supported) {
                    allProvidersAvailable = false;
                }
            }

            long pendingOutboxCount = outboxRepository.countByStatus(GovOutboxStatus.PENDING);
            long failedOutboxCount = outboxRepository.countByStatus(GovOutboxStatus.FAILED);
            GovReliabilityMetricsSnapshot metricsSnapshot = reliabilityMetrics.getSnapshot();

            Health.Builder builder = allProvidersAvailable ? Health.up() : Health.status("DEGRADED");

            return builder
                    .withDetail("providers", providerStatus)
                    .withDetail("outboxPendingBacklog", pendingOutboxCount)
                    .withDetail("outboxFailedBacklog", failedOutboxCount)
                    .withDetail("operationsTotal", metricsSnapshot.getOperationsTotal())
                    .withDetail("operationsSuccess", metricsSnapshot.getOperationsSuccess())
                    .withDetail("operationsFailed", metricsSnapshot.getOperationsFailed())
                    .withDetail("retriesTotal", metricsSnapshot.getRetriesTotal())
                    .withDetail("reconciliationsAttemptedTotal", metricsSnapshot.getReconciliationsAttemptedTotal())
                    .build();
        } catch (Exception ex) {
            log.error("[GOV_HEALTH_INDICATOR_ERROR] Error evaluating government integration health: {}", ex.getMessage());
            return Health.down()
                    .withDetail("error", "Failed to evaluate government integration health")
                    .build();
        }
    }
}
