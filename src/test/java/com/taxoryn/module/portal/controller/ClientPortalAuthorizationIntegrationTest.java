package com.taxoryn.module.portal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.portal.repository.ClientDocumentRequestRepository;
import com.taxoryn.module.portal.repository.ClientNotificationRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClientPortalAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientRepository clientRepository;

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
    private ClientNotificationRepository notificationRepository;

    @Autowired
    private ClientDocumentRequestRepository docRequestRepository;

    private OrganizationEntity org;
    private ClientEntity client1;
    private ClientEntity client2;
    private UserEntity clientAdminUser;
    private UserEntity clientRegularUser;
    private String clientAdminToken;
    private String clientUserToken;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        docRequestRepository.deleteAll();
        notificationRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        // 1. Create Organization
        org = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Chartered Accountants")
                .email("contact@apexca.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(org.getId());

        // 2. Create Clients
        client1 = clientRepository.save(ClientEntity.builder()
                .clientType(ClientType.PRIVATE_LIMITED)
                .displayName("Alpha Tech Solutions")
                .legalName("Alpha Tech Solutions Pvt Ltd")
                .pan("AAACA1111A")
                .gstin("27AAACA1111A1Z1")
                .status(ClientStatus.ACTIVE)
                .build());

        client2 = clientRepository.save(ClientEntity.builder()
                .clientType(ClientType.PROPRIETORSHIP)
                .displayName("Beta Enterprises")
                .legalName("Beta Enterprises")
                .pan("BBBCB2222B")
                .status(ClientStatus.ACTIVE)
                .build());

        // 3. Create Roles
        RoleEntity clientAdminRole = roleRepository.save(RoleEntity.builder()
                .code("CLIENT_ADMIN")
                .name("Client Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        RoleEntity clientUserRole = roleRepository.save(RoleEntity.builder()
                .code("CLIENT_USER")
                .name("Client User")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        // 4. Create Portal Users linked to client1
        clientAdminUser = userRepository.save(UserEntity.builder()
                .email("admin@alphatech.com")
                .passwordHash(passwordEncoder.encode("ClientSecret123!"))
                .firstName("Raj")
                .lastName("Patel")
                .clientId(client1.getId())
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(clientAdminRole)))
                .build());

        clientRegularUser = userRepository.save(UserEntity.builder()
                .email("accounts@alphatech.com")
                .passwordHash(passwordEncoder.encode("ClientSecret123!"))
                .firstName("Meera")
                .lastName("Nair")
                .clientId(client1.getId())
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(clientUserRole)))
                .build());

        clientAdminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                clientAdminUser.getId(),
                org.getId(),
                client1.getId(),
                clientAdminUser.getEmail(),
                Set.of("CLIENT_ADMIN"),
                Set.of("CLIENT_PORTAL_ACCESS", "CLIENT_PORTAL_DOCUMENT_UPLOAD", "CLIENT_PORTAL_DOCUMENT_VIEW",
                        "CLIENT_PORTAL_PROFILE_VIEW", "CLIENT_PORTAL_PROFILE_UPDATE", "CLIENT_PORTAL_STATUS_VIEW")
        );

        clientUserToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                clientRegularUser.getId(),
                org.getId(),
                client1.getId(),
                clientRegularUser.getEmail(),
                Set.of("CLIENT_USER"),
                Set.of("CLIENT_PORTAL_ACCESS", "CLIENT_PORTAL_DOCUMENT_UPLOAD", "CLIENT_PORTAL_DOCUMENT_VIEW",
                        "CLIENT_PORTAL_PROFILE_VIEW", "CLIENT_PORTAL_STATUS_VIEW")
        );

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("1. Client Admin can access dashboard and view own profile")
    void testClientAdminCanAccessPortalDashboardAndProfile() throws Exception {
        mockMvc.perform(get("/api/v1/portal/dashboard")
                        .header("Authorization", clientAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.displayName").value("Alpha Tech Solutions"))
                .andExpect(jsonPath("$.data.pan").value("AAACA1111A"));

        mockMvc.perform(get("/api/v1/portal/profile")
                        .header("Authorization", clientAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Alpha Tech Solutions"));
    }

    @Test
    @DisplayName("2. Client User can access GST and ITR status")
    void testClientUserCanAccessPortalGstAndItrStatus() throws Exception {
        mockMvc.perform(get("/api/v1/portal/gst-status")
                        .header("Authorization", clientUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/portal/itr-status")
                        .header("Authorization", clientUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("3. Security Guard: Client portal user cannot access employee management (403 Forbidden)")
    void testClientUserCannotAccessEmployeeEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/employees")
                        .header("Authorization", clientUserToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Security Guard: Client portal user cannot access organization administration or roles (403 Forbidden)")
    void testClientUserCannotAccessOrganizationAdministration() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/current")
                        .header("Authorization", clientUserToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/roles")
                        .header("Authorization", clientUserToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("5. Security Guard: Client portal user cannot access internal client notes (403 Forbidden)")
    void testClientUserCannotAccessInternalNotes() throws Exception {
        mockMvc.perform(get("/api/v1/clients/" + client1.getId() + "/notes")
                        .header("Authorization", clientUserToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("6. Security Guard: Client portal user cannot access internal tasks directly (403 Forbidden)")
    void testClientUserCannotAccessInternalTasksDirectly() throws Exception {
        mockMvc.perform(get("/api/v1/tasks")
                        .header("Authorization", clientUserToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("7. Security Guard: Client portal user cannot access another client's details via general API (403 Forbidden)")
    void testClientUserCannotAccessOtherClientData() throws Exception {
        mockMvc.perform(get("/api/v1/clients/" + client2.getId())
                        .header("Authorization", clientUserToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("8. SECURITY: Client Admin cannot enumerate another client's portal users by ID (cross-client IDOR)")
    void testClientAdminCannotListAnotherClientsPortalUsers() throws Exception {
        // clientAdminToken belongs to client1 (Alpha Tech). Attempting to list portal users
        // for client2 (Beta Enterprises) - a different client in the SAME organization - must
        // be rejected, even though CLIENT_ADMIN is an allowed role for this endpoint.
        mockMvc.perform(get("/api/v1/portal/clients/" + client2.getId() + "/users")
                        .header("Authorization", clientAdminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("9. Client Admin CAN list portal users for their own client")
    void testClientAdminCanListOwnClientsPortalUsers() throws Exception {
        mockMvc.perform(get("/api/v1/portal/clients/" + client1.getId() + "/users")
                        .header("Authorization", clientAdminToken))
                .andExpect(status().isOk());
    }
}
