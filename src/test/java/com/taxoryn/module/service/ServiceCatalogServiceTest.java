package com.taxoryn.module.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.service.ModuleConfigurationService;
import com.taxoryn.module.service.dto.CreateServiceRequest;
import com.taxoryn.module.service.dto.ServiceDto;
import com.taxoryn.module.service.dto.UpdateServiceRequest;
import com.taxoryn.module.service.entity.ServiceEntity;
import com.taxoryn.module.service.model.ServiceCategory;
import com.taxoryn.module.service.model.ServiceScope;
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

import java.math.BigDecimal;
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
                .scope(ServiceScope.TAXORYN)
                .moduleCode("GST")
                .build();
        gstService.setId(UUID.randomUUID());

        ServiceEntity advisoryService = ServiceEntity.builder()
                .serviceCode("TAX_ADVISORY")
                .serviceName("Tax Planning & Advisory")
                .category(ServiceCategory.ADVISORY)
                .status(ServiceStatus.ACTIVE)
                .scope(ServiceScope.TAXORYN)
                .moduleCode("CLIENTS")
                .build();
        advisoryService.setId(UUID.randomUUID());

        when(serviceRepository.findAllAccessibleServicesForTenant(organizationId)).thenReturn(List.of(gstService, advisoryService));
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
                .billingUnit("HOURLY")
                .defaultPrice(new BigDecimal("4500.00"))
                .taxRate(new BigDecimal("18.00"))
                .build();

        when(serviceRepository.existsByOrganizationIdAndServiceCodeIgnoreCase(organizationId, "CUSTOM_TRANSFER_PRICING")).thenReturn(false);
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
        assertThat(created.getScope()).isEqualTo(ServiceScope.PRACTICE);
        assertThat(created.getDefaultPrice()).isEqualByComparingTo("4500.00");
        assertThat(created.getBillingUnit()).isEqualTo("HOURLY");

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

        when(serviceRepository.existsByOrganizationIdAndServiceCodeIgnoreCase(organizationId, "DUPLICATE_CODE")).thenReturn(true);

        assertThatThrownBy(() -> serviceCatalogService.createService(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Update practice custom service modifies properties and audits")
    void testUpdateCustomService() {
        UUID serviceId = UUID.randomUUID();
        ServiceEntity existing = ServiceEntity.builder()
                .organizationId(organizationId)
                .serviceCode("PAN_APP")
                .serviceName("PAN Card Service")
                .category(ServiceCategory.GOVERNMENT_SERVICES)
                .status(ServiceStatus.ACTIVE)
                .scope(ServiceScope.PRACTICE)
                .defaultPrice(new BigDecimal("500.00"))
                .taxRate(new BigDecimal("18.00"))
                .build();
        existing.setId(serviceId);

        when(serviceRepository.findAccessibleServiceById(serviceId, organizationId)).thenReturn(Optional.of(existing));
        when(serviceRepository.save(any(ServiceEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateServiceRequest request = UpdateServiceRequest.builder()
                .serviceName("PAN Card Allotment & Correction")
                .defaultPrice(new BigDecimal("750.00"))
                .build();

        ServiceDto updated = serviceCatalogService.updateService(serviceId, request);

        assertThat(updated.getServiceName()).isEqualTo("PAN Card Allotment & Correction");
        assertThat(updated.getDefaultPrice()).isEqualByComparingTo("750.00");
        verify(auditService).logEvent(eq("SERVICE_UPDATED"), eq("SERVICE"), any(), any(), any());
    }

    @Test
    @DisplayName("Toggle service status updates status to ACTIVE / INACTIVE")
    void testToggleServiceStatus() {
        UUID serviceId = UUID.randomUUID();
        ServiceEntity existing = ServiceEntity.builder()
                .organizationId(organizationId)
                .serviceCode("FSSAI_APP")
                .serviceName("FSSAI Food License")
                .category(ServiceCategory.REGISTRATION)
                .status(ServiceStatus.ACTIVE)
                .scope(ServiceScope.PRACTICE)
                .build();
        existing.setId(serviceId);

        when(serviceRepository.findAccessibleServiceById(serviceId, organizationId)).thenReturn(Optional.of(existing));
        when(serviceRepository.save(any(ServiceEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ServiceDto result = serviceCatalogService.toggleServiceStatus(serviceId, false);
        assertThat(result.getStatus()).isEqualTo(ServiceStatus.INACTIVE);
        verify(auditService).logEvent(eq("SERVICE_DEACTIVATED"), eq("SERVICE"), any(), any(), any());
    }

    @Test
    @DisplayName("Delete custom practice service removes entity when practice owned")
    void testDeleteCustomService() {
        UUID serviceId = UUID.randomUUID();
        ServiceEntity existing = ServiceEntity.builder()
                .organizationId(organizationId)
                .serviceCode("CUSTOM_TEMP")
                .serviceName("Temporary Custom Service")
                .category(ServiceCategory.OTHER)
                .status(ServiceStatus.ACTIVE)
                .scope(ServiceScope.PRACTICE)
                .build();
        existing.setId(serviceId);

        when(serviceRepository.findAccessibleServiceById(serviceId, organizationId)).thenReturn(Optional.of(existing));

        serviceCatalogService.deleteService(serviceId);

        verify(serviceRepository).delete(existing);
        verify(auditService).logEvent(eq("SERVICE_DELETED"), eq("SERVICE"), any(), any(), any());
    }

    @Test
    @DisplayName("Delete standard Taxoryn service throws BusinessValidationException")
    void testDeleteStandardServiceThrowsError() {
        UUID serviceId = UUID.randomUUID();
        ServiceEntity standardService = ServiceEntity.builder()
                .organizationId(null)
                .serviceCode("GST_COMPLIANCE")
                .serviceName("GST Compliance")
                .category(ServiceCategory.GST)
                .scope(ServiceScope.TAXORYN)
                .build();
        standardService.setId(serviceId);

        when(serviceRepository.findAccessibleServiceById(serviceId, organizationId)).thenReturn(Optional.of(standardService));

        assertThatThrownBy(() -> serviceCatalogService.deleteService(serviceId))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Cannot delete platform default Taxoryn service");
    }
}
