package com.taxoryn.module.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.billing.entity.InvoiceEntity;
import com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus;
import com.taxoryn.module.billing.entity.InvoiceItemEntity;
import com.taxoryn.module.billing.entity.InvoiceItemEntity.BillingServiceType;
import com.taxoryn.module.billing.repository.InvoiceItemRepository;
import com.taxoryn.module.billing.repository.InvoicePaymentRepository;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.service.ModuleConfigurationService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BillingModuleEntitlementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private InvoicePaymentRepository invoicePaymentRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ModuleConfigurationService moduleConfigurationService;

    @Autowired
    private com.taxoryn.module.moduleconfig.repository.ProductModuleRepository productModuleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity organization;
    private UserEntity adminUser;
    private ClientEntity client;
    private InvoiceEntity invoice;
    private String adminToken;

    @BeforeEach
    void setUp() {
        cleanDb();

        productModuleRepository.findByCode(ProductModuleCode.BILLING).ifPresentOrElse(
                existing -> {
                    existing.setCategory(com.taxoryn.module.moduleconfig.model.ProductModuleCategory.FOUNDATION);
                    existing.setMandatory(true);
                    existing.setConfigurable(false);
                    existing.setSubscriptionControlled(false);
                    productModuleRepository.save(existing);
                },
                () -> {
                    productModuleRepository.save(com.taxoryn.module.moduleconfig.entity.ProductModuleEntity.builder()
                            .code(ProductModuleCode.BILLING)
                            .name("Billing & Invoicing")
                            .description("Professional fee invoicing, receipts, and payment tracking.")
                            .category(com.taxoryn.module.moduleconfig.model.ProductModuleCategory.FOUNDATION)
                            .mandatory(true)
                            .configurable(false)
                            .subscriptionControlled(false)
                            .status("ACTIVE")
                            .enabledByDefault(true)
                            .displayOrder(12)
                            .build());
                }
        );

        organization = organizationRepository.save(OrganizationEntity.builder()
                .name("Billing Entitlement Firm - " + UUID.randomUUID())
                .email("billing.entitlement." + UUID.randomUUID() + "@firm.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(organization.getId());

        RoleEntity adminRole = roleRepository.save(RoleEntity.builder()
                .name("Admin Role")
                .code("ORG_ADMIN")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        adminUser = userRepository.save(UserEntity.builder()
                .organizationId(organization.getId())
                .email("admin-" + UUID.randomUUID() + "@firm.com")
                .passwordHash(passwordEncoder.encode("Secure123!"))
                .firstName("Billing")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());

        client = clientRepository.save(ClientEntity.builder()
                .displayName("Entitlement Client Ltd")
                .legalName("Entitlement Client Limited")
                .pan("ENTIT9999K")
                .clientType(ClientType.COMPANY)
                .status(ClientStatus.ACTIVE)
                .build());

        invoice = InvoiceEntity.builder()
                .clientId(client.getId())
                .invoiceNumber("INV-2026-0999")
                .currency("INR")
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(15))
                .subtotal(new BigDecimal("10000.00"))
                .tax(new BigDecimal("1800.00"))
                .total(new BigDecimal("11800.00"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("11800.00"))
                .status(InvoiceStatus.ISSUED)
                .build();
        invoice.setOrganizationId(organization.getId());

        InvoiceItemEntity item = InvoiceItemEntity.builder()
                .invoice(invoice)
                .service(BillingServiceType.CONSULTING)
                .description("Professional Advisory")
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("10000.00"))
                .taxRate(new BigDecimal("18.00"))
                .tax(new BigDecimal("1800.00"))
                .amount(new BigDecimal("11800.00"))
                .build();
        invoice.setItems(List.of(item));
        invoice = invoiceRepository.save(invoice);

        Set<String> perms = Set.of("BILLING_VIEW", "BILLING_CREATE", "BILLING_UPDATE", "BILLING_WRITE", "CLIENT_VIEW", "MODULE_CONFIGURE");
        adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(adminUser.getId(), organization.getId(), adminUser.getEmail(), Set.of("ORG_ADMIN"), perms);
    }

    @AfterEach
    void tearDown() {
        cleanDb();
        TenantContext.clear();
    }

    private void cleanDb() {
        TenantContext.clear();
        invoicePaymentRepository.deleteAll();
        invoiceItemRepository.deleteAll();
        invoiceRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    private void setSecurityContext() {
        SecurityUser securityUser = SecurityUser.builder()
                .userId(adminUser.getId())
                .organizationId(organization.getId())
                .email(adminUser.getEmail())
                .roles(Set.of("ORG_ADMIN"))
                .permissions(Set.of("BILLING_VIEW", "BILLING_CREATE", "BILLING_UPDATE", "BILLING_WRITE", "CLIENT_VIEW", "MODULE_CONFIGURE"))
                .enabled(true)
                .build();
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setTenantId(organization.getId());
    }

    @Test
    @DisplayName("Module Entitlement: BILLING is a mandatory FOUNDATION module that cannot be disabled")
    void testFoundationBillingModuleCannotBeDisabled() throws Exception {
        // 1. Attempt to disable BILLING module -> must throw BusinessValidationException
        setSecurityContext();
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                moduleConfigurationService.updateModuleStatus(organization.getId(), ProductModuleCode.BILLING, false))
                .isInstanceOf(com.taxoryn.core.exception.BusinessValidationException.class)
                .hasMessageContaining("mandatory FOUNDATION module and cannot be disabled");

        // Billing endpoints remain 200 OK
        mockMvc.perform(get("/api/v1/invoices")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));

        mockMvc.perform(get("/api/v1/billing/receivables")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/billing/unbilled-time")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());

        // Client 360 returns populated billing
        mockMvc.perform(get("/api/v1/clients/" + client.getId() + "/360")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.billing").value(notNullValue()))
                .andExpect(jsonPath("$.data.billing.totalBilled").value(11800.00))
                .andExpect(jsonPath("$.data.billing.totalOutstanding").value(11800.00))
                .andExpect(jsonPath("$.data.billing.invoices.length()").value(1));
    }
}
