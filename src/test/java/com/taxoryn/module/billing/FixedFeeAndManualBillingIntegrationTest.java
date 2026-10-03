package com.taxoryn.module.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.billing.dto.CreateInvoiceItemRequest;
import com.taxoryn.module.billing.dto.CreateInvoiceRequest;
import com.taxoryn.module.billing.dto.RecordPaymentRequest;
import com.taxoryn.module.billing.dto.UpdateInvoiceRequest;
import com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus;
import com.taxoryn.module.billing.entity.InvoiceItemEntity.BillingServiceType;
import com.taxoryn.module.billing.entity.InvoicePaymentEntity.PaymentMethod;
import com.taxoryn.module.billing.repository.InvoiceItemRepository;
import com.taxoryn.module.billing.repository.InvoicePaymentRepository;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.engagement.entity.EngagementEntity;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.LocationRepository;
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

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FixedFeeAndManualBillingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EngagementRepository engagementRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private InvoicePaymentRepository invoicePaymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity org;
    private LocationEntity loc;
    private UserEntity user;
    private ClientEntity client;
    private EngagementEntity engagement;
    private String token;

    @BeforeEach
    void setUp() {
        cleanUp();

        RoleEntity adminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Organization Administrator")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        org = organizationRepository.save(OrganizationEntity.builder()
                .name("Kedia & Associates - " + UUID.randomUUID())
                .legalName("Kedia & Associates LLP")
                .email("billing.fixed." + UUID.randomUUID() + "@firm.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        LocationEntity l = LocationEntity.builder()
                .name("Bengaluru Office")
                .code("BLR-" + UUID.randomUUID().toString().substring(0, 4))
                .city("Bengaluru")
                .state("Karnataka")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        l.setOrganizationId(org.getId());
        loc = locationRepository.save(l);

        user = userRepository.save(UserEntity.builder()
                .organizationId(org.getId())
                .email("kedia." + UUID.randomUUID() + "@kedia.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Suresh")
                .lastName("Kedia")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        token = jwtTokenProvider.generateAccessToken(
                user.getId(),
                org.getId(),
                user.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "BILLING_VIEW", "BILLING_CREATE", "BILLING_UPDATE", "BILLING_WRITE", "CLIENT_VIEW")
        );

        ClientEntity c = ClientEntity.builder()
                .displayName("Infosys BPO Ltd")
                .legalName("Infosys BPO Limited")
                .clientType(ClientType.COMPANY)
                .pan("AABCI5555I")
                .gstin("29AABCI5555I1Z3")
                .locationId(loc.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        c.setOrganizationId(org.getId());
        client = clientRepository.save(c);

        EngagementEntity eng = EngagementEntity.builder()
                .locationId(loc.getId())
                .clientId(client.getId())
                .name("Fixed Fee Statutory Audit FY26")
                .build();
        eng.setOrganizationId(org.getId());
        engagement = engagementRepository.save(eng);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    private void cleanUp() {
        invoicePaymentRepository.deleteAll();
        invoiceItemRepository.deleteAll();
        invoiceRepository.deleteAll();
        engagementRepository.deleteAll();
        clientRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("Fixed-fee manual invoice with discount, item modification restrictions, payment and cancellation rules")
    void testFixedFeeAndManualBillingLifecycle() throws Exception {
        // 1. Create fixed fee invoice with 2 items and a discount
        // Item 1: 50,000 + 18% (9,000) = 59,000
        // Item 2: 10,000 + 18% (1,800) = 11,800
        // Subtotal = 60,000; Tax = 10,800; Discount = 5,000 -> Total = 65,800
        CreateInvoiceItemRequest item1 = CreateInvoiceItemRequest.builder()
                .service(BillingServiceType.AUDIT)
                .description("Statutory Audit & Tax Audit Reporting for FY 2025-26")
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("50000.00"))
                .taxRate(new BigDecimal("18.00"))
                .build();

        CreateInvoiceItemRequest item2 = CreateInvoiceItemRequest.builder()
                .service(BillingServiceType.ROC_COMPLIANCE)
                .description("Annual ROC filing (MGT-7 & AOC-4)")
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("10000.00"))
                .taxRate(new BigDecimal("18.00"))
                .build();

        CreateInvoiceRequest req = CreateInvoiceRequest.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .engagementId(engagement.getId())
                .invoiceDate(LocalDate.of(2026, 9, 1))
                .dueDate(LocalDate.of(2026, 9, 20))
                .discount(new BigDecimal("5000.00"))
                .items(List.of(item1, item2))
                .notes("Fixed retainer audit billing")
                .terms("Payment due in 20 days")
                .build();

        String createResp = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.invoiceNumber", startsWith("INV-")))
                .andExpect(jsonPath("$.data.subtotal").value(60000.00))
                .andExpect(jsonPath("$.data.tax").value(10800.00))
                .andExpect(jsonPath("$.data.discount").value(5000.00))
                .andExpect(jsonPath("$.data.total").value(65800.00))
                .andExpect(jsonPath("$.data.balanceDue").value(65800.00))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        UUID invoiceId = UUID.fromString(objectMapper.readTree(createResp).path("data").path("id").asText());

        // 2. Issue the invoice
        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/issue")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ISSUED"));

        // 3. Attempt to modify items on ISSUED invoice -> Must be rejected (400 Bad Request)
        UpdateInvoiceRequest updateReq = UpdateInvoiceRequest.builder()
                .items(List.of(CreateInvoiceItemRequest.builder()
                        .service(BillingServiceType.CONSULTING)
                        .quantity(BigDecimal.ONE)
                        .unitPrice(new BigDecimal("1000.00"))
                        .build()))
                .build();

        mockMvc.perform(put("/api/v1/invoices/" + invoiceId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest());

        // 4. Record Full Payment (₹65,800)
        RecordPaymentRequest paymentReq = RecordPaymentRequest.builder()
                .amount(new BigDecimal("65800.00"))
                .paymentDate(LocalDate.of(2026, 9, 10))
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .referenceNumber("NEFT99887766")
                .notes("Full invoice payment")
                .build();

        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.receiptNumber", startsWith("REC-")))
                .andExpect(jsonPath("$.data.amount").value(65800.00));

        // Verify invoice is PAID
        mockMvc.perform(get("/api/v1/invoices/" + invoiceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.balanceDue").value(0.00));

        // 5. Attempt to cancel fully paid invoice -> Must fail (400 Bad Request)
        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        // 6. Query Client Billing History
        mockMvc.perform(get("/api/v1/billing/clients/" + client.getId() + "/billing")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalBilled").value(65800.00))
                .andExpect(jsonPath("$.data.totalPaid").value(65800.00))
                .andExpect(jsonPath("$.data.totalOutstanding").value(0.00))
                .andExpect(jsonPath("$.data.paidInvoicesCount").value(1))
                .andExpect(jsonPath("$.data.invoices.length()").value(1))
                .andExpect(jsonPath("$.data.recentPayments.length()").value(1));
    }

    @Test
    @DisplayName("Verify complete GST rate lifecycle (0%, 5%, 12%, 18%) - Save, Retrieve, and Reopen")
    void testInvoiceGstRatePersistenceAndRetrieval_ZeroFiveTwelveEighteen() throws Exception {
        BigDecimal[][] testCases = {
                // {taxRate, unitPrice, expectedTax, expectedTotal}
                {new BigDecimal("0.00"), new BigDecimal("1000.00"), new BigDecimal("0.00"), new BigDecimal("1000.00")},
                {new BigDecimal("5.00"), new BigDecimal("1000.00"), new BigDecimal("50.00"), new BigDecimal("1050.00")},
                {new BigDecimal("12.00"), new BigDecimal("1000.00"), new BigDecimal("120.00"), new BigDecimal("1120.00")},
                {new BigDecimal("18.00"), new BigDecimal("1000.00"), new BigDecimal("180.00"), new BigDecimal("1180.00")}
        };

        for (BigDecimal[] tc : testCases) {
            BigDecimal taxRate = tc[0];
            BigDecimal unitPrice = tc[1];
            BigDecimal expectedTax = tc[2];
            BigDecimal expectedTotal = tc[3];

            CreateInvoiceItemRequest item = CreateInvoiceItemRequest.builder()
                    .service(BillingServiceType.CONSULTING)
                    .description("Consulting Service @ " + taxRate + "% GST")
                    .quantity(BigDecimal.ONE)
                    .unitPrice(unitPrice)
                    .taxRate(taxRate)
                    .build();

            CreateInvoiceRequest req = CreateInvoiceRequest.builder()
                    .clientId(client.getId())
                    .locationId(loc.getId())
                    .invoiceDate(LocalDate.of(2026, 10, 1))
                    .dueDate(LocalDate.of(2026, 10, 15))
                    .items(List.of(item))
                    .notes("Invoice with " + taxRate + "% GST")
                    .terms("Payment due in 15 days")
                    .build();

            // 1. Create invoice
            String createResp = mockMvc.perform(post("/api/v1/invoices")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.subtotal").value(unitPrice.doubleValue()))
                    .andExpect(jsonPath("$.data.tax").value(expectedTax.doubleValue()))
                    .andExpect(jsonPath("$.data.total").value(expectedTotal.doubleValue()))
                    .andExpect(jsonPath("$.data.items[0].taxRate").value(taxRate.doubleValue()))
                    .andExpect(jsonPath("$.data.items[0].tax").value(expectedTax.doubleValue()))
                    .andReturn().getResponse().getContentAsString();

            UUID invoiceId = UUID.fromString(objectMapper.readTree(createResp).path("data").path("id").asText());

            // 2. Reopen / Retrieve invoice and assert 0% or selected rate has not reverted to 18%
            mockMvc.perform(get("/api/v1/invoices/" + invoiceId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.subtotal").value(unitPrice.doubleValue()))
                    .andExpect(jsonPath("$.data.tax").value(expectedTax.doubleValue()))
                    .andExpect(jsonPath("$.data.total").value(expectedTotal.doubleValue()))
                    .andExpect(jsonPath("$.data.items[0].taxRate").value(taxRate.doubleValue()))
                    .andExpect(jsonPath("$.data.items[0].tax").value(expectedTax.doubleValue()));
        }
    }
}
