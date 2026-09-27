package com.taxoryn.module.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.billing.dto.CreateInvoiceItemRequest;
import com.taxoryn.module.billing.dto.CreateInvoiceRequest;
import com.taxoryn.module.billing.dto.CreatePromotionRequest;
import com.taxoryn.module.billing.dto.InvoiceDto;
import com.taxoryn.module.billing.dto.UpdatePromotionRequest;
import com.taxoryn.module.billing.dto.UpdatePromotionStatusRequest;
import com.taxoryn.module.billing.entity.BillingProfileEntity;
import com.taxoryn.module.billing.entity.InvoiceItemEntity.BillingServiceType;
import com.taxoryn.module.billing.model.PricingType;
import com.taxoryn.module.billing.model.PromotionDiscountType;
import com.taxoryn.module.billing.model.PromotionType;
import com.taxoryn.module.billing.repository.BillingProfileRepository;
import com.taxoryn.module.billing.repository.InvoiceItemRepository;
import com.taxoryn.module.billing.repository.InvoicePaymentRepository;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.billing.repository.PromotionRepository;
import com.taxoryn.module.billing.service.InvoiceService;
import com.taxoryn.module.billing.service.PromotionService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PracticePromotionalPricingIntegrationTest {

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
    private BillingProfileRepository billingProfileRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private InvoicePaymentRepository invoicePaymentRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private PromotionService promotionService;

    @Autowired
    private InvoiceService invoiceService;

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
                .name("Apex Advisory & Tax - " + UUID.randomUUID())
                .legalName("Apex Advisory & Tax LLP")
                .email("promo.ops." + UUID.randomUUID() + "@firm.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        LocationEntity l = LocationEntity.builder()
                .name("Bengaluru Hub")
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
                .email("partner." + UUID.randomUUID() + "@apexpromo.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Suresh")
                .lastName("Iyer")
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
                .displayName("Zepto Tech Solutions Pvt Ltd")
                .legalName("Zepto Tech Solutions Private Limited")
                .clientType(ClientType.COMPANY)
                .pan("AABCZ7890F")
                .gstin("29AABCZ7890F1Z2")
                .locationId(loc.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        c.setOrganizationId(org.getId());
        client = clientRepository.save(c);

        TenantContext.setTenantId(org.getId());

        SecurityUser securityUser = SecurityUser.builder()
                .userId(user.getId())
                .organizationId(org.getId())
                .email(user.getEmail())
                .enabled(true)
                .roles(Set.of("ORG_ADMIN"))
                .permissions(Set.of("ROLE_ORG_ADMIN", "BILLING_VIEW", "BILLING_CREATE", "BILLING_UPDATE", "BILLING_WRITE", "CLIENT_VIEW"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private void cleanUp() {
        invoicePaymentRepository.deleteAll();
        invoiceItemRepository.deleteAll();
        invoiceRepository.deleteAll();
        billingProfileRepository.deleteAll();
        promotionRepository.deleteAll();
        clientRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    private InvoiceDto createInvoiceViaApi(CreateInvoiceRequest req) throws Exception {
        String responseJson = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Organization-Id", org.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(objectMapper.readTree(responseJson).get("data").toString(), InvoiceDto.class);
    }

    @Test
    @DisplayName("Practice Admin Promotion CRUD: Create, List, Get, Update, Toggle Status, Delete")
    void testPromotionCrudWorkflow() throws Exception {
        CreatePromotionRequest createReq = CreatePromotionRequest.builder()
                .code("DIWALI2026")
                .name("Diwali Festive Compliance 20% Off")
                .description("20% off all GST filing services")
                .promotionType(PromotionType.SERVICE_SPECIFIC)
                .discountType(PromotionDiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("20.00"))
                .targetService(BillingServiceType.GST_FILING)
                .validFrom(LocalDate.now().minusDays(1))
                .validUntil(LocalDate.now().plusMonths(1))
                .priority(10)
                .active(true)
                .build();

        // 1. Create Promotion
        String responseJson = mockMvc.perform(post("/api/v1/billing/promotions")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Organization-Id", org.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("DIWALI2026"))
                .andExpect(jsonPath("$.data.discountValue").value(20.00))
                .andExpect(jsonPath("$.data.active").value(true))
                .andReturn().getResponse().getContentAsString();

        UUID promoId = UUID.fromString(objectMapper.readTree(responseJson).get("data").get("id").asText());

        // 2. List Promotions
        mockMvc.perform(get("/api/v1/billing/promotions")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Organization-Id", org.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(promoId.toString()));

        // 3. Get Promotion by ID
        mockMvc.perform(get("/api/v1/billing/promotions/{id}", promoId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Organization-Id", org.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Diwali Festive Compliance 20% Off"));

        // 4. Update Promotion
        UpdatePromotionRequest updateReq = UpdatePromotionRequest.builder()
                .name("Diwali Festive Compliance Mega 25% Off")
                .discountValue(new BigDecimal("25.00"))
                .build();

        mockMvc.perform(put("/api/v1/billing/promotions/{id}", promoId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Organization-Id", org.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Diwali Festive Compliance Mega 25% Off"))
                .andExpect(jsonPath("$.data.discountValue").value(25.00));

        // 5. Toggle Status
        UpdatePromotionStatusRequest statusReq = UpdatePromotionStatusRequest.builder()
                .active(false)
                .build();

        mockMvc.perform(patch("/api/v1/billing/promotions/{id}/status", promoId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Organization-Id", org.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        // 6. Delete Promotion
        mockMvc.perform(delete("/api/v1/billing/promotions/{id}", promoId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Organization-Id", org.getId().toString()))
                .andExpect(status().isNoContent());

        assertThat(promotionRepository.findById(promoId)).isEmpty();
    }

    @Test
    @DisplayName("Promotional Pricing: Percentage, Fixed Amount, and Fixed Price resolution on Invoices")
    void testPromotionalPricingResolutionTypes() throws Exception {
        // Create 3 active promotions:
        // 1. GST Filing 20% Percentage discount (Standard: 2500 -> 2000)
        promotionRepository.save(com.taxoryn.module.billing.entity.PromotionEntity.builder()
                .name("GST 20% Off")
                .promotionType(PromotionType.SERVICE_SPECIFIC)
                .targetService(BillingServiceType.GST_FILING)
                .discountType(PromotionDiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("20.00"))
                .priority(5)
                .active(true)
                .build());

        // 2. ITR Filing ₹500 Fixed Amount discount (Standard: 3500 -> 3000)
        promotionRepository.save(com.taxoryn.module.billing.entity.PromotionEntity.builder()
                .name("ITR ₹500 Off")
                .promotionType(PromotionType.SERVICE_SPECIFIC)
                .targetService(BillingServiceType.ITR_FILING)
                .discountType(PromotionDiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("500.00"))
                .priority(5)
                .active(true)
                .build());

        // 3. TDS Fixed Price ₹1,800 flat (Standard: 2000 -> 1800)
        promotionRepository.save(com.taxoryn.module.billing.entity.PromotionEntity.builder()
                .name("TDS Fixed Deal")
                .promotionType(PromotionType.SERVICE_SPECIFIC)
                .targetService(BillingServiceType.TDS)
                .discountType(PromotionDiscountType.FIXED_PRICE)
                .discountValue(new BigDecimal("1800.00"))
                .priority(5)
                .active(true)
                .build());

        CreateInvoiceRequest invReq = CreateInvoiceRequest.builder()
                .clientId(client.getId())
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(15))
                .items(List.of(
                        CreateInvoiceItemRequest.builder()
                                .service(BillingServiceType.GST_FILING)
                                .description("Monthly GST Compliance")
                                .quantity(BigDecimal.ONE)
                                .build(),
                        CreateInvoiceItemRequest.builder()
                                .service(BillingServiceType.ITR_FILING)
                                .description("Annual ITR Filing")
                                .quantity(BigDecimal.ONE)
                                .build(),
                        CreateInvoiceItemRequest.builder()
                                .service(BillingServiceType.TDS)
                                .description("Quarterly TDS Return")
                                .quantity(BigDecimal.ONE)
                                .build()
                ))
                .build();

        InvoiceDto invoiceDto = createInvoiceViaApi(invReq);
        assertThat(invoiceDto.getItems()).hasSize(3);

        // Verify GST line item snapshot (2500 - 20% = 2000)
        var gstItem = invoiceDto.getItems().stream().filter(i -> i.getService() == BillingServiceType.GST_FILING).findFirst().orElseThrow();
        assertThat(gstItem.getPricingType()).isEqualTo(PricingType.PROMOTIONAL);
        assertThat(gstItem.getStandardUnitPrice()).isEqualByComparingTo("2500.00");
        assertThat(gstItem.getDiscountType()).isEqualTo(PromotionDiscountType.PERCENTAGE);
        assertThat(gstItem.getDiscountValue()).isEqualByComparingTo("20.00");
        assertThat(gstItem.getDiscountAmount()).isEqualByComparingTo("500.00");
        assertThat(gstItem.getUnitPrice()).isEqualByComparingTo("2000.00");

        // Verify ITR line item snapshot (3500 - 500 = 3000)
        var itrItem = invoiceDto.getItems().stream().filter(i -> i.getService() == BillingServiceType.ITR_FILING).findFirst().orElseThrow();
        assertThat(itrItem.getPricingType()).isEqualTo(PricingType.PROMOTIONAL);
        assertThat(itrItem.getStandardUnitPrice()).isEqualByComparingTo("3500.00");
        assertThat(itrItem.getDiscountType()).isEqualTo(PromotionDiscountType.FIXED_AMOUNT);
        assertThat(itrItem.getDiscountValue()).isEqualByComparingTo("500.00");
        assertThat(itrItem.getDiscountAmount()).isEqualByComparingTo("500.00");
        assertThat(itrItem.getUnitPrice()).isEqualByComparingTo("3000.00");

        // Verify TDS line item snapshot (Fixed 1800)
        var tdsItem = invoiceDto.getItems().stream().filter(i -> i.getService() == BillingServiceType.TDS).findFirst().orElseThrow();
        assertThat(tdsItem.getPricingType()).isEqualTo(PricingType.PROMOTIONAL);
        assertThat(tdsItem.getStandardUnitPrice()).isEqualByComparingTo("2000.00");
        assertThat(tdsItem.getDiscountType()).isEqualTo(PromotionDiscountType.FIXED_PRICE);
        assertThat(tdsItem.getDiscountValue()).isEqualByComparingTo("1800.00");
        assertThat(tdsItem.getDiscountAmount()).isEqualByComparingTo("200.00");
        assertThat(tdsItem.getUnitPrice()).isEqualByComparingTo("1800.00");

        // Subtotal = 2000 + 3000 + 1800 = 6800.00
        assertThat(invoiceDto.getSubtotal()).isEqualByComparingTo("6800.00");
    }

    @Test
    @DisplayName("Pricing Precedence & Anti-Stacking: Custom > Customer-Specific > Promotion > Standard")
    void testPricingPrecedenceAndAntiStacking() throws Exception {
        // Configure active promotion (20% off GST filing)
        promotionRepository.save(com.taxoryn.module.billing.entity.PromotionEntity.builder()
                .name("GST 20% Off Promotion")
                .promotionType(PromotionType.SERVICE_SPECIFIC)
                .targetService(BillingServiceType.GST_FILING)
                .discountType(PromotionDiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("20.00"))
                .priority(10)
                .active(true)
                .build());

        // Create client A with Customer-Specific rate in BillingProfile (defaultRate = 2200.00)
        billingProfileRepository.save(BillingProfileEntity.builder()
                .clientId(client.getId())
                .defaultRate(new BigDecimal("2200.00"))
                .build());

        // Create Client B without Billing Profile
        ClientEntity clientB = clientRepository.save(ClientEntity.builder()
                .displayName("Blinkit Commerce Pvt Ltd")
                .clientType(ClientType.COMPANY)
                .status(ClientStatus.ACTIVE)
                .build());

        // 1. Client A with Customer-Specific Rate (Should get 2200, NO promo stacking)
        CreateInvoiceRequest invReqClientA = CreateInvoiceRequest.builder()
                .clientId(client.getId())
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(15))
                .items(List.of(
                        CreateInvoiceItemRequest.builder()
                                .service(BillingServiceType.GST_FILING)
                                .description("GST Compliance for Client A")
                                .quantity(BigDecimal.ONE)
                                .build()
                ))
                .build();

        InvoiceDto invA = createInvoiceViaApi(invReqClientA);
        assertThat(invA.getItems().get(0).getPricingType()).isEqualTo(PricingType.CUSTOMER_SPECIFIC);
        assertThat(invA.getItems().get(0).getUnitPrice()).isEqualByComparingTo("2200.00");
        assertThat(invA.getItems().get(0).getDiscountAmount()).isEqualByComparingTo("0.00");

        // 2. Client B without Customer-Specific Rate (Should get Promotional 2000)
        CreateInvoiceRequest invReqClientB = CreateInvoiceRequest.builder()
                .clientId(clientB.getId())
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(15))
                .items(List.of(
                        CreateInvoiceItemRequest.builder()
                                .service(BillingServiceType.GST_FILING)
                                .description("GST Compliance for Client B")
                                .quantity(BigDecimal.ONE)
                                .build()
                ))
                .build();

        InvoiceDto invB = createInvoiceViaApi(invReqClientB);
        assertThat(invB.getItems().get(0).getPricingType()).isEqualTo(PricingType.PROMOTIONAL);
        assertThat(invB.getItems().get(0).getUnitPrice()).isEqualByComparingTo("2000.00");

        // 3. Client B with explicit Custom / Manual Unit Price (unitPrice = 4000.00)
        CreateInvoiceRequest invReqManual = CreateInvoiceRequest.builder()
                .clientId(clientB.getId())
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(15))
                .items(List.of(
                        CreateInvoiceItemRequest.builder()
                                .service(BillingServiceType.GST_FILING)
                                .description("Custom Advisory Billing")
                                .quantity(BigDecimal.ONE)
                                .unitPrice(new BigDecimal("4000.00"))
                                .build()
                ))
                .build();

        InvoiceDto invManual = createInvoiceViaApi(invReqManual);
        assertThat(invManual.getItems().get(0).getPricingType()).isEqualTo(PricingType.CUSTOM);
        assertThat(invManual.getItems().get(0).getUnitPrice()).isEqualByComparingTo("4000.00");
    }

    @Test
    @DisplayName("New Client Promotion Qualification: Valid on first invoice, ineligible on subsequent invoices")
    void testNewClientPromotionQualification() throws Exception {
        // Create NEW_CLIENT promotion (30% off for new clients)
        promotionRepository.save(com.taxoryn.module.billing.entity.PromotionEntity.builder()
                .name("Welcome New Client 30% Off")
                .promotionType(PromotionType.NEW_CLIENT)
                .discountType(PromotionDiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("30.00"))
                .priority(10)
                .active(true)
                .build());

        // First invoice for client (Standard consulting: 3000 -> 30% off = 2100)
        CreateInvoiceRequest firstInv = CreateInvoiceRequest.builder()
                .clientId(client.getId())
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(15))
                .items(List.of(
                        CreateInvoiceItemRequest.builder()
                                .service(BillingServiceType.CONSULTING)
                                .description("Initial Onboarding Advisory")
                                .quantity(BigDecimal.ONE)
                                .build()
                ))
                .build();

        InvoiceDto firstResult = createInvoiceViaApi(firstInv);
        assertThat(firstResult.getItems().get(0).getPricingType()).isEqualTo(PricingType.PROMOTIONAL);
        assertThat(firstResult.getItems().get(0).getUnitPrice()).isEqualByComparingTo("2100.00");

        // Second invoice for same client (Should no longer qualify for NEW_CLIENT promotion -> Standard 3000)
        CreateInvoiceRequest secondInv = CreateInvoiceRequest.builder()
                .clientId(client.getId())
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(15))
                .items(List.of(
                        CreateInvoiceItemRequest.builder()
                                .service(BillingServiceType.CONSULTING)
                                .description("Follow-up Advisory")
                                .quantity(BigDecimal.ONE)
                                .build()
                ))
                .build();

        InvoiceDto secondResult = createInvoiceViaApi(secondInv);
        assertThat(secondResult.getItems().get(0).getPricingType()).isEqualTo(PricingType.STANDARD);
        assertThat(secondResult.getItems().get(0).getUnitPrice()).isEqualByComparingTo("3000.00");
    }

    @Test
    @DisplayName("Historical Immutability: Deactivating or changing promotion does not affect existing invoice snapshot")
    void testHistoricalInvoiceImmutability() throws Exception {
        var promo = promotionRepository.save(com.taxoryn.module.billing.entity.PromotionEntity.builder()
                .name("Flash GST 50% Off")
                .promotionType(PromotionType.SERVICE_SPECIFIC)
                .targetService(BillingServiceType.GST_FILING)
                .discountType(PromotionDiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("50.00"))
                .priority(10)
                .active(true)
                .build());

        CreateInvoiceRequest invReq = CreateInvoiceRequest.builder()
                .clientId(client.getId())
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(15))
                .items(List.of(
                        CreateInvoiceItemRequest.builder()
                                .service(BillingServiceType.GST_FILING)
                                .description("GST Filing 50% Promo")
                                .quantity(BigDecimal.ONE)
                                .build()
                ))
                .build();

        InvoiceDto createdInvoice = createInvoiceViaApi(invReq);
        assertThat(createdInvoice.getItems().get(0).getUnitPrice()).isEqualByComparingTo("1250.00");
        assertThat(createdInvoice.getSubtotal()).isEqualByComparingTo("1250.00");

        // Deactivate and modify promotion
        var freshPromo = promotionRepository.findById(promo.getId()).orElseThrow();
        freshPromo.setActive(false);
        freshPromo.setDiscountValue(new BigDecimal("10.00"));
        promotionRepository.save(freshPromo);

        // Fetch existing invoice via API -> must retain original snapshot and amounts
        String getRes = mockMvc.perform(get("/api/v1/invoices/{id}", createdInvoice.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("X-Organization-Id", org.getId().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        InvoiceDto fetchedInvoice = objectMapper.readValue(objectMapper.readTree(getRes).get("data").toString(), InvoiceDto.class);
        assertThat(fetchedInvoice.getItems().get(0).getUnitPrice()).isEqualByComparingTo("1250.00");
        assertThat(fetchedInvoice.getItems().get(0).getDiscountValue()).isEqualByComparingTo("50.00");
        assertThat(fetchedInvoice.getSubtotal()).isEqualByComparingTo("1250.00");
    }
}
