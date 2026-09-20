package com.taxoryn.module.portal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.portal.dto.SendClientPortalMessageRequest;
import com.taxoryn.module.portal.repository.ClientPortalMessageRepository;
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClientPortalMessagingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientPortalMessageRepository clientPortalMessageRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID tenantId;
    private OrganizationEntity organization;
    private UserEntity practitionerUser;
    private String practitionerToken;

    private ClientEntity clientA;
    private UserEntity clientAUser;
    private String clientAToken;

    private UUID foreignTenantId;
    private OrganizationEntity foreignOrg;
    private ClientEntity clientForeign;
    private UserEntity clientForeignUser;
    private String clientForeignToken;

    @BeforeEach
    void setUp() {
        cleanData();

        organization = organizationRepository.save(OrganizationEntity.builder()
                .name("Chat Test Advisory LLP")
                .legalName("Chat Test Advisory LLP")
                .email("admin@chattest.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
        tenantId = organization.getId();
        TenantContext.setTenantId(tenantId);

        RoleEntity orgAdminRole = roleRepository.findByCodeAndOrganizationId("ORG_ADMIN", tenantId)
                .orElseGet(() -> {
                    RoleEntity r = RoleEntity.builder()
                            .code("ORG_ADMIN")
                            .name("Org Admin")
                            .description("Org Admin")
                            .permissions(new HashSet<>())
                            .build();
                    r.setOrganizationId(tenantId);
                    return roleRepository.save(r);
                });

        RoleEntity clientUserRole = roleRepository.findByCodeAndOrganizationId("CLIENT_USER", tenantId)
                .orElseGet(() -> {
                    RoleEntity r = RoleEntity.builder()
                            .code("CLIENT_USER")
                            .name("Client User")
                            .description("Client User")
                            .permissions(new HashSet<>())
                            .build();
                    r.setOrganizationId(tenantId);
                    return roleRepository.save(r);
                });

        practitionerUser = UserEntity.builder()
                .email("ca.sharma@chattest.com")
                .firstName("Sharma")
                .lastName("CA")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .roles(Set.of(orgAdminRole))
                .build();
        practitionerUser.setOrganizationId(tenantId);
        practitionerUser = userRepository.save(practitionerUser);

        practitionerToken = jwtTokenProvider.generateAccessToken(
                practitionerUser.getId(),
                tenantId,
                practitionerUser.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_UPDATE", "DOCUMENT_READ", "DOCUMENT_VIEW")
        );

        clientA = ClientEntity.builder()
                .displayName("Alpha Infotech Private Limited")
                .legalName("Alpha Infotech Private Limited")
                .clientType(ClientType.PRIVATE_LIMITED)
                .pan("AABCA1234F")
                .email("finance@alphainfotech.com")
                .build();
        clientA.setOrganizationId(tenantId);
        clientA = clientRepository.save(clientA);

        clientAUser = UserEntity.builder()
                .email("client.alpha@alphainfotech.com")
                .firstName("Rohan")
                .lastName("Verma")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .clientId(clientA.getId())
                .roles(Set.of(clientUserRole))
                .build();
        clientAUser.setOrganizationId(tenantId);
        clientAUser = userRepository.save(clientAUser);

        clientAToken = jwtTokenProvider.generateAccessToken(
                clientAUser.getId(),
                tenantId,
                clientA.getId(),
                clientAUser.getEmail(),
                Set.of("ROLE_CLIENT_USER"),
                Set.of("CLIENT_PORTAL_ACCESS", "CLIENT_PORTAL_DOCUMENT_VIEW", "CLIENT_PORTAL_DOCUMENT_UPLOAD")
        );

        // Foreign Tenant & Client
        foreignOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Foreign Advisory LLP")
                .legalName("Foreign Advisory LLP")
                .email("admin@foreign.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
        foreignTenantId = foreignOrg.getId();
        TenantContext.setTenantId(foreignTenantId);

        clientForeign = ClientEntity.builder()
                .displayName("Beta Corporation")
                .legalName("Beta Corporation")
                .clientType(ClientType.PROPRIETORSHIP)
                .pan("BBCPB5678G")
                .email("contact@betacorp.com")
                .build();
        clientForeign.setOrganizationId(foreignTenantId);
        clientForeign = clientRepository.save(clientForeign);

        clientForeignUser = UserEntity.builder()
                .email("beta@betacorp.com")
                .firstName("Beta")
                .lastName("Owner")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .clientId(clientForeign.getId())
                .roles(new HashSet<>())
                .build();
        clientForeignUser.setOrganizationId(foreignTenantId);
        clientForeignUser = userRepository.save(clientForeignUser);

        clientForeignToken = jwtTokenProvider.generateAccessToken(
                clientForeignUser.getId(),
                foreignTenantId,
                clientForeign.getId(),
                clientForeignUser.getEmail(),
                Set.of("ROLE_CLIENT_USER"),
                Set.of("CLIENT_PORTAL_ACCESS", "CLIENT_PORTAL_DOCUMENT_VIEW")
        );

        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        cleanData();
        TenantContext.clear();
    }

    private void cleanData() {
        TenantContext.clear();
        clientPortalMessageRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        clientRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("CHAT-001: Client can send consultation message and Practitioner can retrieve and reply")
    void testClientAndPractitionerConsultationChatFlow() throws Exception {
        // 1. Client sends a message
        SendClientPortalMessageRequest clientMsg = SendClientPortalMessageRequest.builder()
                .messageBody("Hello CA Sharma, our March 2026 sales ledger is finalized. Please check.")
                .build();

        mockMvc.perform(post("/api/v1/portal/messages")
                        .header("Authorization", "Bearer " + clientAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clientMsg)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.senderType", is("CLIENT")))
                .andExpect(jsonPath("$.data.senderName", is("Rohan Verma")))
                .andExpect(jsonPath("$.data.messageBody", containsString("March 2026 sales ledger")))
                .andExpect(jsonPath("$.data.readByClient", is(true)))
                .andExpect(jsonPath("$.data.readByPractice", is(false)));

        // 2. Practitioner retrieves messages for Client A
        mockMvc.perform(get("/api/v1/portal/clients/" + clientA.getId() + "/messages")
                        .header("Authorization", "Bearer " + practitionerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].messageBody", containsString("March 2026 sales ledger")));

        // 3. Practitioner checks unread count (should be 1)
        mockMvc.perform(get("/api/v1/portal/clients/" + clientA.getId() + "/messages/unread-count")
                        .header("Authorization", "Bearer " + practitionerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", is(1)));

        // 4. Practitioner marks messages as read
        mockMvc.perform(post("/api/v1/portal/clients/" + clientA.getId() + "/messages/read")
                        .header("Authorization", "Bearer " + practitionerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Unread count is now 0
        mockMvc.perform(get("/api/v1/portal/clients/" + clientA.getId() + "/messages/unread-count")
                        .header("Authorization", "Bearer " + practitionerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", is(0)));

        // 5. Practitioner replies to Client A
        SendClientPortalMessageRequest practiceReply = SendClientPortalMessageRequest.builder()
                .messageBody("Received Rohan. We will compute your GSTR-3B tax liability by tomorrow.")
                .build();

        mockMvc.perform(post("/api/v1/portal/clients/" + clientA.getId() + "/messages")
                        .header("Authorization", "Bearer " + practitionerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(practiceReply)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.senderType", is("PRACTICE")))
                .andExpect(jsonPath("$.data.senderName", is("Sharma CA")))
                .andExpect(jsonPath("$.data.messageBody", containsString("compute your GSTR-3B")))
                .andExpect(jsonPath("$.data.readByClient", is(false)))
                .andExpect(jsonPath("$.data.readByPractice", is(true)));

        // 6. Client checks unread count (should be 1)
        mockMvc.perform(get("/api/v1/portal/messages/unread-count")
                        .header("Authorization", "Bearer " + clientAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", is(1)));

        // 7. Client retrieves messages thread (has 2 messages)
        mockMvc.perform(get("/api/v1/portal/messages")
                        .header("Authorization", "Bearer " + clientAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].senderType", is("CLIENT")))
                .andExpect(jsonPath("$.data[1].senderType", is("PRACTICE")));

        // 8. Client marks messages as read
        mockMvc.perform(post("/api/v1/portal/messages/read")
                        .header("Authorization", "Bearer " + clientAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Unread count is now 0
        mockMvc.perform(get("/api/v1/portal/messages/unread-count")
                        .header("Authorization", "Bearer " + clientAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", is(0)));
    }

    @Test
    @DisplayName("CHAT-002: Rejects blank message with 400 Bad Request")
    void testBlankMessageValidation() throws Exception {
        SendClientPortalMessageRequest blankMsg = SendClientPortalMessageRequest.builder()
                .messageBody("   ")
                .build();

        mockMvc.perform(post("/api/v1/portal/messages")
                        .header("Authorization", "Bearer " + clientAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankMsg)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CHAT-003: Multi-tenant and cross-client isolation - Foreign client cannot access Client A messages")
    void testCrossClientAndTenantIsolation() throws Exception {
        // Send message in Client A
        SendClientPortalMessageRequest clientMsg = SendClientPortalMessageRequest.builder()
                .messageBody("Confidential tax data for Alpha Infotech")
                .build();

        mockMvc.perform(post("/api/v1/portal/messages")
                        .header("Authorization", "Bearer " + clientAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clientMsg)))
                .andExpect(status().isCreated());

        // Foreign client calls getClientMessages -> gets empty list (isolated to Beta Corp)
        mockMvc.perform(get("/api/v1/portal/messages")
                        .header("Authorization", "Bearer " + clientForeignToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // Foreign practitioner token cannot access Client A (belongs to another tenant)
        String foreignAdminToken = jwtTokenProvider.generateAccessToken(
                clientForeignUser.getId(),
                foreignTenantId,
                clientForeignUser.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_UPDATE")
        );

        mockMvc.perform(get("/api/v1/portal/clients/" + clientA.getId() + "/messages")
                        .header("Authorization", "Bearer " + foreignAdminToken))
                .andExpect(status().isNotFound());
    }
}
