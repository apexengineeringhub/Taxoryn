package com.taxoryn.module.moduleconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.moduleconfig.dto.EffectiveConfigurationResponse;
import com.taxoryn.module.moduleconfig.dto.OrganizationModuleDto;
import com.taxoryn.module.moduleconfig.entity.ProductModuleEntity;
import com.taxoryn.module.moduleconfig.model.ProductModuleCategory;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.repository.ProductModuleRepository;
import com.taxoryn.module.moduleconfig.service.ModuleConfigurationService;
import com.taxoryn.module.moduleconfig.service.ModuleEntitlementService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.subscription.entity.SubscriptionEntity;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionStatus;
import com.taxoryn.module.subscription.repository.SubscriptionRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ModuleClassificationAndConfigurabilityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductModuleRepository productModuleRepository;

    @Autowired
    private ModuleConfigurationService moduleConfigurationService;

    @Autowired
    private ModuleEntitlementService moduleEntitlementService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private UserEntity userA;
    private UserEntity userB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        seedProductModules();

        RoleEntity adminRole = roleRepository.findByCodeAndIsSystemRoleTrue("PRACTICE_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("PRACTICE_ADMIN")
                        .name("Practice Admin")
                        .isSystemRole(true)
                        .description("Admin")
                        .build()));

        // Create Tenant A (Enterprise Plan)
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Practice Alpha CA Firm")
                .legalName("Practice Alpha CA Firm LLP")
                .email("admin@practicealpha.com")
                .phone("9876543210")
                .build());

        subscriptionRepository.save(SubscriptionEntity.builder()
                .organizationId(orgA.getId())
                .plan(SubscriptionPlan.ENTERPRISE)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDate.now())
                .renewalDate(LocalDate.now().plusMonths(1))
                .price(BigDecimal.valueOf(9999))
                .build());

        userA = userRepository.save(UserEntity.builder()
                .email("admin@practicealpha.com")
                .firstName("Alpha")
                .lastName("Admin")
                .passwordHash("$2a$10$dummyHashAlpha")
                .organizationId(orgA.getId())
                .roles(Set.of(adminRole))
                .status(UserEntity.UserStatus.ACTIVE)
                .build());

        tokenA = jwtTokenProvider.generateAccessToken(
                userA.getId(),
                orgA.getId(),
                userA.getEmail(),
                Set.of("ROLE_PRACTICE_ADMIN", "PRACTICE_ADMIN", "ORG_ADMIN"),
                Set.of("ORGANIZATION_UPDATE", "ORGANIZATION_VIEW", "ORG_WRITE", "ORG_READ")
        );

        // Create Tenant B (Enterprise Plan)
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Practice Beta CA Firm")
                .legalName("Practice Beta CA Firm LLP")
                .email("admin@practicebeta.com")
                .phone("9876543211")
                .build());

        subscriptionRepository.save(SubscriptionEntity.builder()
                .organizationId(orgB.getId())
                .plan(SubscriptionPlan.ENTERPRISE)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDate.now())
                .renewalDate(LocalDate.now().plusMonths(1))
                .price(BigDecimal.valueOf(9999))
                .build());

        userB = userRepository.save(UserEntity.builder()
                .email("admin@practicebeta.com")
                .firstName("Beta")
                .lastName("Admin")
                .passwordHash("$2a$10$dummyHashBeta")
                .organizationId(orgB.getId())
                .roles(Set.of(adminRole))
                .status(UserEntity.UserStatus.ACTIVE)
                .build());

        tokenB = jwtTokenProvider.generateAccessToken(
                userB.getId(),
                orgB.getId(),
                userB.getEmail(),
                Set.of("ROLE_PRACTICE_ADMIN", "PRACTICE_ADMIN", "ORG_ADMIN"),
                Set.of("ORGANIZATION_UPDATE", "ORGANIZATION_VIEW", "ORG_WRITE", "ORG_READ")
        );

        setTenantSecurityContext(orgA.getId(), userA);
    }

    private void seedProductModules() {
        for (ProductModuleCode code : ProductModuleCode.values()) {
            ProductModuleCategory category;
            boolean mandatory;
            boolean configurable;
            boolean subscriptionControlled;
            boolean usageControlled = false;

            if (code == ProductModuleCode.NOTIFICATIONS || code == ProductModuleCode.AUDIT
                    || code == ProductModuleCode.ORGANIZATION || code == ProductModuleCode.USERS) {
                category = ProductModuleCategory.CORE;
                mandatory = true;
                configurable = false;
                subscriptionControlled = false;
            } else if (code == ProductModuleCode.CLIENTS || code == ProductModuleCode.TASKS
                    || code == ProductModuleCode.DOCUMENTS || code == ProductModuleCode.BILLING
                    || code == ProductModuleCode.REPORTS || code == ProductModuleCode.DASHBOARD
                    || code == ProductModuleCode.DOCUMENT_REQUESTS || code == ProductModuleCode.PRACTICE_DASHBOARD
                    || code == ProductModuleCode.BILLING_PRACTICE_OPERATIONS) {
                category = ProductModuleCategory.FOUNDATION;
                mandatory = true;
                configurable = false;
                subscriptionControlled = false;
                usageControlled = (code == ProductModuleCode.CLIENTS || code == ProductModuleCode.DOCUMENTS || code == ProductModuleCode.TASKS);
            } else if (code == ProductModuleCode.GST || code == ProductModuleCode.GST_COMPLIANCE
                    || code == ProductModuleCode.ITR || code == ProductModuleCode.ITR_COMPLIANCE
                    || code == ProductModuleCode.TDS || code == ProductModuleCode.TDS_COMPLIANCE
                    || code == ProductModuleCode.TAX_NOTICES || code == ProductModuleCode.TAX_NOTICE_MANAGEMENT) {
                category = ProductModuleCategory.BUSINESS;
                mandatory = false;
                configurable = true;
                subscriptionControlled = true;
            } else {
                category = ProductModuleCategory.OPTIONAL;
                mandatory = false;
                configurable = true;
                subscriptionControlled = true;
            }

            ProductModuleEntity entity = productModuleRepository.findByCode(code).orElse(null);
            if (entity == null) {
                entity = ProductModuleEntity.builder()
                        .code(code)
                        .name(code.name())
                        .description(code.name() + " module")
                        .category(category)
                        .mandatory(mandatory)
                        .configurable(configurable)
                        .subscriptionControlled(subscriptionControlled)
                        .usageControlled(usageControlled)
                        .status("ACTIVE")
                        .enabledByDefault(true)
                        .displayOrder(1)
                        .build();
            } else {
                entity.setCategory(category);
                entity.setMandatory(mandatory);
                entity.setConfigurable(configurable);
                entity.setSubscriptionControlled(subscriptionControlled);
                entity.setUsageControlled(usageControlled);
            }

            productModuleRepository.save(entity);
        }
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private void setTenantSecurityContext(UUID orgId, UserEntity user) {
        TenantContext.setTenantId(orgId);
        SecurityUser principal = SecurityUser.builder()
                .userId(user.getId())
                .organizationId(orgId)
                .email(user.getEmail())
                .roles(Set.of("ROLE_PRACTICE_ADMIN", "ROLE_ORG_ADMIN"))
                .permissions(Set.of("ORGANIZATION_UPDATE", "ORGANIZATION_VIEW", "ORG_WRITE", "ORG_READ"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("1. CORE modules cannot be disabled and reject disable requests")
    void testCoreModulesCannotBeDisabled() {
        setTenantSecurityContext(orgA.getId(), userA);

        // Attempting to disable AUDIT module should throw BusinessValidationException
        assertThatThrownBy(() -> moduleConfigurationService.updateModuleStatus(orgA.getId(), ProductModuleCode.AUDIT, false))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("AUDIT")
                .hasMessageContaining("mandatory CORE module and cannot be disabled");

        // Attempting to disable NOTIFICATIONS module should throw BusinessValidationException
        assertThatThrownBy(() -> moduleConfigurationService.updateModuleStatus(orgA.getId(), ProductModuleCode.NOTIFICATIONS, false))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("NOTIFICATIONS")
                .hasMessageContaining("mandatory CORE module and cannot be disabled");

        // isModuleEnabled must always return true for CORE
        assertThat(moduleConfigurationService.isModuleEnabled(orgA.getId(), ProductModuleCode.AUDIT)).isTrue();
        assertThat(moduleConfigurationService.isModuleEnabled(orgA.getId(), ProductModuleCode.NOTIFICATIONS)).isTrue();
    }

    @Test
    @DisplayName("2. FOUNDATION modules cannot be disabled and reject disable requests")
    void testFoundationModulesCannotBeDisabled() {
        setTenantSecurityContext(orgA.getId(), userA);

        // Attempting to disable CLIENTS module should throw BusinessValidationException
        assertThatThrownBy(() -> moduleConfigurationService.updateModuleStatus(orgA.getId(), ProductModuleCode.CLIENTS, false))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("CLIENTS")
                .hasMessageContaining("mandatory FOUNDATION module and cannot be disabled");

        // Attempting to disable DOCUMENTS module should throw BusinessValidationException
        assertThatThrownBy(() -> moduleConfigurationService.updateModuleStatus(orgA.getId(), ProductModuleCode.DOCUMENTS, false))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("DOCUMENTS")
                .hasMessageContaining("mandatory FOUNDATION module and cannot be disabled");

        // Attempting to disable TASKS module should throw BusinessValidationException
        assertThatThrownBy(() -> moduleConfigurationService.updateModuleStatus(orgA.getId(), ProductModuleCode.TASKS, false))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("TASKS")
                .hasMessageContaining("mandatory FOUNDATION module and cannot be disabled");

        // Attempting to disable BILLING module should throw BusinessValidationException
        assertThatThrownBy(() -> moduleConfigurationService.updateModuleStatus(orgA.getId(), ProductModuleCode.BILLING, false))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("BILLING")
                .hasMessageContaining("mandatory FOUNDATION module and cannot be disabled");

        // isModuleEnabled must always return true for FOUNDATION
        assertThat(moduleConfigurationService.isModuleEnabled(orgA.getId(), ProductModuleCode.CLIENTS)).isTrue();
        assertThat(moduleConfigurationService.isModuleEnabled(orgA.getId(), ProductModuleCode.DOCUMENTS)).isTrue();
        assertThat(moduleConfigurationService.isModuleEnabled(orgA.getId(), ProductModuleCode.TASKS)).isTrue();
        assertThat(moduleConfigurationService.isModuleEnabled(orgA.getId(), ProductModuleCode.BILLING)).isTrue();
    }

    @Test
    @DisplayName("3. BUSINESS modules can be enabled and disabled per organization")
    void testBusinessModulesConfigurable() {
        setTenantSecurityContext(orgA.getId(), userA);

        // Initially GST is enabled
        assertThat(moduleConfigurationService.isModuleEnabled(orgA.getId(), ProductModuleCode.GST)).isTrue();

        // Organization A disables GST
        OrganizationModuleDto updated = moduleConfigurationService.updateModuleStatus(orgA.getId(), ProductModuleCode.GST, false);
        assertThat(updated.isEnabled()).isFalse();
        assertThat(updated.getCategory()).isEqualTo(ProductModuleCategory.BUSINESS);
        assertThat(moduleConfigurationService.isModuleEnabled(orgA.getId(), ProductModuleCode.GST)).isFalse();

        // Organization A enables GST again
        OrganizationModuleDto reEnabled = moduleConfigurationService.updateModuleStatus(orgA.getId(), ProductModuleCode.GST, true);
        assertThat(reEnabled.isEnabled()).isTrue();
        assertThat(moduleConfigurationService.isModuleEnabled(orgA.getId(), ProductModuleCode.GST)).isTrue();
    }

    @Test
    @DisplayName("4. OPTIONAL modules can be enabled and disabled per organization")
    void testOptionalModulesConfigurable() {
        setTenantSecurityContext(orgA.getId(), userA);

        // MARKETPLACE is optional
        OrganizationModuleDto updated = moduleConfigurationService.updateModuleStatus(orgA.getId(), ProductModuleCode.MARKETPLACE, true);
        assertThat(updated.isEnabled()).isTrue();
        assertThat(updated.getCategory()).isEqualTo(ProductModuleCategory.OPTIONAL);
        assertThat(moduleConfigurationService.isModuleEnabled(orgA.getId(), ProductModuleCode.MARKETPLACE)).isTrue();

        // Disable MARKETPLACE
        OrganizationModuleDto disabled = moduleConfigurationService.updateModuleStatus(orgA.getId(), ProductModuleCode.MARKETPLACE, false);
        assertThat(disabled.isEnabled()).isFalse();
        assertThat(moduleConfigurationService.isModuleEnabled(orgA.getId(), ProductModuleCode.MARKETPLACE)).isFalse();
    }

    @Test
    @DisplayName("5. Tenant isolation: Disabling BUSINESS module for Org A does NOT affect Org B")
    void testTenantIsolationOnModuleConfiguration() {
        // Org A disables GST
        setTenantSecurityContext(orgA.getId(), userA);
        moduleConfigurationService.updateModuleStatus(orgA.getId(), ProductModuleCode.GST, false);

        // Org B disables TDS
        setTenantSecurityContext(orgB.getId(), userB);
        moduleConfigurationService.updateModuleStatus(orgB.getId(), ProductModuleCode.TDS, false);

        // Verify Org A configuration
        setTenantSecurityContext(orgA.getId(), userA);
        assertThat(moduleConfigurationService.isModuleEnabled(orgA.getId(), ProductModuleCode.GST)).isFalse();
        assertThat(moduleConfigurationService.isModuleEnabled(orgA.getId(), ProductModuleCode.TDS)).isTrue();

        // Verify Org B configuration
        setTenantSecurityContext(orgB.getId(), userB);
        assertThat(moduleConfigurationService.isModuleEnabled(orgB.getId(), ProductModuleCode.GST)).isTrue();
        assertThat(moduleConfigurationService.isModuleEnabled(orgB.getId(), ProductModuleCode.TDS)).isFalse();

        // Both retain full access to Foundation modules
        assertThat(moduleConfigurationService.isModuleEnabled(orgA.getId(), ProductModuleCode.CLIENTS)).isTrue();
        assertThat(moduleConfigurationService.isModuleEnabled(orgB.getId(), ProductModuleCode.CLIENTS)).isTrue();
        assertThat(moduleConfigurationService.isModuleEnabled(orgA.getId(), ProductModuleCode.DOCUMENTS)).isTrue();
        assertThat(moduleConfigurationService.isModuleEnabled(orgB.getId(), ProductModuleCode.DOCUMENTS)).isTrue();
    }

    @Test
    @DisplayName("6. REST API: PUT /api/v1/organizations/modules/CLIENTS with enabled=false returns 400 Bad Request")
    void testApiRejectsDisablingFoundationModule() throws Exception {
        mockMvc.perform(put("/api/v1/organizations/modules/CLIENTS")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("mandatory FOUNDATION module and cannot be disabled")));
    }

    @Test
    @DisplayName("7. REST API: GET /api/v1/organizations/modules returns 4-tier categories with metadata")
    void testApiReturnsAllCategorizedModules() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/modules")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.moduleCode == 'CLIENTS')].category").value("FOUNDATION"))
                .andExpect(jsonPath("$.data[?(@.moduleCode == 'CLIENTS')].mandatory").value(true))
                .andExpect(jsonPath("$.data[?(@.moduleCode == 'CLIENTS')].usageControlled").value(true))
                .andExpect(jsonPath("$.data[?(@.moduleCode == 'GST')].category").value("BUSINESS"))
                .andExpect(jsonPath("$.data[?(@.moduleCode == 'GST')].configurable").value(true))
                .andExpect(jsonPath("$.data[?(@.moduleCode == 'AUDIT')].category").value("CORE"))
                .andExpect(jsonPath("$.data[?(@.moduleCode == 'MARKETPLACE')].category").value("OPTIONAL"));
    }

    @Test
    @DisplayName("8. Effective Configuration API returns correct module states and navigation items")
    void testEffectiveConfigurationReturnsCorrectNavigation() {
        setTenantSecurityContext(orgA.getId(), userA);
        EffectiveConfigurationResponse config = moduleConfigurationService.getEffectiveConfiguration(orgA.getId());

        assertThat(config.getModules()).containsEntry("CLIENTS", true);
        assertThat(config.getModules()).containsEntry("DOCUMENTS", true);
        assertThat(config.getModules()).containsEntry("GST", true);
        assertThat(config.getModules()).containsEntry("ITR", true);
        assertThat(config.getNavigationItems()).contains("DASHBOARD", "CLIENTS", "DOCUMENTS", "GST", "ITR");
    }
}
