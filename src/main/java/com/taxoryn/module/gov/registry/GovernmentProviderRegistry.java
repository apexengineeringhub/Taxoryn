package com.taxoryn.module.gov.registry;

import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.spi.GovernmentProviderAdapter;

import java.util.List;
import java.util.Optional;

/**
 * Registry for managing and dynamically resolving registered government provider adapters.
 */
public interface GovernmentProviderRegistry {

    void registerAdapter(GovernmentProviderAdapter adapter);

    Optional<GovernmentProviderAdapter> getAdapter(GovProviderType providerType);

    Optional<GovernmentProviderAdapter> getAdapter(GovProviderType providerType, String adapterCode);

    GovernmentProviderAdapter requireAdapter(GovProviderType providerType);

    List<GovernmentProviderAdapter> getAllAdapters();

    boolean isProviderSupported(GovProviderType providerType);
}
