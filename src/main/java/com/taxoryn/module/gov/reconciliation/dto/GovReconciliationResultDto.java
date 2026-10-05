package com.taxoryn.module.gov.reconciliation.dto;

import com.taxoryn.module.gov.model.GovOperationStatus;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.reconciliation.model.GovAuthoritativeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Result DTO of an individual government operation reconciliation execution.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovReconciliationResultDto {

    private UUID operationId;
    private UUID organizationId;
    private GovProviderType providerType;
    private String operationType;
    private String correlationId;
    private GovOperationStatus previousStatus;
    private GovOperationStatus newStatus;
    private GovAuthoritativeStatus authoritativeStatus;
    private String providerReferenceId;
    private int reconciliationAttemptCount;
    private boolean changed;
    private String errorCode;
    private String errorMessage;
    private Instant reconciledAt;
}
