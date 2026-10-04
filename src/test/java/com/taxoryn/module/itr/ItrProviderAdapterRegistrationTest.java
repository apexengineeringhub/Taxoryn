package com.taxoryn.module.itr;

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
public class ItrProviderAdapterRegistrationTest {

    @Autowired
    private GovernmentProviderRegistry providerRegistry;

    @Test
    @DisplayName("GovernmentProviderRegistry must auto-discover and resolve the ItrProviderAdapter")
    void testItrProviderAdapterDiscoveryAndResolution() {
        Optional<GovernmentProviderAdapter> adapterOpt = providerRegistry.getAdapter(GovProviderType.INCOME_TAX);

        assertThat(adapterOpt).isPresent();
        GovernmentProviderAdapter adapter = adapterOpt.get();
        assertThat(adapter.getProviderType()).isEqualTo(GovProviderType.INCOME_TAX);
        assertThat(adapter.getAdapterCode()).isEqualTo("ITR_MOCK_ADAPTER");
        assertThat(providerRegistry.isProviderSupported(GovProviderType.INCOME_TAX)).isTrue();
    }
}
