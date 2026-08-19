package com.taxoryn.module.billing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.billing.dto.CreateInvoiceItemRequest;
import com.taxoryn.module.billing.dto.CreateInvoiceRequest;
import com.taxoryn.module.billing.dto.RecordPaymentRequest;
import com.taxoryn.module.billing.dto.UpdateInvoiceRequest;
import com.taxoryn.module.billing.entity.InvoiceEntity;
import com.taxoryn.module.billing.entity.InvoiceItemEntity.BillingServiceType;
import com.taxoryn.module.billing.entity.InvoicePaymentEntity.PaymentMethod;
import com.taxoryn.module.billing.repository.InvoicePaymentRepository;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InvoiceManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoicePaymentRepository invoicePaymentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity tenantA;
    private OrganizationEntity tenantB;
    private ClientEntity clientA;
    private String adminTokenA;
    private String adminTokenB;
    private String clientTokenA;

    @BeforeEach
    void setUp() {
        invoicePaymentRepository.deleteAll();
        invoiceRepository.deleteAll();
        userRepository.deleteAll();
        clientRepository.deleteAll();
        organizationRepository.deleteAll();

        // 1. Create Tenant A
        tenantA = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex CA Firm")
                .email("apex-ca-" + UUID.randomUUID() + "@apextax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 2. Create Tenant B
        tenantB = organizationRepository.save(OrganizationEntity.builder()
                .name("Competitor Tax Practice")
                .email("competitor-" + UUID.randomUUID() + "@competitor.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 3. Create Client under Tenant A
        TenantContext.setTenantId(tenantA.getId());
        clientA = ClientEntity.builder()
                .displayName("ABC Traders")
                .legalName("ABC Traders Pvt Ltd")
                .clientType(ClientType.PRIVATE_LIMITED)
                .pan("AAACB1234D")
                .gstin("27AAACB1234D1Z5")
                .email("billing@abctraders.com")
                .status(ClientStatus.ACTIVE)
                .build();
        clientA.setOrganizationId(tenantA.getId());
        clientA = clientRepository.save(clientA);

        // 4. Create Org Admin A
        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().code("ORG_ADMIN").name("Org Admin").isSystemRole(true).build()));

        UserEntity adminA = UserEntity.builder()
                .email("admin@apexca.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Rajesh")
                .lastName("Sharma")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build();
        adminA.setOrganizationId(tenantA.getId());
        adminA = userRepository.save(adminA);

        adminTokenA = jwtTokenProvider.generateAccessToken(
                adminA.getId(), tenantA.getId(), null, adminA.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("BILLING_CREATE", "BILLING_VIEW", "BILLING_UPDATE", "BILLING_DELETE")
        );

        // 5. Create Org Admin B
        TenantContext.setTenantId(tenantB.getId());
        UserEntity adminB = UserEntity.builder()
                .email("admin@competitor.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Vikram")
                .lastName("Mehta")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build();
        adminB.setOrganizationId(tenantB.getId());
        adminB = userRepository.save(adminB);

        adminTokenB = jwtTokenProvider.generateAccessToken(
                adminB.getId(), tenantB.getId(), null, adminB.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("BILLING_CREATE", "BILLING_VIEW", "BILLING_UPDATE", "BILLING_DELETE")
        );

        // 6. Create Client User A
        TenantContext.setTenantId(tenantA.getId());
        RoleEntity clientAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("CLIENT_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().code("CLIENT_ADMIN").name("Client Admin").isSystemRole(true).build()));

        UserEntity clientUserA = UserEntity.builder()
                .email("finance@abctraders.com")
                .passwordHash(passwordEncoder.encode("ClientPass123!"))
                .firstName("Rohan")
                .lastName("Verma")
                .clientId(clientA.getId())
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(clientAdminRole)))
                .build();
        clientUserA.setOrganizationId(tenantA.getId());
        clientUserA = userRepository.save(clientUserA);

        clientTokenA = jwtTokenProvider.generateAccessToken(
                clientUserA.getId(), tenantA.getId(), clientA.getId(), clientUserA.getEmail(),
                Set.of("CLIENT_ADMIN"),
                Set.of("CLIENT_PORTAL_ACCESS", "CLIENT_PORTAL_STATUS_VIEW", "CLIENT_PORTAL_DOCUMENT_VIEW")
        );

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Complete Billing Lifecycle: Create Invoice -> Issue -> Partial Payment -> Full Payment -> History")
    void testCompleteBillingLifecycle() throws Exception {
        // 1. Create Invoice with multiple line items (GST & ITR)
        CreateInvoiceRequest createReq = CreateInvoiceRequest.builder()
                .clientId(clientA.getId())
                .invoiceDate(LocalDate.of(2026, 8, 20))
                .dueDate(LocalDate.of(2026, 9, 5))
                .items(List.of(
                        CreateInvoiceItemRequest.builder()
                                .service(BillingServiceType.GST_FILING)
                                .description("GSTR-1 & GSTR-3B Filing August 2026")
                                .quantity(new BigDecimal("1.00"))
                                .unitPrice(new BigDecimal("3000.00"))
                                .taxRate(new BigDecimal("18.00"))
                                .build(),
                        CreateInvoiceItemRequest.builder()
                                .service(BillingServiceType.ITR_FILING)
                                .description("Corporate Income Tax Audit & Filing")
                                .quantity(new BigDecimal("1.00"))
                                .unitPrice(new BigDecimal("7000.00"))
                                .taxRate(new BigDecimal("18.00"))
                                .build()
                ))
                .notes("Payment terms: 15 days")
                .terms("Bank Transfer: Apex CA Firm, HDFC Bank")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.subtotal").value(10000.00))
                .andExpect(jsonPath("$.data.tax").value(1800.00))
                .andExpect(jsonPath("$.data.total").value(11800.00))
                .andExpect(jsonPath("$.data.balanceDue").value(11800.00))
                .andExpect(jsonPath("$.data.paidAmount").value(0.00))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();

        String invoiceIdStr = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asText();
        UUID invoiceId = UUID.fromString(invoiceIdStr);

        // 2. Issue Invoice
        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/issue")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ISSUED"));

        // 3. Client Portal user views the invoice
        mockMvc.perform(get("/api/v1/portal/invoices")
                        .header("Authorization", "Bearer " + clientTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(invoiceIdStr))
                .andExpect(jsonPath("$.data[0].total").value(11800.00))
                .andExpect(jsonPath("$.data[0].status").value("ISSUED"));

        // 4. Record Partial Payment (₹5,000 via UPI)
        RecordPaymentRequest partialPayment = RecordPaymentRequest.builder()
                .amount(new BigDecimal("5000.00"))
                .paymentDate(LocalDate.of(2026, 8, 22))
                .paymentMethod(PaymentMethod.UPI)
                .referenceNumber("UPI-88776655")
                .notes("Advance partial payment")
                .build();

        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/payments")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialPayment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount").value(5000.00))
                .andExpect(jsonPath("$.data.paymentMethod").value("UPI"));

        // Check invoice after partial payment
        mockMvc.perform(get("/api/v1/invoices/" + invoiceId)
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paidAmount").value(5000.00))
                .andExpect(jsonPath("$.data.balanceDue").value(6800.00))
                .andExpect(jsonPath("$.data.status").value("PARTIALLY_PAID"));

        // 5. Record Remaining Payment (₹6,800 via Bank Transfer)
        RecordPaymentRequest finalPayment = RecordPaymentRequest.builder()
                .amount(new BigDecimal("6800.00"))
                .paymentDate(LocalDate.of(2026, 8, 25))
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .referenceNumber("NEFT-99881122")
                .notes("Balance payment settled")
                .build();

        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/payments")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(finalPayment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount").value(6800.00));

        // Check invoice after full payment
        mockMvc.perform(get("/api/v1/invoices/" + invoiceId)
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paidAmount").value(11800.00))
                .andExpect(jsonPath("$.data.balanceDue").value(0.00))
                .andExpect(jsonPath("$.data.status").value("PAID"));

        // 6. Client Billing History & Outstanding Summary
        mockMvc.perform(get("/api/v1/billing/clients/" + clientA.getId() + "/history")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBilled").value(11800.00))
                .andExpect(jsonPath("$.data.totalPaid").value(11800.00))
                .andExpect(jsonPath("$.data.totalOutstanding").value(0.00))
                .andExpect(jsonPath("$.data.totalInvoicesCount").value(1))
                .andExpect(jsonPath("$.data.paidInvoicesCount").value(1))
                .andExpect(jsonPath("$.data.overdueInvoicesCount").value(0));

        // 7. Practice Dashboard Stats
        mockMvc.perform(get("/api/v1/billing/dashboard/stats")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBilled").value(11800.00))
                .andExpect(jsonPath("$.data.totalCollected").value(11800.00))
                .andExpect(jsonPath("$.data.totalOutstanding").value(0.00))
                .andExpect(jsonPath("$.data.paidInvoices").value(1));
    }

    @Test
    @DisplayName("Cross-Tenant Isolation: Tenant B cannot access or record payments on Tenant A invoices (404 NOT FOUND)")
    void testCrossTenantIsolation() throws Exception {
        // 1. Create Invoice in Tenant A
        TenantContext.setTenantId(tenantA.getId());
        InvoiceEntity invoiceA = InvoiceEntity.builder()
                .clientId(clientA.getId())
                .invoiceNumber("INV-2026-9999")
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(10))
                .total(new BigDecimal("10000.00"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("10000.00"))
                .status(InvoiceEntity.InvoiceStatus.ISSUED)
                .build();
        invoiceA.setOrganizationId(tenantA.getId());
        invoiceA = invoiceRepository.save(invoiceA);
        TenantContext.clear();

        // 2. Tenant B attempts to fetch Tenant A's invoice -> 404 NOT FOUND
        mockMvc.perform(get("/api/v1/invoices/" + invoiceA.getId())
                        .header("Authorization", "Bearer " + adminTokenB))
                .andExpect(status().isNotFound());

        // 3. Tenant B attempts to record payment on Tenant A's invoice -> 404 NOT FOUND
        RecordPaymentRequest payment = RecordPaymentRequest.builder()
                .amount(new BigDecimal("5000.00"))
                .paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.CASH)
                .build();

        mockMvc.perform(post("/api/v1/invoices/" + invoiceA.getId() + "/payments")
                        .header("Authorization", "Bearer " + adminTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isNotFound());
    }
}
