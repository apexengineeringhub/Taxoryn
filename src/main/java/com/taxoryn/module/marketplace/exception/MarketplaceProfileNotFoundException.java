package com.taxoryn.module.marketplace.exception;

import com.taxoryn.core.exception.ResourceNotFoundException;

import java.util.UUID;

/**
 * Thrown when a requested marketplace practice profile cannot be located.
 */
public class MarketplaceProfileNotFoundException extends ResourceNotFoundException {

    public MarketplaceProfileNotFoundException(String message) {
        super(message);
    }

    public MarketplaceProfileNotFoundException(UUID organizationId) {
        super("MarketplaceProfile", "organizationId", organizationId);
    }

    public MarketplaceProfileNotFoundException(String fieldName, Object fieldValue) {
        super("MarketplaceProfile", fieldName, fieldValue);
    }
}
