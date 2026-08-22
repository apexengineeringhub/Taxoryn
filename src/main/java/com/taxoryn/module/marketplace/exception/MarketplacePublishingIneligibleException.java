package com.taxoryn.module.marketplace.exception;

import com.taxoryn.core.exception.BusinessValidationException;

import java.util.List;

/**
 * Thrown when a practice profile does not meet the minimum required completeness criteria to be published as PUBLIC.
 */
public class MarketplacePublishingIneligibleException extends BusinessValidationException {

    public MarketplacePublishingIneligibleException(String message) {
        super(message);
    }

    public MarketplacePublishingIneligibleException(List<String> missingRequirements) {
        super("Cannot publish profile to Marketplace. Minimum required fields: " + String.join(", ", missingRequirements));
    }
}
