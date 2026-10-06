package com.taxoryn.module.gov.exception;

import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.model.GovOperationStatus;
import lombok.Getter;

/**
 * Exception thrown when an illegal state transition is attempted on a government operation.
 */
@Getter
public class GovStateTransitionException extends GovIntegrationException {

    private final GovOperationStatus currentStatus;
    private final GovOperationStatus targetStatus;

    public GovStateTransitionException(GovOperationStatus currentStatus, GovOperationStatus targetStatus) {
        super(GovErrorCode.VALIDATION_FAILED,
                String.format("Invalid government operation state transition from '%s' to '%s'", currentStatus, targetStatus));
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }
}
