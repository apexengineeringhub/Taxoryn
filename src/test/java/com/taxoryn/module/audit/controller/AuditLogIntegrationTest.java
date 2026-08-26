package com.taxoryn.module.audit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.entity.AuditLogEntity;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.dto.UpdateClientStatusRequest;
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

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditLogIntegrationTest {

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
    private ClientRepository clientRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity platformOrg;
    private OrganizationEntity tenantA;
    private OrganizationEntity tenantB;
    private UserEntity superAdminUser;
    private UserEntity userA;
    private UserEntity userB;
    private String superAdminToken;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        clientRepository.deleteAll();
        organizationRepository.deleteAll();

        RoleEntity superAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("TAXORYN_SUPERADMIN").orElseGet(() -> {
            RoleEntity r = RoleEntity.builder()
                    .code("TAXORYN_SUPERADMIN")
                    .name("Taxoryn Platform SuperAdmin")
                    .isSystemRole(true)
                    .build();
            return roleRepository.save(r);
        });

        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN").orElseGet(() -> {
            RoleEntity r = RoleEntity.builder()
                    .code("ORG_ADMIN")
                    .name("Organization Administrator")
                    .isSystemRole(true)
                    .build();
            return roleRepository.save(r);
        });

        // 0. Setup Platform Root Org & SuperAdmin
        platformOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Taxoryn Platform Global")
                .legalName("Taxoryn Global Inc")
                .email("platform." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        superAdminUser = userRepository.save(UserEntity.builder()
                .organizationId(platformOrg.getId())
                .email("superadmin." + UUID.randomUUID() + "@taxoryn.com")
                .passwordHash(passwordEncoder.encode("SecretPassword123!"))
                .firstName("Platform")
                .lastName("SuperAdmin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(superAdminRole)))
                .build());

        superAdminToken = jwtTokenProvider.generateAccessToken(
                superAdminUser.getId(), platformOrg.getId(), null, superAdminUser.getEmail(),
                Set.of("TAXORYN_SUPERADMIN", "SUPER_ADMIN"),
                Set.of("AUDIT_READ", "AUDIT_VIEW", "SECURITY_VIEW", "PLATFORM_VIEW")
        );

        // 1. Setup Tenant A
        tenantA = OrganizationEntity.builder()
                .name("Audit Tenant A " + UUID.randomUUID())
                .legalName("Tenant A Legal")
                .email("adminA." + UUID.randomUUID() + "@taxpractice.com")
                .status(OrganizationStatus.ACTIVE)
                .build();
        tenantA = organizationRepository.save(tenantA);

        TenantContext.setTenantId(tenantA.getId());
        userA = UserEntity.builder()
                .email(tenantA.getEmail())
                .passwordHash(passwordEncoder.encode("SecretPassword123!"))
                .firstName("Admin")
                .lastName("TenantA")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(orgAdminRole)))
                .build();
        userA.setOrganizationId(tenantA.getId());
        userA = userRepository.save(userA);

        tokenA = jwtTokenProvider.generateAccessToken(
                userA.getId(), tenantA.getId(), null, userA.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("AUDIT_READ", "CLIENT_CREATE", "CLIENT_VIEW", "CLIENT_UPDATE", "CLIENT_DELETE")
        );

        // 2. Setup Tenant B
        tenantB = OrganizationEntity.builder()
                .name("Audit Tenant B " + UUID.randomUUID())
                .legalName("Tenant B Legal")
                .email("adminB." + UUID.randomUUID() + "@taxpractice.com")
                .status(OrganizationStatus.ACTIVE)
                .build();
        tenantB = organizationRepository.save(tenantB);

        TenantContext.setTenantId(tenantB.getId());
        userB = UserEntity.builder()
                .email(tenantB.getEmail())
                .passwordHash(passwordEncoder.encode("SecretPassword123!"))
                .firstName("Admin")
                .lastName("TenantB")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(orgAdminRole)))
                .build();
        userB.setOrganizationId(tenantB.getId());
        userB = userRepository.save(userB);

        tokenB = jwtTokenProvider.generateAccessToken(
                userB.getId(), tenantB.getId(), null, userB.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("AUDIT_READ", "CLIENT_CREATE", "CLIENT_VIEW", "CLIENT_UPDATE", "CLIENT_DELETE")
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("GET /api/v1/audit-logs should return platform-wide audit logs across multiple organizations for SuperAdmin")
    void testSuperAdminCanViewPlatformWideAuditLogs() throws Exception {
        // Record audit logs across different tenants
        AuditLogEntity logA = auditLogRepository.save(AuditLogEntity.builder()
                .organizationId(tenantA.getId())
                .userId(userA.getId())
                .action("PRACTICE_VERIFIED")
                .entityType("PRACTICE")
                .entityName("PRACTICE")
                .entityId(tenantA.getId().toString())
                .ipAddress("10.0.0.1")
                .requestId("trace-superadmin-a")
                .createdAt(Instant.now())
                .build());

        AuditLogEntity logB = auditLogRepository.save(AuditLogEntity.builder()
                .organizationId(tenantB.getId())
                .userId(userB.getId())
                .action("APPLICATION_FEEDBACK_CREATED")
                .entityType("APPLICATION_FEEDBACK")
                .entityName("APPLICATION_FEEDBACK")
                .entityId(UUID.randomUUID().toString())
                .ipAddress("10.0.0.2")
                .requestId("trace-superadmin-b")
                .createdAt(Instant.now())
                .build());

        mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.data.content[0].displayAction").exists())
                .andExpect(jsonPath("$.data.content[0].displayEntityType").exists());
    }

    @Test
    @DisplayName("GET /api/audit-logs and /api/v1/audit-logs should return paginated audit records for Practice Admin")
    void testGetAuditLogsDualRouting() throws Exception {
        AuditLogEntity logEntity = AuditLogEntity.builder()
                .organizationId(tenantA.getId())
                .userId(userA.getId())
                .action("SYSTEM_CHECK")
                .entityType("SYSTEM")
                .entityName("SYSTEM")
                .entityId("SYS-001")
                .ipAddress("127.0.0.1")
                .requestId("req-trace-test-1")
                .createdAt(Instant.now())
                .build();
        auditLogRepository.save(logEntity);

        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].organizationId").value(tenantA.getId().toString()));

        mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Audit logs must strictly enforce tenant isolation for Practice Admins")
    void testStrictTenantIsolation() throws Exception {
        AuditLogEntity logA = AuditLogEntity.builder()
                .organizationId(tenantA.getId())
                .userId(userA.getId())
                .action("CLIENT_CREATED")
                .entityType("CLIENT")
                .entityId(UUID.randomUUID().toString())
                .ipAddress("10.0.0.1")
                .requestId("trace-a")
                .createdAt(Instant.now())
                .build();
        auditLogRepository.save(logA);

        AuditLogEntity logB = AuditLogEntity.builder()
                .organizationId(tenantB.getId())
                .userId(userB.getId())
                .action("CLIENT_CREATED")
                .entityType("CLIENT")
                .entityId(UUID.randomUUID().toString())
                .ipAddress("10.0.0.2")
                .requestId("trace-b")
                .createdAt(Instant.now())
                .build();
        auditLogRepository.save(logB);

        // Tenant A queries audit logs -> only sees Tenant A records
        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].organizationId").value(tenantA.getId().toString()));

        // Tenant B queries audit logs -> only sees Tenant B records
        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].organizationId").value(tenantB.getId().toString()));
    }

    @Test
    @DisplayName("Audit operations must automatically track client creation, update, and status change")
    void testClientOperationsAuditTrail() throws Exception {
        CreateClientRequest createReq = CreateClientRequest.builder()
                .displayName("Audit Test Corp")
                .clientType(ClientType.PRIVATE_LIMITED)
                .pan(String.format("ABCDE%04dF", (int) ((System.currentTimeMillis() / 1000) % 9000 + 1000)))
                .email("client." + UUID.randomUUID() + "@audit.com")
                .phone("9876543210")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String clientIdStr = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();
        UUID clientId = UUID.fromString(clientIdStr);

        UpdateClientStatusRequest statusReq = new UpdateClientStatusRequest();
        statusReq.setStatus(ClientStatus.INACTIVE);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/clients/" + clientId + "/status")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("entityId", clientId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.data.content[0].entityType").value("CLIENT"))
                .andExpect(jsonPath("$.data.content[0].entityId").value(clientId.toString()));
    }

    @Test
    @DisplayName("Audit trail API must be immutable: rejecting POST, PUT, DELETE operations")
    void testAuditLogApiImmutability() throws Exception {
        mockMvc.perform(post("/api/audit-logs")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"HACK\"}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(put("/api/audit-logs/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"HACK\"}"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(delete("/api/audit-logs/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().is4xxClientError());
    }
}
