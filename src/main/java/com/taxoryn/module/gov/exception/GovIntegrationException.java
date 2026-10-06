package com.taxoryn.module.gov.exception;

import com.taxoryn.module.gov.model.GovErrorCode;
import lombok.Getter;

/**
 * Base unchecked exception for government integration framework errors.
 */
@Getter
public class GovIntegrationException extends RuntimeException {

    private final GovErrorCode errorCode;

    public GovIntegrationException(GovErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public GovIntegrationException(GovErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
