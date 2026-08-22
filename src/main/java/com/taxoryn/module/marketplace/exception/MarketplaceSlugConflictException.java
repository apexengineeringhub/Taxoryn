package com.taxoryn.module.marketplace.exception;

import com.taxoryn.core.exception.DuplicateResourceException;

/**
 * Thrown when a specified public slug is already claimed by another practice.
 */
public class MarketplaceSlugConflictException extends DuplicateResourceException {

    public MarketplaceSlugConflictException(String slug) {
        super(String.format("The public slug '%s' is already taken. Please choose another unique vanity slug.", slug));
    }
}
