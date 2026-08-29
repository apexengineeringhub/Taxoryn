package com.taxoryn.module.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.entity.ClientEntity;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClientImportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

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

    private OrganizationEntity org1;
    private OrganizationEntity org2;
    private UserEntity user1;
    private String token1;
    private UserEntity user2;
    private String token2;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        org1 = organizationRepository.save(OrganizationEntity.builder()
                .name("Alpha Tax Solutions")
                .email("alpha_admin@alphatax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        org2 = organizationRepository.save(OrganizationEntity.builder()
                .name("Beta Consultancy")
                .email("beta_admin@betatax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        RoleEntity adminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        TenantContext.setTenantId(org1.getId());

        user1 = userRepository.save(UserEntity.builder()
                .email("alpha_user@alphatax.com")
                .passwordHash(passwordEncoder.encode("SecurePass123!"))
                .firstName("Alpha")
                .lastName("Practitioner")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());

        token1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                user1.getId(),
                org1.getId(),
                user1.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_CREATE", "CLIENT_WRITE", "CLIENT_VIEW")
        );

        TenantContext.setTenantId(org2.getId());

        user2 = userRepository.save(UserEntity.builder()
                .email("beta_user@betatax.com")
                .passwordHash(passwordEncoder.encode("SecurePass123!"))
                .firstName("Beta")
                .lastName("Practitioner")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());

        token2 = "Bearer " + jwtTokenProvider.generateAccessToken(
                user2.getId(),
                org2.getId(),
                user2.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_CREATE", "CLIENT_WRITE", "CLIENT_VIEW")
        );

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Bulk Import: Clean batch with valid PAN, GSTIN, Individual and Corporate entities")
    void testBulkImport_Success() throws Exception {
        List<CreateClientRequest> batch = new ArrayList<>();

        batch.add(CreateClientRequest.builder()
                .displayName("Zenith Infotech Pvt Ltd")
                .legalName("Zenith Infotech Private Limited")
                .tradeName("Zenith Software")
                .pan("AAACZ1234D")
                .gstin("27AAACZ1234D1Z8")
                .clientType(ClientType.PRIVATE_LIMITED)
                .email("finance@zenithinfo.com")
                .phone("+91 98111 22233")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400 093")
                .build());

        batch.add(CreateClientRequest.builder()
                .displayName("Ramesh Kumar Sharma")
                .pan("ABCPK5678M")
                .clientType(ClientType.INDIVIDUAL)
                .email("ramesh.sharma@gmail.com")
                .phone("09822233344")
                .city("Pune")
                .state("Maharashtra")
                .pincode("411001")
                .build());

        mockMvc.perform(post("/api/v1/clients/bulk")
                        .header("Authorization", token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalProcessed", is(2)))
                .andExpect(jsonPath("$.data.totalSuccess", is(2)))
                .andExpect(jsonPath("$.data.totalFailed", is(0)))
                .andExpect(jsonPath("$.data.totalSkipped", is(0)))
                .andExpect(jsonPath("$.data.importedClients", hasSize(2)))
                .andExpect(jsonPath("$.data.importedClients[0].phone", is("9811122233")))
                .andExpect(jsonPath("$.data.importedClients[0].pincode", is("400093")))
                .andExpect(jsonPath("$.data.importedClients[1].phone", is("9822233344")));
    }

    @Test
    @DisplayName("Bulk Import: In-File Duplicate PAN detection")
    void testBulkImport_InFileDuplicatePan() throws Exception {
        List<CreateClientRequest> batch = new ArrayList<>();

        batch.add(CreateClientRequest.builder()
                .displayName("Prime Apex Corp")
                .pan("AAACZ9999D")
                .gstin("27AAACZ9999D1Z8")
                .clientType(ClientType.PRIVATE_LIMITED)
                .build());

        batch.add(CreateClientRequest.builder()
                .displayName("Prime Apex Branch 2")
                .pan("AAACZ9999D")
                .clientType(ClientType.PRIVATE_LIMITED)
                .build());

        mockMvc.perform(post("/api/v1/clients/bulk")
                        .header("Authorization", token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalProcessed", is(2)))
                .andExpect(jsonPath("$.data.totalSuccess", is(1)))
                .andExpect(jsonPath("$.data.totalSkipped", is(1)))
                .andExpect(jsonPath("$.data.errors[0].duplicate", is(true)))
                .andExpect(jsonPath("$.data.errors[0].field", is("PAN")))
                .andExpect(jsonPath("$.data.errors[0].reason", containsString("Duplicate PAN detected within the uploaded spreadsheet")));
    }

    @Test
    @DisplayName("Bulk Import: Pre-existing database duplicate PAN is safely skipped")
    void testBulkImport_ExistingDatabaseDuplicate() throws Exception {
        TenantContext.setTenantId(org1.getId());
        ClientEntity existing = ClientEntity.builder()
                .displayName("Existing Client Corp")
                .pan("AAACZ5555D")
                .clientType(ClientType.PRIVATE_LIMITED)
                .build();
        existing.setOrganizationId(org1.getId());
        clientRepository.save(existing);
        TenantContext.clear();

        List<CreateClientRequest> batch = new ArrayList<>();
        batch.add(CreateClientRequest.builder()
                .displayName("New Import Client")
                .pan("AAACZ5555D")
                .build());

        mockMvc.perform(post("/api/v1/clients/bulk")
                        .header("Authorization", token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalProcessed", is(1)))
                .andExpect(jsonPath("$.data.totalSuccess", is(0)))
                .andExpect(jsonPath("$.data.totalSkipped", is(1)))
                .andExpect(jsonPath("$.data.errors[0].duplicate", is(true)))
                .andExpect(jsonPath("$.data.errors[0].reason", containsString("already exists in practice")));
    }

    @Test
    @DisplayName("Bulk Import: PAN vs GSTIN mismatch error")
    void testBulkImport_PanGstinMismatch() throws Exception {
        List<CreateClientRequest> batch = new ArrayList<>();
        batch.add(CreateClientRequest.builder()
                .displayName("Mismatch Corp")
                .pan("ABCDE1234F")
                .gstin("27AAACZ1234D1Z8") // Embedded PAN is AAACZ1234D
                .build());

        mockMvc.perform(post("/api/v1/clients/bulk")
                        .header("Authorization", token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSuccess", is(0)))
                .andExpect(jsonPath("$.data.totalFailed", is(1)))
                .andExpect(jsonPath("$.data.errors[0].field", is("GSTIN / PAN")))
                .andExpect(jsonPath("$.data.errors[0].reason", containsString("does not match client PAN")));
    }

    @Test
    @DisplayName("Bulk Import: Invalid PAN format rejection with suggested correction")
    void testBulkImport_InvalidPan() throws Exception {
        List<CreateClientRequest> batch = new ArrayList<>();
        batch.add(CreateClientRequest.builder()
                .displayName("Invalid PAN Client")
                .pan("INVALID123")
                .build());

        mockMvc.perform(post("/api/v1/clients/bulk")
                        .header("Authorization", token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalFailed", is(1)))
                .andExpect(jsonPath("$.data.errors[0].field", is("PAN")))
                .andExpect(jsonPath("$.data.errors[0].suggestedCorrection", containsString("5 uppercase letters")));
    }

    @Test
    @DisplayName("Bulk Import: Tenant Isolation - Same PAN in two distinct organizations")
    void testBulkImport_TenantIsolation() throws Exception {
        List<CreateClientRequest> batch1 = List.of(CreateClientRequest.builder()
                .displayName("Alpha Client")
                .pan("AAACZ7777D")
                .build());

        List<CreateClientRequest> batch2 = List.of(CreateClientRequest.builder()
                .displayName("Beta Client")
                .pan("AAACZ7777D")
                .build());

        // Org 1 imports
        mockMvc.perform(post("/api/v1/clients/bulk")
                        .header("Authorization", token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batch1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSuccess", is(1)));

        // Org 2 imports the same PAN -> Should SUCCEED for Org 2 due to tenant isolation
        mockMvc.perform(post("/api/v1/clients/bulk")
                        .header("Authorization", token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batch2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSuccess", is(1)));
    }
}