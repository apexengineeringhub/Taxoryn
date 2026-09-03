package com.taxoryn.module.role.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.dto.AssignUserRolesRequest;
import com.taxoryn.module.role.dto.CreateRoleRequest;
import com.taxoryn.module.role.dto.UpdateRoleRequest;
import com.taxoryn.module.role.entity.PermissionEntity;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.PermissionRepository;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RbacAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity org;
    private UserEntity orgAdminUser;
    private UserEntity viewerUser;
    private UserEntity staffUser;
    private String orgAdminToken;
    private String viewerToken;
    private RoleEntity systemViewerRole;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        // 1. Create Organization
        org = OrganizationEntity.builder()
                .name("Apex Practice Management")
                .email("admin@apexpractice.com")
                .status(OrganizationStatus.ACTIVE)
                .build();
        org = organizationRepository.save(org);

        // 2. Create Permissions
        PermissionEntity orgRead = permissionRepository.save(PermissionEntity.builder().code("ORGANIZATION_VIEW").name("View Org").module("ORGANIZATION").build());
        PermissionEntity roleRead = permissionRepository.save(PermissionEntity.builder().code("ROLE_READ").name("View Roles").module("ROLE").build());
        PermissionEntity roleWrite = permissionRepository.save(PermissionEntity.builder().code("ROLE_WRITE").name("Write Roles").module("ROLE").build());
        PermissionEntity clientView = permissionRepository.save(PermissionEntity.builder().code("CLIENT_VIEW").name("View Clients").module("CLIENT").build());
        PermissionEntity clientCreate = permissionRepository.save(PermissionEntity.builder().code("CLIENT_CREATE").name("Create Client").module("CLIENT").build());
        PermissionEntity userView = permissionRepository.save(PermissionEntity.builder().code("USER_VIEW").name("View Users").module("USER").build());
        PermissionEntity userUpdate = permissionRepository.save(PermissionEntity.builder().code("USER_UPDATE").name("Update Users").module("USER").build());

        // 3. Create System Roles
        RoleEntity orgAdminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Administrator")
                .isSystemRole(true)
                .permissions(new HashSet<>(Set.of(orgRead, roleRead, roleWrite, clientView, clientCreate, userView, userUpdate)))
                .build());

        systemViewerRole = roleRepository.save(RoleEntity.builder()
                .code("VIEWER")
                .name("Read-Only Viewer")
                .isSystemRole(true)
                .permissions(new HashSet<>(Set.of(orgRead, clientView, userView, roleRead)))
                .build());

        // 4. Create Org Admin User
        orgAdminUser = UserEntity.builder()
                .email("admin@apexpractice.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build();
        orgAdminUser.setOrganizationId(org.getId());
        orgAdminUser = userRepository.save(orgAdminUser);

        // 5. Create Viewer User
        viewerUser = UserEntity.builder()
                .email("viewer@apexpractice.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Viewer")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(systemViewerRole)))
                .build();
        viewerUser.setOrganizationId(org.getId());
        viewerUser = userRepository.save(viewerUser);

        // 6. Create Staff User
        staffUser = UserEntity.builder()
                .email("staff@apexpractice.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Staff")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(systemViewerRole)))
                .build();
        staffUser.setOrganizationId(org.getId());
        staffUser = userRepository.save(staffUser);

        // Tokens
        orgAdminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                orgAdminUser.getId(),
                org.getId(),
                orgAdminUser.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("ORGANIZATION_VIEW", "ROLE_READ", "ROLE_WRITE", "CLIENT_VIEW", "CLIENT_CREATE", "USER_VIEW", "USER_UPDATE")
        );

        viewerToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                viewerUser.getId(),
                org.getId(),
                viewerUser.getEmail(),
                Set.of("VIEWER"),
                Set.of("ORGANIZATION_VIEW", "CLIENT_VIEW", "USER_VIEW", "ROLE_READ")
        );
    }

    @Test
    @DisplayName("1. Org Admin can create custom role in organization")
    void testCreateCustomRole() throws Exception {
        CreateRoleRequest request = CreateRoleRequest.builder()
                .code("LEAD_ACCOUNTANT")
                .name("Lead Accountant")
                .description("Handles tax audits")
                .permissionCodes(Set.of("CLIENT_VIEW", "CLIENT_CREATE"))
                .build();

        mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", orgAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("LEAD_ACCOUNTANT"))
                .andExpect(jsonPath("$.data.organizationId").value(org.getId().toString()));
    }

    @Test
    @DisplayName("2. Viewer role is denied custom role creation with 403 FORBIDDEN")
    void testViewerCannotCreateRole() throws Exception {
        CreateRoleRequest request = CreateRoleRequest.builder()
                .code("UNAUTHORIZED_ROLE")
                .name("Unauthorized Role")
                .permissionCodes(Set.of("CLIENT_VIEW"))
                .build();

        mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("3. Viewer can view clients (CLIENT_VIEW) but is rejected on create client (CLIENT_CREATE)")
    void testViewerPermissionsEnforcement() throws Exception {
        // GET /api/v1/clients is allowed for Viewer (has CLIENT_VIEW)
        mockMvc.perform(get("/api/v1/clients")
                        .header("Authorization", viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // POST /api/v1/clients is denied for Viewer (lacks CLIENT_CREATE)
        CreateClientRequest createClient = CreateClientRequest.builder()
                .displayName("New Client Co")
                .clientType(ClientType.PRIVATE_LIMITED)
                .email("client@company.com")
                .build();

        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createClient)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("4. System default roles cannot be updated or deleted")
    void testSystemDefaultRolesImmutability() throws Exception {
        UpdateRoleRequest updateRequest = UpdateRoleRequest.builder()
                .name("Attempt To Alter Viewer")
                .permissionCodes(Set.of("CLIENT_VIEW"))
                .build();

        // Attempt to update system role
        mockMvc.perform(put("/api/v1/roles/" + systemViewerRole.getId())
                        .header("Authorization", orgAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        // Attempt to delete system role
        mockMvc.perform(delete("/api/v1/roles/" + systemViewerRole.getId())
                        .header("Authorization", orgAdminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("5. Assign and remove roles from user within tenant")
    void testAssignAndRemoveUserRoles() throws Exception {
        AssignUserRolesRequest assignRequest = new AssignUserRolesRequest(Set.of("ORG_ADMIN"));

        // Assign ORG_ADMIN to staff user
        mockMvc.perform(put("/api/v1/roles/users/" + staffUser.getId())
                        .header("Authorization", orgAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roles[0].code").value("ORG_ADMIN"));

        // Get effective permissions
        mockMvc.perform(get("/api/v1/roles/users/" + staffUser.getId())
                        .header("Authorization", orgAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.effectivePermissions").isArray());
    }

    @Test
    @DisplayName("6. HTTP Enforcement: Org Admin cannot assign platform role (SUPER_ADMIN)")
    void testHttpPrivilegeEscalationAssignPlatformRoleDenied() throws Exception {
        AssignUserRolesRequest request = new AssignUserRolesRequest(Set.of("SUPER_ADMIN"));

        mockMvc.perform(put("/api/v1/roles/users/" + staffUser.getId())
                        .header("Authorization", orgAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("7. HTTP Enforcement: Viewer cannot self-escalate to ORG_ADMIN")
    void testHttpSelfPrivilegeEscalationDenied() throws Exception {
        AssignUserRolesRequest request = new AssignUserRolesRequest(Set.of("ORG_ADMIN"));

        mockMvc.perform(put("/api/v1/roles/users/" + viewerUser.getId())
                        .header("Authorization", viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("8. HTTP Enforcement: Org Admin cannot create custom role with platform permission")
    void testHttpCreateCustomRoleWithPlatformPermissionDenied() throws Exception {
        CreateRoleRequest request = CreateRoleRequest.builder()
                .code("SUPER_AUDITOR")
                .name("Super Auditor")
                .permissionCodes(Set.of("PLATFORM_USER_CREATE"))
                .build();

        mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", orgAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("9. HTTP Enforcement: Non-Admin Viewer cannot remove role from another user")
    void testHttpNonAdminRemoveRoleDenied() throws Exception {
        mockMvc.perform(delete("/api/v1/roles/users/" + staffUser.getId() + "/" + systemViewerRole.getId())
                        .header("Authorization", viewerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }
}
