package com.taxoryn.module.gov;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.exception.GovProviderNotFoundException;
import com.taxoryn.module.gov.infrastructure.provider.MockProviderAdapter;
import com.taxoryn.module.gov.model.GovProviderHealth;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.repository.GovIntegrationOperationRepository;
import com.taxoryn.module.gov.service.GovernmentIntegrationServiceImpl;
import com.taxoryn.module.gov.spi.GovernmentProviderAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class GovProviderAdapterRegistrationTest {

    @Mock
    private GovIntegrationOperationRepository operationRepository;

    @Mock
    private AuditService auditService;

    private GovernmentIntegrationServiceImpl service;
    private MockProviderAdapter mockAdapter;

    @BeforeEach
    void setUp() {
        mockAdapter = new MockProviderAdapter();
        service = new GovernmentIntegrationServiceImpl(
                operationRepository,
                auditService,
                new ObjectMapper(),
                List.of(mockAdapter)
        );
    }

    @Test
    @DisplayName("Should register and discover default MockProviderAdapter")
    void testDiscoverMockAdapter() {
        List<String> adapters = service.getAvailableAdapters();
        assertThat(adapters).contains(MockProviderAdapter.ADAPTER_CODE);

        Optional<GovernmentProviderAdapter> adapter = service.getAdapter(GovProviderType.GST);
        assertThat(adapter).isPresent();
        assertThat(adapter.get().getAdapterCode()).isEqualTo(MockProviderAdapter.ADAPTER_CODE);
    }

    @Test
    @DisplayName("Should dynamically register custom provider adapter")
    void testDynamicAdapterRegistration() {
        GovernmentProviderAdapter incomeTaxAdapter = new GovernmentProviderAdapter() {
            @Override
            public GovProviderType getProviderType() {
                return GovProviderType.INCOME_TAX;
            }

            @Override
            public String getAdapterCode() {
                return "CUSTOM_ITD_ADAPTER";
            }

            @Override
            public GovIntegrationResult execute(GovIntegrationRequest request) {
                return GovIntegrationResult.success(null, request.getOrganizationId(), request.getProviderType(),
                        request.getOperationType(), request.getCorrelationId(), request.getIdempotencyKey(), "ITD-12345", Collections.emptyMap());
            }

            @Override
            public GovProviderHealth checkHealth() {
                return GovProviderHealth.up(GovProviderType.INCOME_TAX, "CUSTOM_ITD_ADAPTER", "Healthy");
            }
        };

        service.registerAdapter(incomeTaxAdapter);

        assertThat(service.getAvailableAdapters()).contains("CUSTOM_ITD_ADAPTER");
        Optional<GovernmentProviderAdapter> resolved = service.getAdapter(GovProviderType.INCOME_TAX);
        assertThat(resolved).isPresent();
        assertThat(resolved.get().getAdapterCode()).isEqualTo("CUSTOM_ITD_ADAPTER");
    }

    @Test
    @DisplayName("Should return empty optional when no adapter registered for provider type and mock fallback is absent")
    void testAdapterNotFoundWhenNoMockPresent() {
        GovernmentIntegrationServiceImpl emptyService = new GovernmentIntegrationServiceImpl(
                operationRepository,
                auditService,
                new ObjectMapper(),
                Collections.emptyList()
        );

        Optional<GovernmentProviderAdapter> resolved = emptyService.getAdapter(GovProviderType.TRACES);
        assertThat(resolved).isEmpty();
    }
}
