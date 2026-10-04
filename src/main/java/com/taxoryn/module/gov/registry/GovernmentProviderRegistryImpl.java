package com.taxoryn.module.gov.registry;

import com.taxoryn.module.gov.exception.GovProviderNotFoundException;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.spi.GovernmentProviderAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise implementation of GovernmentProviderRegistry.
 * Auto-discovers all Spring beans implementing GovernmentProviderAdapter and resolves adapters dynamically.
 */
@Slf4j
@Component
public class GovernmentProviderRegistryImpl implements GovernmentProviderRegistry {

    private final Map<GovProviderType, Map<String, GovernmentProviderAdapter>> registry = new ConcurrentHashMap<>();

    public GovernmentProviderRegistryImpl(List<GovernmentProviderAdapter> adapters) {
        if (adapters != null) {
            for (GovernmentProviderAdapter adapter : adapters) {
                registerAdapter(adapter);
            }
        }
    }

    @Override
    public void registerAdapter(GovernmentProviderAdapter adapter) {
        if (adapter == null) {
            return;
        }
        registry.computeIfAbsent(adapter.getProviderType(), k -> new ConcurrentHashMap<>())
                .put(adapter.getAdapterCode().toUpperCase(), adapter);

        log.info("[GOV_REGISTRY] Registered adapter '{}' for provider type {}",
                adapter.getAdapterCode(), adapter.getProviderType());
    }

    @Override
    public Optional<GovernmentProviderAdapter> getAdapter(GovProviderType providerType) {
        if (providerType == null) {
            return Optional.empty();
        }
        Map<String, GovernmentProviderAdapter> adapters = registry.get(providerType);
        if (adapters != null && !adapters.isEmpty()) {
            // Prefer dedicated provider adapter over generic mock
            Optional<GovernmentProviderAdapter> dedicated = adapters.values().stream()
                    .filter(a -> !"MOCK_PROVIDER".equalsIgnoreCase(a.getAdapterCode()))
                    .findFirst();
            if (dedicated.isPresent()) {
                return dedicated;
            }
            return Optional.of(adapters.values().iterator().next());
        }

        // Fallback for mock testing: if MOCK_PROVIDER is available under any type, use it
        return registry.values().stream()
                .flatMap(map -> map.values().stream())
                .filter(a -> "MOCK_PROVIDER".equalsIgnoreCase(a.getAdapterCode()))
                .findFirst();
    }

    @Override
    public Optional<GovernmentProviderAdapter> getAdapter(GovProviderType providerType, String adapterCode) {
        if (providerType == null || adapterCode == null) {
            return Optional.empty();
        }
        Map<String, GovernmentProviderAdapter> adapters = registry.get(providerType);
        if (adapters != null) {
            GovernmentProviderAdapter adapter = adapters.get(adapterCode.toUpperCase());
            if (adapter != null) {
                return Optional.of(adapter);
            }
        }

        // Fallback for mock provider
        if ("MOCK_PROVIDER".equalsIgnoreCase(adapterCode)) {
            return getAdapter(providerType);
        }
        return Optional.empty();
    }

    @Override
    public GovernmentProviderAdapter requireAdapter(GovProviderType providerType) {
        return getAdapter(providerType)
                .orElseThrow(() -> new GovProviderNotFoundException(providerType));
    }

    @Override
    public List<GovernmentProviderAdapter> getAllAdapters() {
        List<GovernmentProviderAdapter> list = new ArrayList<>();
        registry.values().forEach(map -> list.addAll(map.values()));
        return list;
    }

    @Override
    public boolean isProviderSupported(GovProviderType providerType) {
        return getAdapter(providerType).isPresent();
    }
}
