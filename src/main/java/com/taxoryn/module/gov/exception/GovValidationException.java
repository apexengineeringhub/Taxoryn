package com.taxoryn.module.gov.exception;

import com.taxoryn.module.gov.model.GovErrorCode;

/**
 * Exception thrown when a government integration request fails contract validation.
 */
public class GovValidationException extends GovIntegrationException {

    public GovValidationException(String message) {
        super(GovErrorCode.VALIDATION_FAILED, message);
    }
}
