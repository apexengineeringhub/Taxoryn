package com.taxoryn.module.moduleconfig.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.TenantAccessDeniedException;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.moduleconfig.dto.OrganizationModuleDto;
import com.taxoryn.module.moduleconfig.dto.ProductModuleDto;
import com.taxoryn.module.moduleconfig.entity.OrganizationModuleEntity;
import com.taxoryn.module.moduleconfig.entity.ProductModuleEntity;
import com.taxoryn.module.moduleconfig.model.ProductModuleCategory;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.repository.OrganizationModuleRepository;
import com.taxoryn.module.moduleconfig.repository.ProductModuleRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModuleConfigurationServiceTest {

    @Mock
    private ProductModuleRepository productModuleRepository;

    @Mock
    private OrganizationModuleRepository organizationModuleRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ModuleConfigurationServiceImpl moduleConfigurationService;

    private final UUID testOrgId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID foreignOrgId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID testUserId = UUID.fromString("aaaaaaaa-0000-0000-0000-111111111111");

    private ProductModuleEntity clientsModule;
    private ProductModuleEntity tdsModule;
    private ProductModuleEntity marketplaceModule;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(testOrgId);
        SecurityUser principal = SecurityUser.builder()
                .userId(testUserId)
                .organizationId(testOrgId)
                .email("admin@taxoryn.com")
                .roles(Set.of("ROLE_ORG_ADMIN"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        clientsModule = ProductModuleEntity.builder()
                .code(ProductModuleCode.CLIENTS)
                .name("Client Management")
                .category(ProductModuleCategory.CORE)
                .enabledByDefault(true)
                .displayOrder(1)
                .build();
        clientsModule.setId(UUID.randomUUID());

        tdsModule = ProductModuleEntity.builder()
                .code(ProductModuleCode.TDS)
                .name("TDS Compliance")
                .category(ProductModuleCategory.TAX)
                .enabledByDefault(true)
                .displayOrder(10)
                .build();
        tdsModule.setId(UUID.randomUUID());

        marketplaceModule = ProductModuleEntity.builder()
                .code(ProductModuleCode.MARKETPLACE)
                .name("Practice Marketplace")
                .category(ProductModuleCategory.NETWORK_GROWTH)
                .enabledByDefault(false)
                .displayOrder(14)
                .build();
        marketplaceModule.setId(UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("1. Unconfigured organization receives safe catalog default enabled status")
    void testUnconfiguredOrganizationUsesCatalogDefaults() {
        when(organizationRepository.existsById(testOrgId)).thenReturn(true);
        when(productModuleRepository.findAllByOrderByDisplayOrderAsc())
                .thenReturn(List.of(clientsModule, tdsModule, marketplaceModule));
        when(organizationModuleRepository.findByOrganizationId(testOrgId)).thenReturn(List.of());

        List<OrganizationModuleDto> modules = moduleConfigurationService.getOrganizationModules(testOrgId);

        assertNotNull(modules);
        assertEquals(3, modules.size());

        // CLIENTS is enabled by default
        assertTrue(modules.get(0).isEnabled());
        assertFalse(modules.get(0).isExplicitlyConfigured());

        // TDS is enabled by default
        assertTrue(modules.get(1).isEnabled());
        assertFalse(modules.get(1).isExplicitlyConfigured());

        // MARKETPLACE is disabled by default
        assertFalse(modules.get(2).isEnabled());
        assertFalse(modules.get(2).isExplicitlyConfigured());
    }

    @Test
    @DisplayName("2. isModuleEnabled returns true for default enabled module and false for default disabled module")
    void testIsModuleEnabledWithCatalogDefaults() {
        when(organizationModuleRepository.findByOrganizationIdAndModuleCode(testOrgId, ProductModuleCode.CLIENTS))
                .thenReturn(Optional.empty());
        when(productModuleRepository.findByCode(ProductModuleCode.CLIENTS))
                .thenReturn(Optional.of(clientsModule));

        when(organizationModuleRepository.findByOrganizationIdAndModuleCode(testOrgId, ProductModuleCode.MARKETPLACE))
                .thenReturn(Optional.empty());
        when(productModuleRepository.findByCode(ProductModuleCode.MARKETPLACE))
                .thenReturn(Optional.of(marketplaceModule));

        assertTrue(moduleConfigurationService.isModuleEnabled(testOrgId, ProductModuleCode.CLIENTS));
        assertFalse(moduleConfigurationService.isModuleEnabled(testOrgId, ProductModuleCode.MARKETPLACE));
    }

    @Test
    @DisplayName("3. Explicit organization configuration overrides default catalog behavior")
    void testExplicitConfigurationOverridesDefaults() {
        // TDS explicitly disabled for this organization
        OrganizationModuleEntity disabledTds = OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.TDS)
                .enabled(false)
                .build();
        disabledTds.setOrganizationId(testOrgId);

        when(organizationModuleRepository.findByOrganizationIdAndModuleCode(testOrgId, ProductModuleCode.TDS))
                .thenReturn(Optional.of(disabledTds));

        assertFalse(moduleConfigurationService.isModuleEnabled(testOrgId, ProductModuleCode.TDS));
    }

    @Test
    @DisplayName("4. Enabling a module creates an audit event and saves entity")
    void testUpdateModuleStatusEnablesModuleAndAudits() {
        when(organizationRepository.existsById(testOrgId)).thenReturn(true);
        when(productModuleRepository.findByCode(ProductModuleCode.MARKETPLACE))
                .thenReturn(Optional.of(marketplaceModule));
        when(organizationModuleRepository.findByOrganizationIdAndModuleCode(testOrgId, ProductModuleCode.MARKETPLACE))
                .thenReturn(Optional.empty());

        OrganizationModuleEntity saved = OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.MARKETPLACE)
                .enabled(true)
                .build();
        saved.setOrganizationId(testOrgId);
        when(organizationModuleRepository.save(any(OrganizationModuleEntity.class))).thenReturn(saved);

        OrganizationModuleDto dto = moduleConfigurationService.updateModuleStatus(
                testOrgId, ProductModuleCode.MARKETPLACE, true);

        assertNotNull(dto);
        assertEquals(ProductModuleCode.MARKETPLACE, dto.getModuleCode());
        assertTrue(dto.isEnabled());

        // Verify audit event
        verify(auditService).logEvent(
                eq(testOrgId),
                eq(testUserId),
                eq("ORGANIZATION_MODULE_ENABLED"),
                eq("ORGANIZATION_MODULE"),
                eq("MARKETPLACE"),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("5. Disabling a module creates an audit event and saves entity")
    void testUpdateModuleStatusDisablesModuleAndAudits() {
        when(organizationRepository.existsById(testOrgId)).thenReturn(true);
        when(productModuleRepository.findByCode(ProductModuleCode.TDS))
                .thenReturn(Optional.of(tdsModule));

        OrganizationModuleEntity existing = OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.TDS)
                .enabled(true)
                .build();
        existing.setOrganizationId(testOrgId);

        when(organizationModuleRepository.findByOrganizationIdAndModuleCode(testOrgId, ProductModuleCode.TDS))
                .thenReturn(Optional.of(existing));

        OrganizationModuleEntity saved = OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.TDS)
                .enabled(false)
                .build();
        saved.setOrganizationId(testOrgId);
        when(organizationModuleRepository.save(any(OrganizationModuleEntity.class))).thenReturn(saved);

        OrganizationModuleDto dto = moduleConfigurationService.updateModuleStatus(
                testOrgId, ProductModuleCode.TDS, false);

        assertNotNull(dto);
        assertEquals(ProductModuleCode.TDS, dto.getModuleCode());
        assertFalse(dto.isEnabled());

        // Verify audit event
        verify(auditService).logEvent(
                eq(testOrgId),
                eq(testUserId),
                eq("ORGANIZATION_MODULE_DISABLED"),
                eq("ORGANIZATION_MODULE"),
                eq("TDS"),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("6. Cross-tenant access is rejected with TenantAccessDeniedException")
    void testCrossTenantAccessRejected() {
        assertThrows(TenantAccessDeniedException.class, () ->
                moduleConfigurationService.getOrganizationModules(foreignOrgId));

        assertThrows(TenantAccessDeniedException.class, () ->
                moduleConfigurationService.updateModuleStatus(foreignOrgId, ProductModuleCode.CLIENTS, false));
    }

    @Test
    @DisplayName("7. Non-existent organization is rejected with ResourceNotFoundException")
    void testNonExistentOrganizationRejected() {
        when(organizationRepository.existsById(testOrgId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
                moduleConfigurationService.getOrganizationModules(testOrgId));
    }

    @Test
    @DisplayName("8. getAllProductModules returns master catalog in display order")
    void testGetAllProductModules() {
        when(productModuleRepository.findAllByOrderByDisplayOrderAsc())
                .thenReturn(List.of(clientsModule, tdsModule, marketplaceModule));

        List<ProductModuleDto> catalog = moduleConfigurationService.getAllProductModules();

        assertNotNull(catalog);
        assertEquals(3, catalog.size());
        assertEquals(ProductModuleCode.CLIENTS, catalog.get(0).getCode());
        assertEquals(ProductModuleCode.TDS, catalog.get(1).getCode());
        assertEquals(ProductModuleCode.MARKETPLACE, catalog.get(2).getCode());
    }
}
