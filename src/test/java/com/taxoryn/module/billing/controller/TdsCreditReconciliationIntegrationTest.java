package com.taxoryn.module.billing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.billing.dto.CreateInvoiceItemRequest;
import com.taxoryn.module.billing.dto.CreateInvoiceRequest;
import com.taxoryn.module.billing.dto.TdsCreditDto;
import com.taxoryn.module.billing.entity.InvoiceEntity;
import com.taxoryn.module.billing.entity.InvoiceItemEntity.BillingServiceType;
import com.taxoryn.module.billing.entity.TdsCreditEntity.TdsCreditSourceType;
import com.taxoryn.module.billing.repository.InvoicePaymentRepository;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.billing.repository.TdsCreditMatchRepository;
import com.taxoryn.module.billing.repository.TdsCreditRepository;
import com.taxoryn.module.billing.service.InvoiceService;
import com.taxoryn.module.billing.service.TdsReconciliationService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TdsCreditReconciliationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoicePaymentRepository invoicePaymentRepository;

    @Autowired
    private TdsCreditRepository tdsCreditRepository;

    @Autowired
    private TdsCreditMatchRepository tdsCreditMatchRepository;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private TdsReconciliationService tdsReconciliationService;

    private OrganizationEntity org1;
    private OrganizationEntity org2;
    private UserEntity adminUser1;
    private UserEntity adminUser2;
    private String tokenOrg1;
    private String tokenOrg2;
    private ClientEntity client1;
    private ClientEntity client2;

    @BeforeEach
    void setUp() {
        cleanUp();

        // 1. Setup Tenant 1
        org1 = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex CA Firm")
                .legalName("Apex Chartered Accountants LLP")
                .email("admin-" + UUID.randomUUID() + "@apexca.in")
                .status(OrganizationStatus.ACTIVE)
                .pan("AAACA1234A")
                .build());

        // 2. Setup Tenant 2
        org2 = organizationRepository.save(OrganizationEntity.builder()
                .name("Beta Tax Advisors")
                .legalName("Beta Tax Advisors LLP")
                .email("admin-" + UUID.randomUUID() + "@betatax.in")
                .status(OrganizationStatus.ACTIVE)
                .pan("AAACB5678B")
                .build());

        // Roles
        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().code("ORG_ADMIN").name("Org Admin").isSystemRole(true).build()));

        // Users
        adminUser1 = UserEntity.builder()
                .email("partner-" + UUID.randomUUID() + "@apexca.in")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Partner")
                .lastName("One")
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .status(UserStatus.ACTIVE)
                .build();
        adminUser1.setOrganizationId(org1.getId());
        adminUser1 = userRepository.save(adminUser1);

        adminUser2 = UserEntity.builder()
                .email("partner-" + UUID.randomUUID() + "@betatax.in")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Partner")
                .lastName("Two")
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .status(UserStatus.ACTIVE)
                .build();
        adminUser2.setOrganizationId(org2.getId());
        adminUser2 = userRepository.save(adminUser2);

        tokenOrg1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser1.getId(), org1.getId(), null, adminUser1.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("BILLING_CREATE", "BILLING_VIEW", "BILLING_UPDATE", "BILLING_DELETE")
        );

        tokenOrg2 = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser2.getId(), org2.getId(), null, adminUser2.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("BILLING_CREATE", "BILLING_VIEW", "BILLING_UPDATE", "BILLING_DELETE")
        );

        // Setup Clients
        TenantContext.setTenantId(org1.getId());
        client1 = ClientEntity.builder()
                .displayName("Acme Technologies Pvt Ltd")
                .pan("ABCDE1234F")
                .clientType(ClientType.COMPANY)
                .status(ClientStatus.ACTIVE)
                .build();
        client1.setOrganizationId(org1.getId());
        client1 = clientRepository.save(client1);

        TenantContext.setTenantId(org2.getId());
        client2 = ClientEntity.builder()
                .displayName("Beta Corporation")
                .pan("XYZPQ9876M")
                .clientType(ClientType.COMPANY)
                .status(ClientStatus.ACTIVE)
                .build();
        client2.setOrganizationId(org2.getId());
        client2 = clientRepository.save(client2);
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        TenantContext.clear();
        tdsCreditMatchRepository.deleteAll();
        tdsCreditRepository.deleteAll();
        invoicePaymentRepository.deleteAll();
        invoiceRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("Should successfully record inward TDS credit and reconcile with invoice")
    void testRecordAndReconcileTdsCreditExactMatch() throws Exception {
        // 1. Create and Issue Invoice for ₹50,000 + 18% GST (Total ₹59,000, Subtotal ₹50,000)
        CreateInvoiceRequest invReq = CreateInvoiceRequest.builder()
                .clientId(client1.getId())
                .invoiceDate(LocalDate.of(2025, 7, 10))
                .dueDate(LocalDate.of(2025, 7, 25))
                .items(List.of(CreateInvoiceItemRequest.builder()
                        .service(BillingServiceType.CONSULTING)
                        .description("Corporate Tax Advisory Q1")
                        .quantity(BigDecimal.ONE)
                        .unitPrice(new BigDecimal("50000.00"))
                        .taxRate(new BigDecimal("18.00"))
                        .build()))
                .build();

        var invResult = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", tokenOrg1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String invIdStr = objectMapper.readTree(invResult.getResponse().getContentAsString()).path("data").path("id").asText();
        UUID invoiceId = UUID.fromString(invIdStr);

        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/issue")
                        .header("Authorization", tokenOrg1))
                .andExpect(status().isOk());

        // 2. Client logs TDS credit: Gross ₹50,000 @ 10% (Section 194J) = ₹5,000
        TdsCreditDto creditReq = TdsCreditDto.builder()
                .clientId(client1.getId())
                .deductorPan("ABCDE1234F")
                .deductorName("Acme Technologies Pvt Ltd")
                .sectionCode("194J")
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .quarter("Q2")
                .grossAmount(new BigDecimal("50000.00"))
                .tdsDeducted(new BigDecimal("5000.00"))
                .sourceType(TdsCreditSourceType.FORM_26AS_IMPORT)
                .certificateNumber("CERT-194J-001")
                .build();

        mockMvc.perform(post("/api/v1/billing/tds-reconciliation/credits")
                        .header("Authorization", tokenOrg1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creditReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.deductorPan").value("ABCDE1234F"))
                .andExpect(jsonPath("$.data.tdsDeducted").value(5000.00));

        // 3. Run reconciliation endpoint
        mockMvc.perform(post("/api/v1/billing/tds-reconciliation/run")
                        .header("Authorization", tokenOrg1)
                        .param("financialYear", "2025-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchedCount").value(1))
                .andExpect(jsonPath("$.data.discrepancyCount").value(0))
                .andExpect(jsonPath("$.data.matchedItems[0].matchStatus").value("EXACT_MATCH"))
                .andExpect(jsonPath("$.data.matchedItems[0].actualTdsDeducted").value(5000.00))
                .andExpect(jsonPath("$.data.matchedItems[0].invoiceSubtotal").value(50000.00));

        // 4. Verify distinct aggregates: Invoice balance due is NOT cleared by TDS credit recognition
        InvoiceEntity invoiceInDb = invoiceRepository.findById(invoiceId).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(invoiceInDb.getBalanceDue()).isEqualByComparingTo("59000.00");
        org.assertj.core.api.Assertions.assertThat(invoiceInDb.getStatus()).isEqualTo(InvoiceEntity.InvoiceStatus.ISSUED);
    }

    @Test
    @DisplayName("Should detect rate discrepancy when deducted TDS does not match statutory 10% rate")
    void testDetectRateDiscrepancy() throws Exception {
        // 1. Create invoice: subtotal ₹1,00,000
        CreateInvoiceRequest invReq = CreateInvoiceRequest.builder()
                .clientId(client1.getId())
                .invoiceDate(LocalDate.of(2025, 8, 15))
                .dueDate(LocalDate.of(2025, 8, 30))
                .items(List.of(CreateInvoiceItemRequest.builder()
                        .service(BillingServiceType.CONSULTING)
                        .description("GST Representation")
                        .quantity(BigDecimal.ONE)
                        .unitPrice(new BigDecimal("100000.00"))
                        .taxRate(new BigDecimal("18.00"))
                        .build()))
                .build();

        var invResult = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", tokenOrg1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String invIdStr = objectMapper.readTree(invResult.getResponse().getContentAsString()).path("data").path("id").asText();
        UUID invoiceId = UUID.fromString(invIdStr);

        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/issue")
                        .header("Authorization", tokenOrg1))
                .andExpect(status().isOk());

        // 2. Client deducted only ₹2,000 (2%) instead of standard ₹10,000 (10%) under 194J
        TdsCreditDto creditReq = TdsCreditDto.builder()
                .clientId(client1.getId())
                .deductorPan("ABCDE1234F")
                .deductorName("Acme Technologies Pvt Ltd")
                .sectionCode("194J")
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .grossAmount(new BigDecimal("100000.00"))
                .tdsDeducted(new BigDecimal("2000.00"))
                .sourceType(TdsCreditSourceType.FORM_26AS_IMPORT)
                .build();

        mockMvc.perform(post("/api/v1/billing/tds-reconciliation/credits")
                        .header("Authorization", tokenOrg1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creditReq)))
                .andExpect(status().isCreated());

        // 3. Run reconciliation and assert discrepancy
        mockMvc.perform(post("/api/v1/billing/tds-reconciliation/run")
                        .header("Authorization", tokenOrg1)
                        .param("financialYear", "2025-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchedCount").value(0))
                .andExpect(jsonPath("$.data.discrepancyCount").value(1))
                .andExpect(jsonPath("$.data.discrepancyItems[0].matchStatus").value("RATE_DISCREPANCY"))
                .andExpect(jsonPath("$.data.discrepancyItems[0].expectedTdsAmount").value(10000.00))
                .andExpect(jsonPath("$.data.discrepancyItems[0].actualTdsDeducted").value(2000.00))
                .andExpect(jsonPath("$.data.discrepancyItems[0].rateDiscrepancy").value(8000.00));
    }

    @Test
    @DisplayName("Should strictly isolate TDS credits and reconciliation matches across tenants")
    void testTenantIsolation() throws Exception {
        // Org 2 logs TDS credit
        TdsCreditDto creditOrg2 = TdsCreditDto.builder()
                .clientId(client2.getId())
                .deductorPan("XYZPQ9876M")
                .deductorName("Beta Corporation")
                .sectionCode("194J")
                .financialYear("2025-26")
                .grossAmount(new BigDecimal("75000.00"))
                .tdsDeducted(new BigDecimal("7500.00"))
                .build();

        mockMvc.perform(post("/api/v1/billing/tds-reconciliation/credits")
                        .header("Authorization", tokenOrg2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creditOrg2)))
                .andExpect(status().isCreated());

        // Org 1 queries credits -> should NOT see Org 2's credit
        mockMvc.perform(get("/api/v1/billing/tds-reconciliation/credits")
                        .header("Authorization", tokenOrg1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // Org 2 queries credits -> sees its 1 credit
        mockMvc.perform(get("/api/v1/billing/tds-reconciliation/credits")
                        .header("Authorization", tokenOrg2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].deductorPan").value("XYZPQ9876M"));
    }
}
