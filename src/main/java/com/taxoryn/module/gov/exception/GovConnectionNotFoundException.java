package com.taxoryn.module.gov.exception;

import com.taxoryn.module.gov.model.GovErrorCode;

import java.util.UUID;

/**
 * Thrown when a requested government connection is not found for the tenant.
 */
public class GovConnectionNotFoundException extends GovIntegrationException {

    public GovConnectionNotFoundException(UUID connectionId) {
        super(GovErrorCode.VALIDATION_FAILED, "Government connection not found with ID: " + connectionId);
    }
}
