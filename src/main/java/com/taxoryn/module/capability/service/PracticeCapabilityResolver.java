package com.taxoryn.module.capability.service;

import com.taxoryn.module.capability.model.ProductCapability;

import java.util.UUID;

public interface PracticeCapabilityResolver {

    boolean hasCapability(ProductCapability capability);

    boolean hasCapability(UUID organizationId, ProductCapability capability);

    void requireCapability(ProductCapability capability, String forbiddenMessage);

    void requireCapability(UUID organizationId, ProductCapability capability, String forbiddenMessage);

    boolean isSoloPractice();

    boolean isSoloPractice(UUID organizationId);
}
