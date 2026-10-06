package com.taxoryn.module.gov.exception;

import com.taxoryn.module.gov.model.GovConnectionStatus;
import com.taxoryn.module.gov.model.GovErrorCode;
import lombok.Getter;

/**
 * Exception thrown when an illegal state transition is attempted on a government connection.
 */
@Getter
public class GovConnectionStateTransitionException extends GovIntegrationException {

    private final GovConnectionStatus currentStatus;
    private final GovConnectionStatus targetStatus;

    public GovConnectionStateTransitionException(GovConnectionStatus currentStatus, GovConnectionStatus targetStatus) {
        super(GovErrorCode.VALIDATION_FAILED,
                String.format("Invalid government connection state transition from '%s' to '%s'", currentStatus, targetStatus));
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public GovConnectionStateTransitionException(String message) {
        super(GovErrorCode.VALIDATION_FAILED, message);
        this.currentStatus = null;
        this.targetStatus = null;
    }
}
