package com.taxoryn.module.gst;

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
public class GstProviderAdapterRegistrationTest {

    @Autowired
    private GovernmentProviderRegistry providerRegistry;

    @Test
    @DisplayName("GovernmentProviderRegistry must auto-discover and resolve the GstProviderAdapter")
    void testGstProviderAdapterDiscoveryAndResolution() {
        Optional<GovernmentProviderAdapter> adapterOpt = providerRegistry.getAdapter(GovProviderType.GST);

        assertThat(adapterOpt).isPresent();
        GovernmentProviderAdapter adapter = adapterOpt.get();
        assertThat(adapter.getProviderType()).isEqualTo(GovProviderType.GST);
        assertThat(adapter.getAdapterCode()).isIn("GSTN_MOCK_ADAPTER", "MOCK_PROVIDER");
        assertThat(providerRegistry.isProviderSupported(GovProviderType.GST)).isTrue();
    }
}
