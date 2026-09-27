package com.taxoryn.module.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.billing.dto.GenerateInvoiceFromTimeEntriesRequest;
import com.taxoryn.module.billing.dto.RecordPaymentRequest;
import com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus;
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
import com.taxoryn.module.timetracking.entity.TimeEntryEntity;
import com.taxoryn.module.timetracking.model.TimeEntryStatus;
import com.taxoryn.module.timetracking.repository.TimeEntryRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PracticeBillingOperationsIntegrationTest {

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
    private TimeEntryRepository timeEntryRepository;

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
                .name("Apex CA & Tax Consultants - " + UUID.randomUUID())
                .legalName("Apex CA & Tax Consultants LLP")
                .email("billing.ops." + UUID.randomUUID() + "@firm.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        LocationEntity l = LocationEntity.builder()
                .name("Mumbai HQ")
                .code("MUM-" + UUID.randomUUID().toString().substring(0, 4))
                .city("Mumbai")
                .state("Maharashtra")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        l.setOrganizationId(org.getId());
        loc = locationRepository.save(l);

        user = userRepository.save(UserEntity.builder()
                .organizationId(org.getId())
                .email("partner." + UUID.randomUUID() + "@apex.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Rajesh")
                .lastName("Verma")
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
                .displayName("Swiggy Express Private Limited")
                .legalName("Swiggy Express Private Limited")
                .clientType(ClientType.COMPANY)
                .pan("AABCS1234F")
                .gstin("27AABCS1234F1Z5")
                .locationId(loc.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        c.setOrganizationId(org.getId());
        client = clientRepository.save(c);

        EngagementEntity eng = EngagementEntity.builder()
                .locationId(loc.getId())
                .clientId(client.getId())
                .name("FY26 Comprehensive Corporate Tax & Audit")
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
        timeEntryRepository.deleteAll();
        engagementRepository.deleteAll();
        clientRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("Complete Practice Billing Workflow: Unbilled Time -> Invoice -> Issue -> Partial Payment -> Final Payment -> Receivables Aging")
    void testCompletePracticeBillingWorkflow() throws Exception {
        // 1. Create billable unbilled time entries
        TimeEntryEntity time1 = TimeEntryEntity.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .engagementId(engagement.getId())
                .userId(user.getId())
                .entryDate(LocalDate.now().minusDays(5))
                .durationMinutes(120) // 2.0 hours
                .billingRate(new BigDecimal("3000.00")) // 2.0 * 3000 = 6000
                .description("Transfer Pricing documentation and benchmarking analysis")
                .billable(true)
                .status(TimeEntryStatus.APPROVED)
                .build();
        time1.setOrganizationId(org.getId());
        time1 = timeEntryRepository.save(time1);

        TimeEntryEntity time2 = TimeEntryEntity.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .engagementId(engagement.getId())
                .userId(user.getId())
                .entryDate(LocalDate.now().minusDays(3))
                .durationMinutes(90) // 1.5 hours
                .billingRate(new BigDecimal("2000.00")) // 1.5 * 2000 = 3000
                .description("GSTR-9 annual return preparation & reconciliation")
                .billable(true)
                .status(TimeEntryStatus.SUBMITTED)
                .build();
        time2.setOrganizationId(org.getId());
        time2 = timeEntryRepository.save(time2);

        // 2. Query unbilled time entries
        mockMvc.perform(get("/api/v1/billing/unbilled-time")
                        .param("clientId", client.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].clientName").value("Swiggy Express Private Limited"))
                .andExpect(jsonPath("$.data[0].billableAmount").value(6000.00))
                .andExpect(jsonPath("$.data[1].billableAmount").value(3000.00));

        // 3. Generate Invoice from Time Entries
        // Subtotal = 6000 + 3000 = 9000
        // Tax 18% = 1620
        // Discount = 500
        // Total = 9000 + 1620 - 500 = 10120
        GenerateInvoiceFromTimeEntriesRequest genReq = GenerateInvoiceFromTimeEntriesRequest.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .engagementId(engagement.getId())
                .timeEntryIds(List.of(time1.getId(), time2.getId()))
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(15))
                .taxRate(new BigDecimal("18.00"))
                .discount(new BigDecimal("500.00"))
                .notes("Billing for Transfer Pricing & GSTR-9")
                .terms("Due within 15 days")
                .build();

        String genResp = mockMvc.perform(post("/api/v1/billing/from-time-entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.invoiceNumber", startsWith("INV-")))
                .andExpect(jsonPath("$.data.subtotal").value(9000.00))
                .andExpect(jsonPath("$.data.tax").value(1620.00))
                .andExpect(jsonPath("$.data.discount").value(500.00))
                .andExpect(jsonPath("$.data.total").value(10120.00))
                .andExpect(jsonPath("$.data.balanceDue").value(10120.00))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        UUID invoiceId = UUID.fromString(objectMapper.readTree(genResp).path("data").path("id").asText());

        // Verify time entries are now BILLED in DB
        TimeEntryEntity reloaded1 = timeEntryRepository.findById(time1.getId()).orElseThrow();
        TimeEntryEntity reloaded2 = timeEntryRepository.findById(time2.getId()).orElseThrow();
        assertThat(reloaded1.getStatus()).isEqualTo(TimeEntryStatus.BILLED);
        assertThat(reloaded2.getStatus()).isEqualTo(TimeEntryStatus.BILLED);

        // 4. Verify unbilled time list is now empty for this client
        mockMvc.perform(get("/api/v1/billing/unbilled-time")
                        .param("clientId", client.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        // 5. Issue the Invoice
        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/issue")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ISSUED"));

        // 6. Record Partial Payment (₹4,000)
        RecordPaymentRequest partPaymentReq = RecordPaymentRequest.builder()
                .amount(new BigDecimal("4000.00"))
                .paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .referenceNumber("NEFT12345678")
                .notes("Part advance payment")
                .build();

        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partPaymentReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount").value(4000.00))
                .andExpect(jsonPath("$.data.receiptNumber", startsWith("REC-")));

        // Check invoice balance: Paid = 4000, Balance = 6120, Status = PARTIALLY_PAID
        mockMvc.perform(get("/api/v1/invoices/" + invoiceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paidAmount").value(4000.00))
                .andExpect(jsonPath("$.data.balanceDue").value(6120.00))
                .andExpect(jsonPath("$.data.status").value("PARTIALLY_PAID"));

        // 7. Attempt Overpayment (> 6120) -> Must fail with 400 Bad Request
        RecordPaymentRequest overpayReq = RecordPaymentRequest.builder()
                .amount(new BigDecimal("7000.00"))
                .paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.UPI)
                .build();

        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overpayReq)))
                .andExpect(status().isBadRequest());

        // 8. Record Final Payment of exact remaining balance (₹6,120)
        RecordPaymentRequest finalPaymentReq = RecordPaymentRequest.builder()
                .amount(new BigDecimal("6120.00"))
                .paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.UPI)
                .referenceNumber("UPI987654321")
                .notes("Final settlement")
                .build();

        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(finalPaymentReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount").value(6120.00))
                .andExpect(jsonPath("$.data.receiptNumber", startsWith("REC-")));

        // Check invoice balance: Paid = 10120, Balance = 0, Status = PAID
        mockMvc.perform(get("/api/v1/invoices/" + invoiceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paidAmount").value(10120.00))
                .andExpect(jsonPath("$.data.balanceDue").value(0.00))
                .andExpect(jsonPath("$.data.status").value("PAID"));

        // 9. Query Receivables Summary
        mockMvc.perform(get("/api/v1/billing/receivables")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalInvoiced").value(10120.00))
                .andExpect(jsonPath("$.data.totalCollected").value(10120.00))
                .andExpect(jsonPath("$.data.totalOutstanding").value(0.00))
                .andExpect(jsonPath("$.data.paidInvoicesCount").value(1))
                .andExpect(jsonPath("$.data.clientBreakdown.length()").value(1))
                .andExpect(jsonPath("$.data.clientBreakdown[0].clientName").value("Swiggy Express Private Limited"));
    }
}
