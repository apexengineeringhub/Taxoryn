package com.taxoryn.module.gov.exception;

import com.taxoryn.module.gov.model.GovErrorCode;

import java.util.UUID;

/**
 * Thrown when a requested government credential reference is not found for the tenant.
 */
public class GovCredentialNotFoundException extends GovIntegrationException {

    public GovCredentialNotFoundException(UUID credentialRefId) {
        super(GovErrorCode.VALIDATION_FAILED, "Government credential reference not found with ID: " + credentialRefId);
    }
}
