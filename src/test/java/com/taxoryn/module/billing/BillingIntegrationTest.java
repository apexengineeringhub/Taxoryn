package com.taxoryn.module.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.billing.dto.CreateBillingProfileRequest;
import com.taxoryn.module.billing.dto.CreateInvoiceItemRequest;
import com.taxoryn.module.billing.dto.CreateInvoiceRequest;
import com.taxoryn.module.billing.dto.UpdateBillingProfileRequest;
import com.taxoryn.module.billing.dto.UpdateInvoiceStatusRequest;
import com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus;
import com.taxoryn.module.billing.entity.InvoiceItemEntity.BillingServiceType;
import com.taxoryn.module.billing.repository.BillingProfileRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BillingIntegrationTest {

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
    private BillingProfileRepository billingProfileRepository;

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
                .name("Singhania & Partners - " + UUID.randomUUID())
                .legalName("Singhania & Partners LLP")
                .email("billing." + UUID.randomUUID() + "@firm.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        LocationEntity l = LocationEntity.builder()
                .name("Delhi HQ")
                .code("DEL-" + UUID.randomUUID().toString().substring(0, 4))
                .city("Delhi")
                .state("Delhi")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        l.setOrganizationId(org.getId());
        loc = locationRepository.save(l);

        user = userRepository.save(UserEntity.builder()
                .organizationId(org.getId())
                .email("admin." + UUID.randomUUID() + "@singhania.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Alok")
                .lastName("Singhania")
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
                .displayName("Zomato Logistics Pvt Ltd")
                .legalName("Zomato Logistics Private Limited")
                .clientType(ClientType.COMPANY)
                .pan("AABCZ8888Z")
                .locationId(loc.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        c.setOrganizationId(org.getId());
        client = clientRepository.save(c);

        EngagementEntity eng = EngagementEntity.builder()
                .locationId(loc.getId())
                .clientId(client.getId())
                .name("Monthly GST Filing & Annual Review")
                .build();
        eng.setOrganizationId(org.getId());
        engagement = engagementRepository.save(eng);
        engagement = engagementRepository.save(engagement);
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
        billingProfileRepository.deleteAll();
        engagementRepository.deleteAll();
        clientRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("Create Billing Profile, retrieve, and update")
    void testBillingProfileCRUD() throws Exception {
        CreateBillingProfileRequest request = CreateBillingProfileRequest.builder()
                .clientId(client.getId())
                .engagementId(engagement.getId())
                .billingFrequency("MONTHLY")
                .currency("INR")
                .defaultRate(new BigDecimal("15000.00"))
                .taxApplicable(true)
                .active(true)
                .notes("Standard monthly retainer fee")
                .build();

        String response = mockMvc.perform(post("/api/v1/billing-profiles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.billingFrequency").value("MONTHLY"))
                .andExpect(jsonPath("$.data.defaultRate").value(15000.00))
                .andReturn().getResponse().getContentAsString();

        UUID profileId = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());

        // Get by Client ID
        mockMvc.perform(get("/api/v1/billing-profiles/clients/" + client.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].clientName").value("Zomato Logistics Pvt Ltd"));

        // Update Billing Profile
        UpdateBillingProfileRequest updateReq = UpdateBillingProfileRequest.builder()
                .defaultRate(new BigDecimal("18000.00"))
                .billingFrequency("QUARTERLY")
                .build();

        mockMvc.perform(put("/api/v1/billing-profiles/" + profileId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultRate").value(18000.00))
                .andExpect(jsonPath("$.data.billingFrequency").value("QUARTERLY"));
    }

    @Test
    @DisplayName("Create Invoice with line items, verify tax and total calculations, update status and query client invoices")
    void testInvoiceCRUDAndStatusTransitions() throws Exception {
        CreateInvoiceItemRequest item1 = CreateInvoiceItemRequest.builder()
                .service(BillingServiceType.GST_FILING)
                .description("GSTR-1 and GSTR-3B Monthly Filing for July 2026")
                .quantity(new BigDecimal("1"))
                .unitPrice(new BigDecimal("10000.00"))
                .taxRate(new BigDecimal("18.00"))
                .build();

        CreateInvoiceItemRequest item2 = CreateInvoiceItemRequest.builder()
                .service(BillingServiceType.CONSULTING)
                .description("Input Tax Credit Reversal Analysis under Rule 37A")
                .quantity(new BigDecimal("2"))
                .unitPrice(new BigDecimal("2500.00"))
                .taxRate(new BigDecimal("18.00"))
                .build();

        CreateInvoiceRequest invReq = CreateInvoiceRequest.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .engagementId(engagement.getId())
                .currency("INR")
                .invoiceDate(LocalDate.of(2026, 8, 1))
                .dueDate(LocalDate.of(2026, 8, 15))
                .items(List.of(item1, item2))
                .terms("Payment due in 15 days")
                .notes("Monthly retainer invoice")
                .build();

        // subtotal = 10000 + 5000 = 15000
        // tax = 1800 + 900 = 2700
        // total = 17700
        String response = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.subtotal").value(15000.00))
                .andExpect(jsonPath("$.data.tax").value(2700.00))
                .andExpect(jsonPath("$.data.total").value(17700.00))
                .andExpect(jsonPath("$.data.currency").value("INR"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        UUID invoiceId = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());

        // Get by ID
        mockMvc.perform(get("/api/v1/invoices/" + invoiceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clientName").value("Zomato Logistics Pvt Ltd"))
                .andExpect(jsonPath("$.data.locationName").value("Delhi HQ"))
                .andExpect(jsonPath("$.data.engagementName").value("Monthly GST Filing & Annual Review"))
                .andExpect(jsonPath("$.data.items.length()").value(2));

        // Update status to ISSUED via PATCH /api/v1/invoices/{id}/status
        UpdateInvoiceStatusRequest statusReq = UpdateInvoiceStatusRequest.builder()
                .status(InvoiceStatus.ISSUED)
                .notes("Invoice approved by partner and sent to client")
                .build();

        mockMvc.perform(patch("/api/v1/invoices/" + invoiceId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ISSUED"));

        // Retrieve invoices by Client ID (Client 360)
        mockMvc.perform(get("/api/v1/invoices/clients/" + client.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(invoiceId.toString()));
    }
}
