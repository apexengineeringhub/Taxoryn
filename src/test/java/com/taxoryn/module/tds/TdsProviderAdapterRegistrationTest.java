package com.taxoryn.module.tds;

import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.registry.GovernmentProviderRegistry;
import com.taxoryn.module.gov.spi.GovernmentProviderAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@ActiveProfiles("test")
public class TdsProviderAdapterRegistrationTest {

    @Autowired
    private GovernmentProviderRegistry providerRegistry;

    @Test
    @DisplayName("GovernmentProviderRegistry must auto-discover and resolve the TdsProviderAdapter")
    void testTdsProviderAdapterDiscoveryAndResolution() {
        Optional<GovernmentProviderAdapter> adapterOpt = providerRegistry.getAdapter(GovProviderType.TDS);

        assertThat(adapterOpt).isPresent();
        GovernmentProviderAdapter adapter = adapterOpt.get();
        assertThat(adapter.getProviderType()).isEqualTo(GovProviderType.TDS);
        assertThat(adapter.getAdapterCode()).isEqualTo("TDS_MOCK_ADAPTER");
        assertThat(providerRegistry.isProviderSupported(GovProviderType.TDS)).isTrue();
    }

    @Test
    @DisplayName("TdsProviderAdapter health check must return UP with healthy message")
    void testTdsProviderAdapterHealth() {
        GovernmentProviderAdapter adapter = providerRegistry.requireAdapter(GovProviderType.TDS);
        assertThat(adapter.checkHealth()).isNotNull();
        assertThat(adapter.checkHealth().getStatus()).isEqualTo(com.taxoryn.module.gov.model.GovProviderHealth.Status.UP);
        assertThat(adapter.checkHealth().getMessage()).contains("operational");
    }
}
