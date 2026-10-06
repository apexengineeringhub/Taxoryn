package com.taxoryn.module.gov.registry;

import com.taxoryn.module.gov.exception.GovProviderNotFoundException;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.spi.GovernmentProviderAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise implementation of GovernmentProviderRegistry.
 * Auto-discovers all Spring beans implementing GovernmentProviderAdapter and resolves adapters dynamically.
 * Hardened for production safety: enforces explicit mock provider and integration enablement gates.
 */
@Slf4j
@Component
public class GovernmentProviderRegistryImpl implements GovernmentProviderRegistry {

    private final Map<GovProviderType, Map<String, GovernmentProviderAdapter>> registry = new ConcurrentHashMap<>();
    private final boolean integrationEnabled;
    private final boolean mockEnabled;

    @org.springframework.beans.factory.annotation.Autowired
    public GovernmentProviderRegistryImpl(
            List<GovernmentProviderAdapter> adapters,
            @Value("${taxoryn.gov.integration.enabled:true}") boolean integrationEnabled,
            @Value("${taxoryn.gov.integration.mock-enabled:true}") boolean mockEnabled) {
        this.integrationEnabled = integrationEnabled;
        this.mockEnabled = mockEnabled;
        if (adapters != null) {
            for (GovernmentProviderAdapter adapter : adapters) {
                registerAdapter(adapter);
            }
        }
    }

    public GovernmentProviderRegistryImpl(List<GovernmentProviderAdapter> adapters) {
        this(adapters, true, true);
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
        if (!integrationEnabled || providerType == null) {
            return Optional.empty();
        }

        Map<String, GovernmentProviderAdapter> adapters = registry.get(providerType);
        if (adapters != null && !adapters.isEmpty()) {
            // Prefer dedicated real provider adapter over mock
            Optional<GovernmentProviderAdapter> dedicatedReal = adapters.values().stream()
                    .filter(a -> !isMockAdapter(a))
                    .findFirst();
            if (dedicatedReal.isPresent()) {
                return dedicatedReal;
            }

            // If only mock adapter is present, only return it if mockEnabled is true
            if (mockEnabled) {
                return Optional.of(adapters.values().iterator().next());
            } else {
                log.warn("[GOV_REGISTRY_SAFETY] Mock adapter requested for provider {} but mock providers are disabled in production", providerType);
                return Optional.empty();
            }
        }

        // Generic mock fallback only permitted in development/testing mode (mockEnabled = true)
        if (mockEnabled) {
            return registry.values().stream()
                    .flatMap(map -> map.values().stream())
                    .filter(this::isMockAdapter)
                    .findFirst();
        }

        return Optional.empty();
    }

    @Override
    public Optional<GovernmentProviderAdapter> getAdapter(GovProviderType providerType, String adapterCode) {
        if (!integrationEnabled || providerType == null || adapterCode == null) {
            return Optional.empty();
        }
        Map<String, GovernmentProviderAdapter> adapters = registry.get(providerType);
        if (adapters != null) {
            GovernmentProviderAdapter adapter = adapters.get(adapterCode.toUpperCase());
            if (adapter != null) {
                if (isMockAdapter(adapter) && !mockEnabled) {
                    log.warn("[GOV_REGISTRY_SAFETY] Explicit mock adapter '{}' requested but mock providers are disabled in production", adapterCode);
                    return Optional.empty();
                }
                return Optional.of(adapter);
            }
        }

        // Fallback for mock provider only when mockEnabled = true
        if (mockEnabled && isMockCode(adapterCode)) {
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
        if (!integrationEnabled) {
            return List.of();
        }
        List<GovernmentProviderAdapter> list = new ArrayList<>();
        registry.values().forEach(map -> list.addAll(map.values()));
        if (!mockEnabled) {
            return list.stream().filter(a -> !isMockAdapter(a)).toList();
        }
        return list;
    }

    @Override
    public boolean isProviderSupported(GovProviderType providerType) {
        return getAdapter(providerType).isPresent();
    }

    private boolean isMockAdapter(GovernmentProviderAdapter adapter) {
        return adapter != null && isMockCode(adapter.getAdapterCode());
    }

    private boolean isMockCode(String code) {
        if (code == null) {
            return false;
        }
        String upper = code.toUpperCase();
        return upper.contains("MOCK") || upper.equals("SANDBOX");
    }
}
