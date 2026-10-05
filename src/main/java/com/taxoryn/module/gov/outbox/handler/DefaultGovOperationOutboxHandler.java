package com.taxoryn.module.gov.outbox.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.entity.GovIntegrationOperationEntity;
import com.taxoryn.module.gov.exception.GovIntegrationException;
import com.taxoryn.module.gov.outbox.entity.GovOutboxEventEntity;
import com.taxoryn.module.gov.outbox.model.GovOutboxEventType;
import com.taxoryn.module.gov.reliability.service.GovOperationRecoveryService;
import com.taxoryn.module.gov.reliability.service.GovReliabilityService;
import com.taxoryn.module.gov.repository.GovIntegrationOperationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Default outbox handler for dispatching, retrying, and reconciling government integration operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultGovOperationOutboxHandler implements GovOutboxHandler {

    private final GovReliabilityService reliabilityService;
    private final GovOperationRecoveryService recoveryService;
    private final com.taxoryn.module.gov.reconciliation.service.GovernmentOperationReconciliationService reconciliationService;
    private final GovIntegrationOperationRepository operationRepository;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String eventType) {
        return GovOutboxEventType.GOV_OPERATION_DISPATCH.equalsIgnoreCase(eventType)
                || GovOutboxEventType.GOV_OPERATION_RETRY.equalsIgnoreCase(eventType)
                || GovOutboxEventType.GOV_OPERATION_RECONCILIATION.equalsIgnoreCase(eventType);
    }

    @Override
    public void handle(GovOutboxEventEntity event) {
        if (GovOutboxEventType.GOV_OPERATION_RECONCILIATION.equalsIgnoreCase(event.getEventType())) {
            if (event.getOperationId() != null) {
                reconciliationService.reconcileOperation(event.getOperationId());
            }
            return;
        }

        if (event.getOperationId() != null) {
            Optional<GovIntegrationOperationEntity> opOpt = operationRepository
                    .findByIdAndOrganizationId(event.getOperationId(), event.getOrganizationId());

            if (opOpt.isPresent()) {
                GovIntegrationOperationEntity op = opOpt.get();
                Map<String, Object> payloadMap = null;
                if (event.getPayload() != null && !event.getPayload().isBlank()) {
                    try {
                        payloadMap = objectMapper.readValue(event.getPayload(), new TypeReference<Map<String, Object>>() {});
                    } catch (Exception e) {
                        log.debug("Outbox payload is not JSON map: {}", e.getMessage());
                    }
                }

                GovIntegrationRequest request = GovIntegrationRequest.builder()
                        .organizationId(event.getOrganizationId())
                        .providerType(event.getProviderType() != null ? event.getProviderType() : op.getProviderType())
                        .operationType(op.getOperationType())
                        .businessEntityType(op.getBusinessEntityType())
                        .businessEntityId(op.getBusinessEntityId())
                        .correlationId(event.getCorrelationId())
                        .idempotencyKey(op.getIdempotencyKey())
                        .requestData(payloadMap != null ? payloadMap : new java.util.HashMap<>())
                        .build();

                GovIntegrationResult result = reliabilityService.executeWithRetry(request);
                if (!result.isSuccess() && result.getErrorCode() != null) {
                    throw new GovIntegrationException(
                            result.getErrorCode(),
                            "Outbox dispatched operation failed: " + result.getErrorMessage()
                    );
                }
            } else {
                log.warn("Operation {} referenced in outbox event {} not found in organization {}",
                        event.getOperationId(), event.getId(), event.getOrganizationId());
            }
        }
    }
}
