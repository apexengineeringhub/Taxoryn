package com.taxoryn.module.dsc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.dsc.dto.CreateDscRequest;
import com.taxoryn.module.dsc.dto.UpdateDscRequest;
import com.taxoryn.module.dsc.entity.DscEntity;
import com.taxoryn.module.dsc.model.DscCertificateType;
import com.taxoryn.module.dsc.model.DscStatus;
import com.taxoryn.module.dsc.repository.DscRepository;
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

import java.time.LocalDate;
import java.util.HashSet;
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
public class DscIntegrationTest {

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
    private DscRepository dscRepository;

    private OrganizationEntity orgA;
    private UserEntity userA;
    private String tokenA;

    private OrganizationEntity orgB;
    private UserEntity userB;
    private String tokenB;

    private ClientEntity clientA;

    @BeforeEach
    void setUp() {
        TenantContext.clear();

        RoleEntity adminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Organization Administrator")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        // Practice A
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Kapadia & Co - " + UUID.randomUUID())
                .legalName("Kapadia & Co LLP")
                .email("admin." + UUID.randomUUID() + "@kapadia.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        userA = userRepository.save(UserEntity.builder()
                .organizationId(orgA.getId())
                .email("admin." + UUID.randomUUID() + "@kapadia.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Aarav")
                .lastName("Kapadia")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        tokenA = jwtTokenProvider.generateAccessToken(
                userA.getId(),
                orgA.getId(),
                userA.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "CLIENT_VIEW", "CLIENT_READ", "ORGANIZATION_UPDATE", "ORG_WRITE", "CLIENT_UPDATE")
        );

        TenantContext.setTenantId(orgA.getId());
        ClientEntity clA = ClientEntity.builder()
                .displayName("Nova Enterprises Ltd")
                .clientType(ClientEntity.ClientType.COMPANY)
                .status(ClientEntity.ClientStatus.ACTIVE)
                .email("nova." + UUID.randomUUID() + "@example.com")
                .pan("NOVAA1234N")
                .build();
        clA.setOrganizationId(orgA.getId());
        clientA = clientRepository.save(clA);
        TenantContext.clear();

        // Practice B (Tenant Isolation)
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Deshmukh & Associates - " + UUID.randomUUID())
                .legalName("Deshmukh & Associates")
                .email("admin." + UUID.randomUUID() + "@deshmukh.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        userB = userRepository.save(UserEntity.builder()
                .organizationId(orgB.getId())
                .email("admin." + UUID.randomUUID() + "@deshmukh.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Dinesh")
                .lastName("Deshmukh")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        tokenB = jwtTokenProvider.generateAccessToken(
                userB.getId(),
                orgB.getId(),
                userB.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "CLIENT_VIEW", "CLIENT_READ", "ORGANIZATION_UPDATE", "ORG_WRITE")
        );

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("POST /api/v1/dsc creates DSC and GET /api/v1/dsc/{id} retrieves it with client link")
    void testCreateAndGetDsc() throws Exception {
        LocalDate today = LocalDate.now();
        CreateDscRequest request = CreateDscRequest.builder()
                .clientId(clientA.getId())
                .holderName("Vikramaditya Singhania")
                .certificateIdentifier("CERT-SN-882299")
                .certificateType(DscCertificateType.CLASS_3)
                .issuer("eMudhra")
                .issuedDate(today.minusMonths(2))
                .expiryDate(today.plusYears(2))
                .applicableServices("GST, ITR, MCA, TDS")
                .notes("Physical USB token in Safe #2")
                .build();

        String response = mockMvc.perform(post("/api/v1/dsc")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.holderName").value("Vikramaditya Singhania"))
                .andExpect(jsonPath("$.data.certificateIdentifier").value("CERT-SN-882299"))
                .andExpect(jsonPath("$.data.certificateType").value("CLASS_3"))
                .andExpect(jsonPath("$.data.issuer").value("eMudhra"))
                .andExpect(jsonPath("$.data.clientName").value("Nova Enterprises Ltd"))
                .andExpect(jsonPath("$.data.clientPan").value("NOVAA1234N"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();

        UUID dscId = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());

        // Get by ID
        mockMvc.perform(get("/api/v1/dsc/" + dscId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(dscId.toString()))
                .andExpect(jsonPath("$.data.holderName").value("Vikramaditya Singhania"));
    }

    @Test
    @DisplayName("Dynamic Expiry Status derivation: ACTIVE vs EXPIRING vs EXPIRED")
    void testDscExpiryStatusDerivation() throws Exception {
        LocalDate today = LocalDate.now();

        // 1. Expiring soon (10 days)
        CreateDscRequest expSoonReq = CreateDscRequest.builder()
                .holderName("Expiring Soon Holder")
                .issuedDate(today.minusYears(2))
                .expiryDate(today.plusDays(10))
                .build();

        mockMvc.perform(post("/api/v1/dsc")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expSoonReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("EXPIRING"))
                .andExpect(jsonPath("$.data.daysUntilExpiry").value(10));

        // 2. Already Expired in the past
        CreateDscRequest expiredReq = CreateDscRequest.builder()
                .holderName("Expired Holder")
                .issuedDate(today.minusYears(3))
                .expiryDate(today.minusDays(5))
                .build();

        mockMvc.perform(post("/api/v1/dsc")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expiredReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("EXPIRED"));
    }

    @Test
    @DisplayName("Multi-tenant isolation: Practice B cannot access or modify Practice A's DSC records")
    void testTenantIsolation() throws Exception {
        LocalDate today = LocalDate.now();
        CreateDscRequest request = CreateDscRequest.builder()
                .holderName("Practice A Partner DSC")
                .issuedDate(today.minusMonths(1))
                .expiryDate(today.plusYears(1))
                .build();

        String response = mockMvc.perform(post("/api/v1/dsc")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID dscId = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());

        // Practice B attempting to get Practice A's DSC -> 404 Not Found
        mockMvc.perform(get("/api/v1/dsc/" + dscId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // Practice B attempting to update Practice A's DSC -> 404 Not Found
        UpdateDscRequest updateReq = UpdateDscRequest.builder()
                .holderName("Hacked Holder")
                .build();
        mockMvc.perform(put("/api/v1/dsc/" + dscId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        // Practice B list does NOT contain Practice A's DSC
        mockMvc.perform(get("/api/v1/dsc")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == '" + dscId + "')]").doesNotExist());
    }

    @Test
    @DisplayName("Lifecycle actions: Activate, Deactivate, and Revoke with reason")
    void testLifecycleTransitions() throws Exception {
        LocalDate today = LocalDate.now();
        CreateDscRequest request = CreateDscRequest.builder()
                .holderName("Sunil Verma")
                .issuedDate(today.minusMonths(1))
                .expiryDate(today.plusYears(1))
                .build();

        String response = mockMvc.perform(post("/api/v1/dsc")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID dscId = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());

        // 1. Deactivate
        mockMvc.perform(patch("/api/v1/dsc/" + dscId + "/deactivate")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        // 2. Activate
        mockMvc.perform(patch("/api/v1/dsc/" + dscId + "/activate")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // 3. Revoke
        mockMvc.perform(patch("/api/v1/dsc/" + dscId + "/revoke")
                        .param("reason", "Token compromised")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVOKED"))
                .andExpect(jsonPath("$.data.notes").value(org.hamcrest.Matchers.containsString("Token compromised")));
    }

    @Test
    @DisplayName("GET /api/v1/dsc/summary returns accurate metrics breakdown")
    void testDscSummaryEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/dsc/summary")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.active").isNumber())
                .andExpect(jsonPath("$.data.expiringSoon").isNumber())
                .andExpect(jsonPath("$.data.expired").isNumber())
                .andExpect(jsonPath("$.data.revoked").isNumber());
    }

    @Test
    @DisplayName("RBAC: Read-only staff user without edit permission gets 403 Forbidden on create/update")
    void testRbacPermissions() throws Exception {
        RoleEntity staffRole = roleRepository.findByCodeAndIsSystemRoleTrue("STAFF")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("STAFF")
                        .name("Staff")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        UserEntity staffUser = userRepository.save(UserEntity.builder()
                .organizationId(orgA.getId())
                .email("staff." + UUID.randomUUID() + "@kapadia.com")
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

        CreateDscRequest request = CreateDscRequest.builder()
                .holderName("Unauthorized Attempt")
                .issuedDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(1))
                .build();

        // POST -> 403 Forbidden
        mockMvc.perform(post("/api/v1/dsc")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/dsc/trigger-expiry-reminders checks and dispatches alerts")
    void testTriggerExpiryRemindersEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/dsc/trigger-expiry-reminders")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isNumber());
    }
}
