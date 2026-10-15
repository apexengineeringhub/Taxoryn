package com.taxoryn.module.billing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.billing.dto.CreateInvoiceItemRequest;
import com.taxoryn.module.billing.dto.CreateInvoiceRequest;
import com.taxoryn.module.billing.dto.UpdateInvoiceRequest;
import com.taxoryn.module.billing.entity.InvoiceItemEntity.BillingServiceType;
import com.taxoryn.module.billing.entity.PracticeInvoiceSeriesEntity;
import com.taxoryn.module.billing.repository.InvoicePaymentRepository;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.billing.repository.PracticeInvoiceSeriesRepository;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GstInvoicingIntegrationTest {

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
    private PracticeInvoiceSeriesRepository practiceInvoiceSeriesRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private OrganizationEntity organization;
    private UserEntity adminUser;
    private String adminToken;
    private ClientEntity intraStateClient;
    private ClientEntity interStateClient;

    @BeforeEach
    void setUp() {
        organization = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Chartered Accountants LLP")
                .legalName("Apex Chartered Accountants LLP")
                .email("admin-" + UUID.randomUUID() + "@apexca.com")
                .state("Maharashtra")
                .gstin("27AABCU9603R1ZM")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(organization.getId());

        RoleEntity adminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Org Admin")
                        .description("Firm Administrator")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        adminUser = UserEntity.builder()
                .email("admin-" + UUID.randomUUID() + "@apexca.com")
                .passwordHash(passwordEncoder.encode("Test@123456"))
                .firstName("Ramesh")
                .lastName("Shah")
                .roles(new HashSet<>(Set.of(adminRole)))
                .status(UserStatus.ACTIVE)
                .build();
        adminUser.setOrganizationId(organization.getId());
        adminUser = userRepository.save(adminUser);

        adminToken = jwtTokenProvider.generateAccessToken(
                adminUser.getId(),
                organization.getId(),
                null,
                adminUser.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("BILLING_CREATE", "BILLING_VIEW", "BILLING_UPDATE", "BILLING_DELETE")
        );

        intraStateClient = ClientEntity.builder()
                .displayName("Mumbai Tech Enterprises")
                .legalName("Mumbai Tech Enterprises Pvt Ltd")
                .pan("AAECM1234A")
                .gstin("27AAECM1234A1Z5")
                .state("Maharashtra")
                .stateCode("27")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .build();
        intraStateClient.setOrganizationId(organization.getId());
        intraStateClient = clientRepository.save(intraStateClient);

        interStateClient = ClientEntity.builder()
                .displayName("Bangalore Cloud Solutions")
                .legalName("Bangalore Cloud Solutions LLP")
                .pan("AABCB5678B")
                .gstin("29AABCB5678B1Z8")
                .state("Karnataka")
                .stateCode("29")
                .clientType(ClientType.LLP)
                .status(ClientStatus.ACTIVE)
                .build();
        interStateClient.setOrganizationId(organization.getId());
        interStateClient = clientRepository.save(interStateClient);

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        invoicePaymentRepository.deleteAll();
        invoiceRepository.deleteAll();
        practiceInvoiceSeriesRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("Statutory GST - Intra-State: Split into equal CGST and SGST with IGST zero")
    void testIntraStateGstCalculation_CgstAndSgstSplitWithSacCodes() throws Exception {
        CreateInvoiceRequest req = CreateInvoiceRequest.builder()
                .clientId(intraStateClient.getId())
                .invoiceDate(LocalDate.of(2026, 5, 10))
                .dueDate(LocalDate.of(2026, 5, 25))
                .currency("INR")
                .notes("Monthly GST Filing & Tax Advisory")
                .items(List.of(
                        CreateInvoiceItemRequest.builder()
                                .service(BillingServiceType.GST_FILING)
                                .description("Monthly GSTR-1 and GSTR-3B Filing Compliance")
                                .quantity(BigDecimal.ONE)
                                .unitPrice(new BigDecimal("10000.00"))
                                .taxRate(new BigDecimal("18.00"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.invoiceNumber", startsWith("INV-")))
                .andExpect(jsonPath("$.data.invoiceNumber", containsString("26-27")))
                .andExpect(jsonPath("$.data.subtotal").value(10000.00))
                .andExpect(jsonPath("$.data.tax").value(1800.00))
                .andExpect(jsonPath("$.data.cgstAmount").value(900.00))
                .andExpect(jsonPath("$.data.sgstAmount").value(900.00))
                .andExpect(jsonPath("$.data.igstAmount").value(0.00))
                .andExpect(jsonPath("$.data.total").value(11800.00))
                .andExpect(jsonPath("$.data.balanceDue").value(11800.00))
                .andExpect(jsonPath("$.data.placeOfSupplyState").value("Maharashtra"))
                .andExpect(jsonPath("$.data.placeOfSupplyStateCode").value("27"))
                .andExpect(jsonPath("$.data.supplierState").value("Maharashtra"))
                .andExpect(jsonPath("$.data.supplierStateCode").value("27"))
                .andExpect(jsonPath("$.data.isRcm").value(false))
                .andExpect(jsonPath("$.data.financialYear").value("2026-27"))
                .andExpect(jsonPath("$.data.seriesPrefix").value("INV"))
                .andExpect(jsonPath("$.data.items[0].sacHsnCode").value("998231"))
                .andExpect(jsonPath("$.data.items[0].cgstRate").value(9.00))
                .andExpect(jsonPath("$.data.items[0].cgstAmount").value(900.00))
                .andExpect(jsonPath("$.data.items[0].sgstRate").value(9.00))
                .andExpect(jsonPath("$.data.items[0].sgstAmount").value(900.00))
                .andExpect(jsonPath("$.data.items[0].igstRate").value(0.00))
                .andExpect(jsonPath("$.data.items[0].igstAmount").value(0.00));
    }

    @Test
    @DisplayName("Statutory GST - Inter-State: Exclusive IGST with CGST and SGST zero")
    void testInterStateGstCalculation_IgstAppliedExclusively() throws Exception {
        CreateInvoiceRequest req = CreateInvoiceRequest.builder()
                .clientId(interStateClient.getId())
                .invoiceDate(LocalDate.of(2026, 6, 15))
                .dueDate(LocalDate.of(2026, 6, 30))
                .currency("INR")
                .notes("Inter-State Corporate Tax Advisory")
                .items(List.of(
                        CreateInvoiceItemRequest.builder()
                                .service(BillingServiceType.CONSULTING)
                                .description("Cross-Border Transfer Pricing Advisory")
                                .quantity(new BigDecimal("2.00"))
                                .unitPrice(new BigDecimal("15000.00"))
                                .taxRate(new BigDecimal("18.00"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.invoiceNumber", startsWith("INV-")))
                .andExpect(jsonPath("$.data.subtotal").value(30000.00))
                .andExpect(jsonPath("$.data.tax").value(5400.00))
                .andExpect(jsonPath("$.data.cgstAmount").value(0.00))
                .andExpect(jsonPath("$.data.sgstAmount").value(0.00))
                .andExpect(jsonPath("$.data.igstAmount").value(5400.00))
                .andExpect(jsonPath("$.data.total").value(35400.00))
                .andExpect(jsonPath("$.data.placeOfSupplyState").value("Karnataka"))
                .andExpect(jsonPath("$.data.placeOfSupplyStateCode").value("29"))
                .andExpect(jsonPath("$.data.supplierState").value("Maharashtra"))
                .andExpect(jsonPath("$.data.supplierStateCode").value("27"))
                .andExpect(jsonPath("$.data.items[0].sacHsnCode").value("998311"))
                .andExpect(jsonPath("$.data.items[0].cgstRate").value(0.00))
                .andExpect(jsonPath("$.data.items[0].cgstAmount").value(0.00))
                .andExpect(jsonPath("$.data.items[0].sgstRate").value(0.00))
                .andExpect(jsonPath("$.data.items[0].sgstAmount").value(0.00))
                .andExpect(jsonPath("$.data.items[0].igstRate").value(18.00))
                .andExpect(jsonPath("$.data.items[0].igstAmount").value(5400.00));
    }

    @Test
    @DisplayName("Statutory GST - Zero/Exempt Tax Rate Produces Zero Tax Breakdown")
    void testZeroTaxAndExemptServices() throws Exception {
        CreateInvoiceRequest req = CreateInvoiceRequest.builder()
                .clientId(intraStateClient.getId())
                .invoiceDate(LocalDate.of(2026, 7, 1))
                .dueDate(LocalDate.of(2026, 7, 15))
                .items(List.of(
                        CreateInvoiceItemRequest.builder()
                                .service(BillingServiceType.OTHER)
                                .description("Exempt Educational Seminar Presentation")
                                .quantity(BigDecimal.ONE)
                                .unitPrice(new BigDecimal("5000.00"))
                                .taxRate(BigDecimal.ZERO)
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.subtotal").value(5000.00))
                .andExpect(jsonPath("$.data.tax").value(0.00))
                .andExpect(jsonPath("$.data.cgstAmount").value(0.00))
                .andExpect(jsonPath("$.data.sgstAmount").value(0.00))
                .andExpect(jsonPath("$.data.igstAmount").value(0.00))
                .andExpect(jsonPath("$.data.total").value(5000.00));
    }

    @Test
    @DisplayName("Financial Year Sequential Series - Isolated Sequence Counters across Financial Years")
    void testFinancialYearRolloverAndCustomSeriesPrefix() throws Exception {
        // FY 2026-27 Invoice 1
        CreateInvoiceRequest req1 = CreateInvoiceRequest.builder()
                .clientId(intraStateClient.getId())
                .seriesPrefix("TAX")
                .invoiceDate(LocalDate.of(2026, 5, 10))
                .dueDate(LocalDate.of(2026, 5, 25))
                .items(List.of(CreateInvoiceItemRequest.builder()
                        .service(BillingServiceType.AUDIT)
                        .quantity(BigDecimal.ONE)
                        .unitPrice(new BigDecimal("25000.00"))
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.invoiceNumber").value("TAX-26-27-0001"))
                .andExpect(jsonPath("$.data.financialYear").value("2026-27"))
                .andExpect(jsonPath("$.data.seriesPrefix").value("TAX"));

        // FY 2026-27 Invoice 2
        CreateInvoiceRequest req2 = CreateInvoiceRequest.builder()
                .clientId(intraStateClient.getId())
                .seriesPrefix("TAX")
                .invoiceDate(LocalDate.of(2026, 6, 12))
                .dueDate(LocalDate.of(2026, 6, 27))
                .items(List.of(CreateInvoiceItemRequest.builder()
                        .service(BillingServiceType.AUDIT)
                        .quantity(BigDecimal.ONE)
                        .unitPrice(new BigDecimal("25000.00"))
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.invoiceNumber").value("TAX-26-27-0002"));

        // Prior FY 2025-26 Invoice (Dated Feb 2026) -> should allocate TAX-25-26-0001 independently!
        CreateInvoiceRequest req3 = CreateInvoiceRequest.builder()
                .clientId(intraStateClient.getId())
                .seriesPrefix("TAX")
                .invoiceDate(LocalDate.of(2026, 2, 15))
                .dueDate(LocalDate.of(2026, 3, 2))
                .items(List.of(CreateInvoiceItemRequest.builder()
                        .service(BillingServiceType.AUDIT)
                        .quantity(BigDecimal.ONE)
                        .unitPrice(new BigDecimal("15000.00"))
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.invoiceNumber").value("TAX-25-26-0001"))
                .andExpect(jsonPath("$.data.financialYear").value("2025-26"))
                .andExpect(jsonPath("$.data.seriesPrefix").value("TAX"));
    }

    @Test
    @DisplayName("Concurrency Safety - Multithreaded concurrent allocation produces zero duplicate invoice numbers")
    void testMultithreadedConcurrentSequentialAllocation() throws Exception {
        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        List<String> allocatedNumbers = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    CreateInvoiceRequest req = CreateInvoiceRequest.builder()
                            .clientId(intraStateClient.getId())
                            .seriesPrefix("CONC")
                            .invoiceDate(LocalDate.of(2026, 8, 1))
                            .dueDate(LocalDate.of(2026, 8, 15))
                            .items(List.of(CreateInvoiceItemRequest.builder()
                                    .service(BillingServiceType.CONSULTING)
                                    .quantity(BigDecimal.ONE)
                                    .unitPrice(new BigDecimal("1000.00"))
                                    .build()))
                            .build();

                    MvcResult result = mockMvc.perform(post("/api/v1/invoices")
                                    .header("Authorization", "Bearer " + adminToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(req)))
                            .andReturn();

                    if (result.getResponse().getStatus() == 201) {
                        String invNum = objectMapper.readTree(result.getResponse().getContentAsString())
                                .path("data").path("invoiceNumber").asText();
                        allocatedNumbers.add(invNum);
                    } else {
                        errors.add(new RuntimeException("HTTP Status: " + result.getResponse().getStatus() + " - " + result.getResponse().getContentAsString()));
                    }
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All concurrent tasks should complete within timeout");
        assertTrue(errors.isEmpty(), "No concurrent requests should fail. Errors: " + errors);
        assertEquals(threadCount, allocatedNumbers.size(), "All threads must successfully create an invoice");

        Set<String> uniqueNumbers = new HashSet<>(allocatedNumbers);
        assertEquals(threadCount, uniqueNumbers.size(), "Zero duplicate invoice numbers allowed across threads: " + allocatedNumbers);

        // Verify database series counter matches allocated count
        PracticeInvoiceSeriesEntity series = practiceInvoiceSeriesRepository
                .findByOrganizationIdAndFinancialYearAndSeriesPrefix(organization.getId(), "2026-27", "CONC")
                .orElseThrow();
        assertEquals(threadCount, series.getCurrentSequence(), "Series sequence counter must exactly match total allocated invoices");
    }

    @Test
    @DisplayName("Draft Update - Recalculates GST breakdown when line items are updated")
    void testDraftInvoiceUpdateRecalculatesGstBreakdown() throws Exception {
        CreateInvoiceRequest createReq = CreateInvoiceRequest.builder()
                .clientId(intraStateClient.getId())
                .invoiceDate(LocalDate.of(2026, 5, 20))
                .dueDate(LocalDate.of(2026, 6, 5))
                .items(List.of(CreateInvoiceItemRequest.builder()
                        .service(BillingServiceType.ACCOUNTING)
                        .quantity(BigDecimal.ONE)
                        .unitPrice(new BigDecimal("5000.00"))
                        .taxRate(new BigDecimal("18.00"))
                        .build()))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.cgstAmount").value(450.00))
                .andExpect(jsonPath("$.data.sgstAmount").value(450.00))
                .andReturn();

        UUID invoiceId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Update Draft with larger line items
        UpdateInvoiceRequest updateReq = UpdateInvoiceRequest.builder()
                .items(List.of(
                        CreateInvoiceItemRequest.builder()
                                .service(BillingServiceType.ACCOUNTING)
                                .description("Full-Year Accounting & Trial Balance Reconciliation")
                                .quantity(BigDecimal.ONE)
                                .unitPrice(new BigDecimal("20000.00"))
                                .taxRate(new BigDecimal("18.00"))
                                .build()
                ))
                .notes("Revised scope of bookkeeping services")
                .build();

        mockMvc.perform(put("/api/v1/invoices/" + invoiceId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subtotal").value(20000.00))
                .andExpect(jsonPath("$.data.tax").value(3600.00))
                .andExpect(jsonPath("$.data.cgstAmount").value(1800.00))
                .andExpect(jsonPath("$.data.sgstAmount").value(1800.00))
                .andExpect(jsonPath("$.data.igstAmount").value(0.00))
                .andExpect(jsonPath("$.data.total").value(23600.00));
    }
}
