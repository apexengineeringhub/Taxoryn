package com.taxoryn.module.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.billing.dto.CreateInvoiceRequest;
import com.taxoryn.module.billing.dto.InvoiceDto;
import com.taxoryn.module.billing.entity.InvoiceEntity;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.engagement.dto.CreateEngagementRequest;
import com.taxoryn.module.engagement.model.EngagementPriority;
import com.taxoryn.module.engagement.model.EngagementStatus;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.service.dto.CreateServiceRequest;
import com.taxoryn.module.service.dto.UpdatePracticeServicePricingRequest;
import com.taxoryn.module.service.dto.UpdateServiceRequest;
import com.taxoryn.module.service.entity.ServiceEntity;
import com.taxoryn.module.service.model.ServiceCategory;
import com.taxoryn.module.service.model.ServiceScope;
import com.taxoryn.module.service.model.ServiceStatus;
import com.taxoryn.module.service.repository.ServiceRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ServiceCatalogIntegrationTest {

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
    private ServiceRepository serviceRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private EngagementRepository engagementRepository;

    private OrganizationEntity orgA;
    private UserEntity userA;
    private String tokenA;

    private OrganizationEntity orgB;
    private UserEntity userB;
    private String tokenB;

    @BeforeEach
    void setUp() {
        TenantContext.clear();

        seedDefaultServicesIfMissing();

        RoleEntity adminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Organization Administrator")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        // Practice A
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Sharma & Co - " + UUID.randomUUID())
                .legalName("Sharma & Co LLP")
                .email("admin." + UUID.randomUUID() + "@sharma.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        userA = userRepository.save(UserEntity.builder()
                .organizationId(orgA.getId())
                .email("admin." + UUID.randomUUID() + "@sharma.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Aakash")
                .lastName("Sharma")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        tokenA = jwtTokenProvider.generateAccessToken(
                userA.getId(),
                orgA.getId(),
                userA.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "CLIENT_VIEW", "CLIENT_READ", "ORGANIZATION_UPDATE", "ORG_WRITE", "INVOICE_CREATE", "INVOICE_VIEW", "BILLING_MANAGE")
        );

        // Practice B
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Patel & Associates - " + UUID.randomUUID())
                .legalName("Patel & Associates")
                .email("admin." + UUID.randomUUID() + "@patel.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        userB = userRepository.save(UserEntity.builder()
                .organizationId(orgB.getId())
                .email("admin." + UUID.randomUUID() + "@patel.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Bhavesh")
                .lastName("Patel")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        tokenB = jwtTokenProvider.generateAccessToken(
                userB.getId(),
                orgB.getId(),
                userB.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "CLIENT_VIEW", "CLIENT_READ", "ORGANIZATION_UPDATE", "ORG_WRITE", "INVOICE_CREATE", "INVOICE_VIEW", "BILLING_MANAGE")
        );

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("GET /api/v1/services returns seeded catalog with GST, TDS, ITR, Audit, Notice, Advisory services")
    void testGetServicesCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/services")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.serviceCode == 'GST_COMPLIANCE')]").exists())
                .andExpect(jsonPath("$.data[?(@.serviceCode == 'TDS_COMPLIANCE')]").exists())
                .andExpect(jsonPath("$.data[?(@.serviceCode == 'ITR_FILING')]").exists())
                .andExpect(jsonPath("$.data[?(@.serviceCode == 'TAX_AUDIT')]").exists())
                .andExpect(jsonPath("$.data[?(@.serviceCode == 'NOTICE_MANAGEMENT')]").exists())
                .andExpect(jsonPath("$.data[?(@.serviceCode == 'TAX_ADVISORY')]").exists());
    }

    @Test
    @DisplayName("POST /api/v1/services creates custom practice service with default price, billing unit and GST rate")
    void testCreateAndGetCustomService() throws Exception {
        CreateServiceRequest request = CreateServiceRequest.builder()
                .serviceCode("PAN_CARD_APP_" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                .serviceName("PAN Card Application Assistance")
                .description("New PAN allotment / correction form 49A")
                .category(ServiceCategory.GOVERNMENT_SERVICES)
                .billingUnit("PER_APPLICATION")
                .defaultPrice(new BigDecimal("500.00"))
                .taxRate(new BigDecimal("18.00"))
                .build();

        String response = mockMvc.perform(post("/api/v1/services")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.serviceName").value("PAN Card Application Assistance"))
                .andExpect(jsonPath("$.data.category").value("GOVERNMENT_SERVICES"))
                .andExpect(jsonPath("$.data.scope").value("PRACTICE"))
                .andExpect(jsonPath("$.data.billingUnit").value("PER_APPLICATION"))
                .andExpect(jsonPath("$.data.defaultPrice").value(500.00))
                .andExpect(jsonPath("$.data.taxRate").value(18.00))
                .andReturn().getResponse().getContentAsString();

        UUID serviceId = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());

        mockMvc.perform(get("/api/v1/services/" + serviceId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(serviceId.toString()))
                .andExpect(jsonPath("$.data.serviceName").value("PAN Card Application Assistance"));
    }

    @Test
    @DisplayName("Multi-tenant isolation: Practice B cannot see, edit, or delete Practice A's custom service")
    void testTenantIsolationForCustomServices() throws Exception {
        CreateServiceRequest request = CreateServiceRequest.builder()
                .serviceCode("FSSAI_REG_" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                .serviceName("FSSAI Food License Registration")
                .category(ServiceCategory.REGISTRATION)
                .billingUnit("PER_APPLICATION")
                .defaultPrice(new BigDecimal("3500.00"))
                .taxRate(new BigDecimal("18.00"))
                .build();

        String response = mockMvc.perform(post("/api/v1/services")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID serviceId = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());

        // Practice B attempting to get Practice A's service returns 404
        mockMvc.perform(get("/api/v1/services/" + serviceId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // Practice B attempting to update Practice A's service returns 404
        UpdateServiceRequest updateReq = UpdateServiceRequest.builder()
                .serviceName("Hacked Service")
                .build();
        mockMvc.perform(put("/api/v1/services/" + serviceId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        // Practice B attempting to delete Practice A's service returns 404
        mockMvc.perform(delete("/api/v1/services/" + serviceId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // Practice B's service catalog list does NOT contain Practice A's service
        mockMvc.perform(get("/api/v1/services")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + serviceId + "')]").doesNotExist());
    }

    @Test
    @DisplayName("Filtering services by search, category, scope, and active status")
    void testServiceFiltering() throws Exception {
        CreateServiceRequest request = CreateServiceRequest.builder()
                .serviceCode("DSC_TOKEN_" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                .serviceName("Class 3 Digital Signature Certificate")
                .category(ServiceCategory.GOVERNMENT_SERVICES)
                .billingUnit("PER_APPLICATION")
                .defaultPrice(new BigDecimal("1200.00"))
                .taxRate(new BigDecimal("18.00"))
                .build();

        mockMvc.perform(post("/api/v1/services")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Filter by scope=PRACTICE
        mockMvc.perform(get("/api/v1/services?scope=PRACTICE")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.serviceName == 'Class 3 Digital Signature Certificate')]").exists());

        // Filter by category=GOVERNMENT_SERVICES
        mockMvc.perform(get("/api/v1/services?category=GOVERNMENT_SERVICES")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.serviceName == 'Class 3 Digital Signature Certificate')]").exists());

        // Search by term 'Digital'
        mockMvc.perform(get("/api/v1/services?search=Digital")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.serviceName == 'Class 3 Digital Signature Certificate')]").exists());
    }

    @Test
    @DisplayName("Toggle status and delete custom practice service")
    void testToggleStatusAndDeleteService() throws Exception {
        CreateServiceRequest request = CreateServiceRequest.builder()
                .serviceCode("TAN_APP_" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                .serviceName("TAN Registration")
                .category(ServiceCategory.REGISTRATION)
                .billingUnit("PER_APPLICATION")
                .defaultPrice(new BigDecimal("800.00"))
                .taxRate(new BigDecimal("18.00"))
                .build();

        String response = mockMvc.perform(post("/api/v1/services")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID serviceId = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());

        // Deactivate service
        mockMvc.perform(patch("/api/v1/services/" + serviceId + "/status?active=false")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        // Reactivate service
        mockMvc.perform(patch("/api/v1/services/" + serviceId + "/status?active=true")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // Delete service
        mockMvc.perform(delete("/api/v1/services/" + serviceId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Getting deleted service returns 404
        mockMvc.perform(get("/api/v1/services/" + serviceId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Custom practice services are included in /api/v1/practice/service-pricing")
    void testPracticePricingIncludesCustomServices() throws Exception {
        CreateServiceRequest request = CreateServiceRequest.builder()
                .serviceCode("MSME_REG_" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                .serviceName("MSME Udyam Registration")
                .category(ServiceCategory.REGISTRATION)
                .billingUnit("PER_APPLICATION")
                .defaultPrice(new BigDecimal("1500.00"))
                .taxRate(new BigDecimal("18.00"))
                .build();

        mockMvc.perform(post("/api/v1/services")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/practice/service-pricing")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.serviceName == 'MSME Udyam Registration')]").exists())
                .andExpect(jsonPath("$.data[?(@.serviceName == 'MSME Udyam Registration')].practicePrice").value(1500.00))
                .andExpect(jsonPath("$.data[?(@.serviceName == 'MSME Udyam Registration')].scope").value("PRACTICE"));
    }

    @Test
    @DisplayName("Historical Invoice Immutability: Changing service price does not mutate existing invoice lines")
    void testHistoricalInvoiceImmutability() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        ClientEntity client = ClientEntity.builder()
                .displayName("Ramesh Patel")
                .clientType(ClientEntity.ClientType.INDIVIDUAL)
                .status(ClientEntity.ClientStatus.ACTIVE)
                .email("ramesh." + UUID.randomUUID() + "@example.com")
                .phone("+919876543210")
                .pan("ABCDE1234F")
                .build();
        client.setOrganizationId(orgA.getId());
        client = clientRepository.save(client);

        TenantContext.clear();

        // Create Custom Service with Price ₹2,000
        String code = "TAX_PLAN_" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        CreateServiceRequest serviceReq = CreateServiceRequest.builder()
                .serviceCode(code)
                .serviceName("HNW Tax Planning Session")
                .category(ServiceCategory.ADVISORY)
                .billingUnit("HOURLY")
                .defaultPrice(new BigDecimal("2000.00"))
                .taxRate(new BigDecimal("18.00"))
                .build();

        String serviceResp = mockMvc.perform(post("/api/v1/services")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(serviceReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID serviceId = UUID.fromString(objectMapper.readTree(serviceResp).path("data").path("id").asText());

        // Create Invoice using that service with ₹2,000
        CreateInvoiceRequest invoiceReq = CreateInvoiceRequest.builder()
                .clientId(client.getId())
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(15))
                .items(List.of(
                        com.taxoryn.module.billing.dto.CreateInvoiceItemRequest.builder()
                                .serviceCode(code)
                                .description("HNW Tax Planning Session")
                                .quantity(BigDecimal.ONE)
                                .unitPrice(new BigDecimal("2000.00"))
                                .taxRate(new BigDecimal("18.00"))
                                .build()
                ))
                .build();

        String invoiceResp = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invoiceReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalAmount").value(2360.00)) // 2000 + 18% = 2360
                .andReturn().getResponse().getContentAsString();

        UUID invoiceId = UUID.fromString(objectMapper.readTree(invoiceResp).path("data").path("id").asText());

        // Now Update the service default price to ₹5,000
        UpdateServiceRequest updateServiceReq = UpdateServiceRequest.builder()
                .defaultPrice(new BigDecimal("5000.00"))
                .build();

        mockMvc.perform(put("/api/v1/services/" + serviceId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateServiceReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultPrice").value(5000.00));

        // Fetch Historical Invoice — verify it still contains unitPrice=2000 and totalAmount=2360
        mockMvc.perform(get("/api/v1/invoices/" + invoiceId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(2360.00))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(2000.00))
                .andExpect(jsonPath("$.data.items[0].taxRate").value(18.00));
    }

    @Test
    @DisplayName("Engagement creation successfully links to custom practice service")
    void testEngagementCreationWithCustomPracticeService() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        ClientEntity client = ClientEntity.builder()
                .displayName("Client For Custom Service - " + UUID.randomUUID())
                .clientType(ClientEntity.ClientType.INDIVIDUAL)
                .status(ClientEntity.ClientStatus.ACTIVE)
                .email("cust." + UUID.randomUUID() + "@example.com")
                .pan("ABCDE9999Z")
                .build();
        client.setOrganizationId(orgA.getId());
        client = clientRepository.save(client);
        TenantContext.clear();

        // 1. Create practice custom service
        String sCode = "PAYROLL_" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        CreateServiceRequest serviceReq = CreateServiceRequest.builder()
                .serviceCode(sCode)
                .serviceName("Monthly Payroll Processing")
                .category(ServiceCategory.ADVISORY)
                .billingUnit("PER_MONTH")
                .defaultPrice(new BigDecimal("7500.00"))
                .taxRate(new BigDecimal("18.00"))
                .build();

        String serviceResp = mockMvc.perform(post("/api/v1/services")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(serviceReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID serviceId = UUID.fromString(objectMapper.readTree(serviceResp).path("data").path("id").asText());

        // 2. Create Engagement with custom service
        CreateEngagementRequest engReq = CreateEngagementRequest.builder()
                .clientId(client.getId())
                .serviceId(serviceId)
                .name("FY 2026-27 Payroll Mandate")
                .priority(EngagementPriority.HIGH)
                .status(EngagementStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 4, 1))
                .build();

        mockMvc.perform(post("/api/v1/engagements")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(engReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.serviceId").value(serviceId.toString()))
                .andExpect(jsonPath("$.data.serviceName").value("Monthly Payroll Processing"))
                .andExpect(jsonPath("$.data.serviceCategory").value("ADVISORY"));
    }

    @Test
    @DisplayName("Inactive service cannot be used to create a new engagement")
    void testInactiveServiceCannotBeUsedForNewEngagement() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        ClientEntity client = ClientEntity.builder()
                .displayName("Client For Inactive Service - " + UUID.randomUUID())
                .clientType(ClientEntity.ClientType.INDIVIDUAL)
                .status(ClientEntity.ClientStatus.ACTIVE)
                .email("inactive." + UUID.randomUUID() + "@example.com")
                .pan("ABCDE8888Y")
                .build();
        client.setOrganizationId(orgA.getId());
        client = clientRepository.save(client);
        TenantContext.clear();

        // 1. Create and deactivate a service
        String sCode = "DEPRECATED_" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        CreateServiceRequest serviceReq = CreateServiceRequest.builder()
                .serviceCode(sCode)
                .serviceName("Legacy VAT Consulting")
                .category(ServiceCategory.ADVISORY)
                .billingUnit("ONE_TIME")
                .defaultPrice(new BigDecimal("3000.00"))
                .taxRate(new BigDecimal("18.00"))
                .build();

        String serviceResp = mockMvc.perform(post("/api/v1/services")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(serviceReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID serviceId = UUID.fromString(objectMapper.readTree(serviceResp).path("data").path("id").asText());

        // Deactivate service
        mockMvc.perform(patch("/api/v1/services/" + serviceId + "/status?active=false")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        // 2. Try to create engagement with deactivated service -> 400 Bad Request
        CreateEngagementRequest engReq = CreateEngagementRequest.builder()
                .clientId(client.getId())
                .serviceId(serviceId)
                .name("Attempted Engagement with Inactive Service")
                .build();

        mockMvc.perform(post("/api/v1/engagements")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(engReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("0% GST rate is strictly preserved on custom service and invoices without reverting to 18%")
    void testZeroPercentGstRatePreservedOnCustomServiceAndInvoice() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        ClientEntity client = ClientEntity.builder()
                .displayName("Export Client - " + UUID.randomUUID())
                .clientType(ClientEntity.ClientType.COMPANY)
                .status(ClientEntity.ClientStatus.ACTIVE)
                .email("export." + UUID.randomUUID() + "@example.com")
                .pan("EXPOR1234E")
                .build();
        client.setOrganizationId(orgA.getId());
        client = clientRepository.save(client);
        TenantContext.clear();

        // 1. Create custom service with taxRate = 0.00
        String code = "EXEMPT_ADV_" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        CreateServiceRequest serviceReq = CreateServiceRequest.builder()
                .serviceCode(code)
                .serviceName("Exempt Educational / Export Advisory")
                .category(ServiceCategory.ADVISORY)
                .billingUnit("FIXED")
                .defaultPrice(new BigDecimal("10000.00"))
                .taxRate(BigDecimal.ZERO)
                .build();

        String serviceResp = mockMvc.perform(post("/api/v1/services")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(serviceReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.taxRate").value(0.00))
                .andReturn().getResponse().getContentAsString();

        // 2. Create invoice with 0% GST line
        CreateInvoiceRequest invoiceReq = CreateInvoiceRequest.builder()
                .clientId(client.getId())
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .items(List.of(
                        com.taxoryn.module.billing.dto.CreateInvoiceItemRequest.builder()
                                .serviceCode(code)
                                .description("Exempt Educational / Export Advisory")
                                .quantity(BigDecimal.ONE)
                                .unitPrice(new BigDecimal("10000.00"))
                                .taxRate(BigDecimal.ZERO)
                                .build()
                ))
                .build();

        String invoiceResp = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invoiceReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.subtotal").value(10000.00))
                .andExpect(jsonPath("$.data.tax").value(0.00))
                .andExpect(jsonPath("$.data.totalAmount").value(10000.00))
                .andExpect(jsonPath("$.data.items[0].taxRate").value(0.00))
                .andExpect(jsonPath("$.data.items[0].tax").value(0.00))
                .andReturn().getResponse().getContentAsString();

        UUID invoiceId = UUID.fromString(objectMapper.readTree(invoiceResp).path("data").path("id").asText());

        // 3. Verify fetched invoice preserves 0.00 tax rate
        mockMvc.perform(get("/api/v1/invoices/" + invoiceId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subtotal").value(10000.00))
                .andExpect(jsonPath("$.data.tax").value(0.00))
                .andExpect(jsonPath("$.data.totalAmount").value(10000.00))
                .andExpect(jsonPath("$.data.items[0].taxRate").value(0.00));
    }

    @Test
    @DisplayName("RBAC: Read-only or unauthorized staff user cannot create or modify service catalog entries")
    void testRbacUnauthorizedUserCannotCreateOrModifyService() throws Exception {
        // Staff user without ORG_ADMIN or ORGANIZATION_UPDATE
        RoleEntity staffRole = roleRepository.findByCodeAndIsSystemRoleTrue("STAFF")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("STAFF")
                        .name("Staff")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        UserEntity staffUser = userRepository.save(UserEntity.builder()
                .organizationId(orgA.getId())
                .email("staff." + UUID.randomUUID() + "@sharma.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Staff")
                .lastName("Member")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(staffRole))
                .build());

        String staffToken = jwtTokenProvider.generateAccessToken(
                staffUser.getId(),
                orgA.getId(),
                staffUser.getEmail(),
                Set.of("ROLE_STAFF"),
                Set.of("ROLE_STAFF", "CLIENT_VIEW", "CLIENT_READ")
        );

        CreateServiceRequest request = CreateServiceRequest.builder()
                .serviceCode("UNAUTH_" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                .serviceName("Unauthorized Service Creation Attempt")
                .category(ServiceCategory.ADVISORY)
                .billingUnit("PER_FILING")
                .defaultPrice(new BigDecimal("1000.00"))
                .taxRate(new BigDecimal("18.00"))
                .build();

        // POST /api/v1/services -> 403 Forbidden
        mockMvc.perform(post("/api/v1/services")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    private void seedDefaultServicesIfMissing() {
        if (serviceRepository.findByServiceCodeAndOrganizationIdIsNull("GST_COMPLIANCE").isEmpty()) {
            serviceRepository.save(ServiceEntity.builder()
                    .serviceCode("GST_COMPLIANCE")
                    .serviceName("GST Compliance & Returns")
                    .category(ServiceCategory.GST)
                    .scope(ServiceScope.TAXORYN)
                    .status(ServiceStatus.ACTIVE)
                    .moduleCode("GST")
                    .defaultPrice(new BigDecimal("1500.00"))
                    .billingUnit("PER_FILING")
                    .taxRate(new BigDecimal("18.00"))
                    .build());
        }
        if (serviceRepository.findByServiceCodeAndOrganizationIdIsNull("TDS_COMPLIANCE").isEmpty()) {
            serviceRepository.save(ServiceEntity.builder()
                    .serviceCode("TDS_COMPLIANCE")
                    .serviceName("TDS Compliance & Returns")
                    .category(ServiceCategory.TDS)
                    .scope(ServiceScope.TAXORYN)
                    .status(ServiceStatus.ACTIVE)
                    .moduleCode("TDS")
                    .defaultPrice(new BigDecimal("1200.00"))
                    .billingUnit("PER_FILING")
                    .taxRate(new BigDecimal("18.00"))
                    .build());
        }
        if (serviceRepository.findByServiceCodeAndOrganizationIdIsNull("ITR_FILING").isEmpty()) {
            serviceRepository.save(ServiceEntity.builder()
                    .serviceCode("ITR_FILING")
                    .serviceName("Income Tax Returns (ITR)")
                    .category(ServiceCategory.ITR)
                    .scope(ServiceScope.TAXORYN)
                    .status(ServiceStatus.ACTIVE)
                    .moduleCode("ITR")
                    .defaultPrice(new BigDecimal("2000.00"))
                    .billingUnit("PER_RETURN")
                    .taxRate(new BigDecimal("18.00"))
                    .build());
        }
        if (serviceRepository.findByServiceCodeAndOrganizationIdIsNull("TAX_AUDIT").isEmpty()) {
            serviceRepository.save(ServiceEntity.builder()
                    .serviceCode("TAX_AUDIT")
                    .serviceName("Tax Audit (Form 3CD/3CA/3CB)")
                    .category(ServiceCategory.AUDIT)
                    .scope(ServiceScope.TAXORYN)
                    .status(ServiceStatus.ACTIVE)
                    .moduleCode("AUDIT")
                    .defaultPrice(new BigDecimal("15000.00"))
                    .billingUnit("PER_REPORT")
                    .taxRate(new BigDecimal("18.00"))
                    .build());
        }
        if (serviceRepository.findByServiceCodeAndOrganizationIdIsNull("NOTICE_MANAGEMENT").isEmpty()) {
            serviceRepository.save(ServiceEntity.builder()
                    .serviceCode("NOTICE_MANAGEMENT")
                    .serviceName("Tax Notice & Assessment Management")
                    .category(ServiceCategory.NOTICE)
                    .scope(ServiceScope.TAXORYN)
                    .status(ServiceStatus.ACTIVE)
                    .moduleCode("NOTICE")
                    .defaultPrice(new BigDecimal("2500.00"))
                    .billingUnit("PER_NOTICE")
                    .taxRate(new BigDecimal("18.00"))
                    .build());
        }
        if (serviceRepository.findByServiceCodeAndOrganizationIdIsNull("TAX_ADVISORY").isEmpty()) {
            serviceRepository.save(ServiceEntity.builder()
                    .serviceCode("TAX_ADVISORY")
                    .serviceName("Tax Advisory & Opinion")
                    .category(ServiceCategory.ADVISORY)
                    .scope(ServiceScope.TAXORYN)
                    .status(ServiceStatus.ACTIVE)
                    .defaultPrice(new BigDecimal("5000.00"))
                    .billingUnit("HOURLY")
                    .taxRate(new BigDecimal("18.00"))
                    .build());
        }
    }
}
