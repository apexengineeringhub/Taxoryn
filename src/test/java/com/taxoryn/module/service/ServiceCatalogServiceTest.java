package com.taxoryn.module.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.service.ModuleConfigurationService;
import com.taxoryn.module.service.dto.CreateServiceRequest;
import com.taxoryn.module.service.dto.ServiceDto;
import com.taxoryn.module.service.entity.ServiceEntity;
import com.taxoryn.module.service.model.ServiceCategory;
import com.taxoryn.module.service.model.ServiceStatus;
import com.taxoryn.module.service.repository.ServiceRepository;
import com.taxoryn.module.service.service.ServiceCatalogServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ServiceCatalogServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private ModuleConfigurationService moduleConfigurationService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ServiceCatalogServiceImpl serviceCatalogService;

    private UUID organizationId;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        TenantContext.setTenantId(organizationId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Get services returns catalog entries with correct entitlement availability")
    void testGetServicesWithEntitlement() {
        ServiceEntity gstService = ServiceEntity.builder()
                .serviceCode("GST_COMPLIANCE")
                .serviceName("GST Compliance & Returns")
                .category(ServiceCategory.GST)
                .status(ServiceStatus.ACTIVE)
                .moduleCode("GST")
                .build();
        gstService.setId(UUID.randomUUID());

        ServiceEntity advisoryService = ServiceEntity.builder()
                .serviceCode("TAX_ADVISORY")
                .serviceName("Tax Planning & Advisory")
                .category(ServiceCategory.ADVISORY)
                .status(ServiceStatus.ACTIVE)
                .moduleCode("CLIENTS")
                .build();
        advisoryService.setId(UUID.randomUUID());

        when(serviceRepository.findActiveServicesForTenant(organizationId)).thenReturn(List.of(gstService, advisoryService));
        when(moduleConfigurationService.isModuleEnabled(organizationId, ProductModuleCode.GST)).thenReturn(true);
        when(moduleConfigurationService.isModuleEnabled(organizationId, ProductModuleCode.CLIENTS)).thenReturn(true);

        List<ServiceDto> services = serviceCatalogService.getServices();

        assertThat(services).hasSize(2);
        assertThat(services.get(0).getServiceCode()).isEqualTo("GST_COMPLIANCE");
        assertThat(services.get(0).isAvailable()).isTrue();
        assertThat(services.get(1).getServiceCode()).isEqualTo("TAX_ADVISORY");
        assertThat(services.get(1).isAvailable()).isTrue();
    }

    @Test
    @DisplayName("Create custom practice service persists and audits")
    void testCreateCustomService() {
        CreateServiceRequest request = CreateServiceRequest.builder()
                .serviceCode("CUSTOM_TRANSFER_PRICING")
                .serviceName("Transfer Pricing Study & Form 3CEB")
                .description("Documentation and certification")
                .category(ServiceCategory.ADVISORY)
                .build();

        when(serviceRepository.existsByOrganizationIdAndServiceCode(organizationId, "CUSTOM_TRANSFER_PRICING")).thenReturn(false);
        when(serviceRepository.save(any(ServiceEntity.class))).thenAnswer(inv -> {
            ServiceEntity s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        ServiceDto created = serviceCatalogService.createService(request);

        assertThat(created).isNotNull();
        assertThat(created.getServiceCode()).isEqualTo("CUSTOM_TRANSFER_PRICING");
        assertThat(created.getServiceName()).isEqualTo("Transfer Pricing Study & Form 3CEB");
        assertThat(created.getCategory()).isEqualTo(ServiceCategory.ADVISORY);

        verify(auditService).logEvent(eq("SERVICE_CREATED"), eq("SERVICE"), any(), any(), any());
    }

    @Test
    @DisplayName("Create custom service fails when duplicate code exists for tenant")
    void testCreateCustomServiceDuplicate() {
        CreateServiceRequest request = CreateServiceRequest.builder()
                .serviceCode("DUPLICATE_CODE")
                .serviceName("Duplicate Service")
                .category(ServiceCategory.OTHER)
                .build();

        when(serviceRepository.existsByOrganizationIdAndServiceCode(organizationId, "DUPLICATE_CODE")).thenReturn(true);

        assertThatThrownBy(() -> serviceCatalogService.createService(request))
                .isInstanceOf(DuplicateResourceException.class);
    }
}
