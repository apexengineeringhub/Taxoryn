package com.taxoryn.module.gov.exception;

import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.model.GovProviderType;
import lombok.Getter;

/**
 * Exception thrown when no registered provider adapter is available for a requested provider type.
 */
@Getter
public class GovProviderNotFoundException extends GovIntegrationException {

    private final GovProviderType providerType;

    public GovProviderNotFoundException(GovProviderType providerType) {
        super(GovErrorCode.PROVIDER_UNAVAILABLE,
                String.format("No registered government provider adapter found for provider type '%s'", providerType));
        this.providerType = providerType;
    }
}
