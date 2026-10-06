package com.taxoryn.module.gov;

import com.taxoryn.module.gov.exception.GovProviderNotFoundException;
import com.taxoryn.module.gov.model.GovProviderHealth;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.registry.GovernmentProviderRegistry;
import com.taxoryn.module.gov.registry.GovernmentProviderRegistryImpl;
import com.taxoryn.module.gov.spi.GovernmentProviderAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GovProviderRegistryTest {

    private GovernmentProviderRegistry registry;
    private GovernmentProviderAdapter gstAdapter;
    private GovernmentProviderAdapter itdAdapter;

    @BeforeEach
    void setUp() {
        gstAdapter = new GovernmentProviderAdapter() {
            @Override
            public GovProviderType getProviderType() {
                return GovProviderType.GST;
            }

            @Override
            public String getAdapterCode() {
                return "CUSTOM_GST_ADAPTER";
            }

            @Override
            public com.taxoryn.module.gov.dto.GovIntegrationResult execute(com.taxoryn.module.gov.dto.GovIntegrationRequest request) {
                return null;
            }

            @Override
            public GovProviderHealth checkHealth() {
                return GovProviderHealth.up(GovProviderType.GST, "CUSTOM_GST_ADAPTER", "Healthy");
            }
        };

        itdAdapter = new GovernmentProviderAdapter() {
            @Override
            public GovProviderType getProviderType() {
                return GovProviderType.INCOME_TAX;
            }

            @Override
            public String getAdapterCode() {
                return "CUSTOM_ITD_ADAPTER";
            }

            @Override
            public com.taxoryn.module.gov.dto.GovIntegrationResult execute(com.taxoryn.module.gov.dto.GovIntegrationRequest request) {
                return null;
            }

            @Override
            public GovProviderHealth checkHealth() {
                return GovProviderHealth.up(GovProviderType.INCOME_TAX, "CUSTOM_ITD_ADAPTER", "Healthy");
            }
        };

        registry = new GovernmentProviderRegistryImpl(List.of(gstAdapter, itdAdapter));
    }

    @Test
    @DisplayName("Registry must resolve registered adapters by provider type and adapter code")
    void testResolveRegisteredAdapters() {
        Optional<GovernmentProviderAdapter> resolvedGst = registry.getAdapter(GovProviderType.GST);
        assertThat(resolvedGst).isPresent();
        assertThat(resolvedGst.get().getAdapterCode()).isEqualTo("CUSTOM_GST_ADAPTER");

        Optional<GovernmentProviderAdapter> resolvedItd = registry.getAdapter(GovProviderType.INCOME_TAX, "CUSTOM_ITD_ADAPTER");
        assertThat(resolvedItd).isPresent();
        assertThat(resolvedItd.get().getAdapterCode()).isEqualTo("CUSTOM_ITD_ADAPTER");

        assertThat(registry.isProviderSupported(GovProviderType.GST)).isTrue();
        assertThat(registry.isProviderSupported(GovProviderType.INCOME_TAX)).isTrue();
    }

    @Test
    @DisplayName("Registry must reject unknown or unregistered provider type")
    void testRejectUnknownProvider() {
        GovernmentProviderRegistry emptyRegistry = new GovernmentProviderRegistryImpl(List.of());

        assertThatThrownBy(() -> emptyRegistry.requireAdapter(GovProviderType.TRACES))
                .isInstanceOf(GovProviderNotFoundException.class);

        assertThat(emptyRegistry.isProviderSupported(GovProviderType.TRACES)).isFalse();
    }
}
