package com.taxoryn.module.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.billing.dto.CreateInvoiceItemRequest;
import com.taxoryn.module.billing.dto.CreateInvoiceRequest;
import com.taxoryn.module.billing.dto.RecordPaymentRequest;
import com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus;
import com.taxoryn.module.billing.entity.InvoiceItemEntity.BillingServiceType;
import com.taxoryn.module.billing.entity.InvoicePaymentEntity.PaymentMethod;
import com.taxoryn.module.billing.repository.InvoiceItemRepository;
import com.taxoryn.module.billing.repository.InvoicePaymentRepository;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.entity.ClientUserAssignmentEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.repository.ClientUserAssignmentRepository;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.entity.UserLocationEntity;
import com.taxoryn.module.user.repository.UserLocationRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BillingAccessScopeIntegrationTest {

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
    private ClientUserAssignmentRepository clientUserAssignmentRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private InvoicePaymentRepository invoicePaymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserLocationRepository userLocationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity org;
    private LocationEntity locA;
    private LocationEntity locB;
    private ClientEntity clientA;
    private ClientEntity clientB;

    private UserEntity adminUser;
    private String adminToken;

    private UserEntity staffUserA;
    private String staffTokenA;

    private UserEntity portalUserA;
    private String portalTokenA;

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

        RoleEntity staffRole = roleRepository.findByCodeAndIsSystemRoleTrue("PRACTICE_STAFF")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("PRACTICE_STAFF")
                        .name("Practice Staff")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        RoleEntity portalRole = roleRepository.findByCodeAndIsSystemRoleTrue("CLIENT_PORTAL_USER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("CLIENT_PORTAL_USER")
                        .name("Client Portal User")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        org = organizationRepository.save(OrganizationEntity.builder()
                .name("National Tax Partners - " + UUID.randomUUID())
                .legalName("National Tax Partners LLP")
                .email("scope." + UUID.randomUUID() + "@firm.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // Location A & B
        locA = LocationEntity.builder()
                .name("Mumbai Branch")
                .code("MUM-" + UUID.randomUUID().toString().substring(0, 4))
                .city("Mumbai")
                .state("Maharashtra")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        locA.setOrganizationId(org.getId());
        locA = locationRepository.save(locA);

        locB = LocationEntity.builder()
                .name("Delhi Branch")
                .code("DEL-" + UUID.randomUUID().toString().substring(0, 4))
                .city("Delhi")
                .state("Delhi")
                .isHeadOffice(false)
                .isActive(true)
                .build();
        locB.setOrganizationId(org.getId());
        locB = locationRepository.save(locB);

        // Client A (Loc A) & Client B (Loc B)
        clientA = ClientEntity.builder()
                .displayName("Alpha Traders Pvt Ltd")
                .legalName("Alpha Traders Private Limited")
                .clientType(ClientType.COMPANY)
                .pan("AABCA1111A")
                .locationId(locA.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        clientA.setOrganizationId(org.getId());
        clientA = clientRepository.save(clientA);

        clientB = ClientEntity.builder()
                .displayName("Beta Logistics LLP")
                .legalName("Beta Logistics LLP")
                .clientType(ClientType.LLP)
                .pan("AABCB2222B")
                .locationId(locB.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        clientB.setOrganizationId(org.getId());
        clientB = clientRepository.save(clientB);

        // Admin User
        adminUser = userRepository.save(UserEntity.builder()
                .organizationId(org.getId())
                .email("admin." + UUID.randomUUID() + "@firm.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Super")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        adminToken = jwtTokenProvider.generateAccessToken(
                adminUser.getId(),
                org.getId(),
                adminUser.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "BILLING_VIEW", "BILLING_CREATE", "BILLING_UPDATE", "BILLING_WRITE", "CLIENT_VIEW")
        );

        // Staff User scoped only to Location A & Client A
        staffUserA = userRepository.save(UserEntity.builder()
                .organizationId(org.getId())
                .email("staff.a." + UUID.randomUUID() + "@firm.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Amit")
                .lastName("Shah")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(staffRole))
                .build());

        UserLocationEntity userLocA = UserLocationEntity.builder()
                .id(UserLocationEntity.UserLocationId.builder()
                        .userId(staffUserA.getId())
                        .locationId(locA.getId())
                        .build())
                .organizationId(org.getId())
                .build();
        userLocationRepository.save(userLocA);

        ClientUserAssignmentEntity assignA = ClientUserAssignmentEntity.builder()
                .clientId(clientA.getId())
                .userId(staffUserA.getId())
                .assignedAt(Instant.now())
                .build();
        assignA.setOrganizationId(org.getId());
        clientUserAssignmentRepository.save(assignA);

        staffTokenA = jwtTokenProvider.generateAccessToken(
                staffUserA.getId(),
                org.getId(),
                staffUserA.getEmail(),
                Set.of("ROLE_PRACTICE_STAFF"),
                Set.of("ROLE_PRACTICE_STAFF", "BILLING_VIEW", "BILLING_CREATE", "BILLING_UPDATE", "CLIENT_VIEW")
        );

        // Portal User for Client A
        portalUserA = userRepository.save(UserEntity.builder()
                .organizationId(org.getId())
                .clientId(clientA.getId())
                .email("portal.a." + UUID.randomUUID() + "@alphatraders.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Alpha")
                .lastName("Director")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(portalRole))
                .build());

        portalTokenA = jwtTokenProvider.generateAccessToken(
                portalUserA.getId(),
                org.getId(),
                portalUserA.getEmail(),
                Set.of("ROLE_CLIENT_PORTAL_USER"),
                Set.of("ROLE_CLIENT_PORTAL_USER", "BILLING_VIEW")
        );
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
        clientUserAssignmentRepository.deleteAll();
        userLocationRepository.deleteAll();
        clientRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("ABAC: Staff user scoped to Client A cannot view or invoice Client B")
    void testStaffPortfolioAndLocationScoping() throws Exception {
        // 1. Admin creates invoice for Client B (in Location B)
        CreateInvoiceRequest invReqB = CreateInvoiceRequest.builder()
                .clientId(clientB.getId())
                .locationId(locB.getId())
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(15))
                .items(List.of(CreateInvoiceItemRequest.builder()
                        .service(BillingServiceType.CONSULTING)
                        .description("Delhi location tax advisory")
                        .quantity(BigDecimal.ONE)
                        .unitPrice(new BigDecimal("10000.00"))
                        .build()))
                .build();

        String respB = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invReqB)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID invoiceIdB = UUID.fromString(objectMapper.readTree(respB).path("data").path("id").asText());

        // 2. Staff A (scoped to Client A / Location A) tries to get Invoice B -> 403 Forbidden
        mockMvc.perform(get("/api/v1/invoices/" + invoiceIdB)
                        .header("Authorization", "Bearer " + staffTokenA))
                .andExpect(status().isForbidden());

        // 3. Staff A tries to create invoice for Client B -> 403 Forbidden
        mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + staffTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invReqB)))
                .andExpect(status().isForbidden());

        // 4. Staff A creates and views invoice for Client A -> 201 Created & 200 OK
        CreateInvoiceRequest invReqA = CreateInvoiceRequest.builder()
                .clientId(clientA.getId())
                .locationId(locA.getId())
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(15))
                .items(List.of(CreateInvoiceItemRequest.builder()
                        .service(BillingServiceType.CONSULTING)
                        .description("Mumbai location tax advisory")
                        .quantity(BigDecimal.ONE)
                        .unitPrice(new BigDecimal("5000.00"))
                        .build()))
                .build();

        String respA = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + staffTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invReqA)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID invoiceIdA = UUID.fromString(objectMapper.readTree(respA).path("data").path("id").asText());

        mockMvc.perform(get("/api/v1/invoices/" + invoiceIdA)
                        .header("Authorization", "Bearer " + staffTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clientName").value("Alpha Traders Pvt Ltd"));
    }

    @Test
    @DisplayName("Client Portal: Portal user of Client A cannot view invoices of Client B")
    void testClientPortalUserScoping() throws Exception {
        // Admin creates invoice for Client B
        CreateInvoiceRequest invReqB = CreateInvoiceRequest.builder()
                .clientId(clientB.getId())
                .locationId(locB.getId())
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(15))
                .items(List.of(CreateInvoiceItemRequest.builder()
                        .service(BillingServiceType.GST_FILING)
                        .quantity(BigDecimal.ONE)
                        .unitPrice(new BigDecimal("8000.00"))
                        .build()))
                .build();

        String respB = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invReqB)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID invoiceIdB = UUID.fromString(objectMapper.readTree(respB).path("data").path("id").asText());

        // Portal User A tries to view Invoice B -> 403 Forbidden
        mockMvc.perform(get("/api/v1/invoices/" + invoiceIdB)
                        .header("Authorization", "Bearer " + portalTokenA))
                .andExpect(status().isForbidden());
    }
}
