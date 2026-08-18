package com.taxoryn.module.authentication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TokenBlacklistService;
import com.taxoryn.module.authentication.dto.LoginRequest;
import com.taxoryn.module.authentication.dto.LogoutRequest;
import com.taxoryn.module.authentication.dto.RefreshTokenRequest;
import com.taxoryn.module.authentication.dto.RegisterUserByAdminRequest;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
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

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationSecurityIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    private OrganizationEntity org1;
    private OrganizationEntity org2;
    private UserEntity activeUserOrg1;
    private UserEntity disabledUserOrg1;
    private UserEntity userOrg2;
    private RoleEntity orgAdminRole;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        // 1. Create Organization 1 & Roles
        org1 = OrganizationEntity.builder()
                .name("Alpha Tax Advisory")
                .email("contact@alphatax.com")
                .status(OrganizationStatus.ACTIVE)
                .build();
        org1 = organizationRepository.save(org1);

        // 2. Create Organization 2
        org2 = OrganizationEntity.builder()
                .name("Beta Tax Consultants")
                .email("contact@betatax.com")
                .status(OrganizationStatus.ACTIVE)
                .build();
        org2 = organizationRepository.save(org2);

        orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Organization Admin")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        // 3. Create Active User in Org 1
        activeUserOrg1 = UserEntity.builder()
                .email("rajesh@alphatax.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Rajesh")
                .lastName("Kumar")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build();
        activeUserOrg1.setOrganizationId(org1.getId());
        activeUserOrg1 = userRepository.save(activeUserOrg1);

        // 4. Create Disabled User in Org 1
        disabledUserOrg1 = UserEntity.builder()
                .email("disabled@alphatax.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Inactive")
                .lastName("User")
                .status(UserStatus.INACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build();
        disabledUserOrg1.setOrganizationId(org1.getId());
        disabledUserOrg1 = userRepository.save(disabledUserOrg1);

        // 5. Create User in Org 2
        userOrg2 = UserEntity.builder()
                .email("sunil@betatax.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Sunil")
                .lastName("Mehta")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build();
        userOrg2.setOrganizationId(org2.getId());
        userOrg2 = userRepository.save(userOrg2);
    }

    @Test
    @DisplayName("1. Successful Login returns JWT access & refresh tokens with claims")
    void testSuccessfulLogin() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("rajesh@alphatax.com")
                .password("SecretPass123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("rajesh@alphatax.com"))
                .andExpect(jsonPath("$.data.organization.id").value(org1.getId().toString()));
    }

    @Test
    @DisplayName("2. Login with Invalid Password returns 401 UNAUTHORIZED")
    void testLoginInvalidPassword() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("rajesh@alphatax.com")
                .password("WrongPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("3. Login with Disabled/Inactive User returns 403 ACCOUNT_INACTIVE")
    void testLoginDisabledUser() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("disabled@alphatax.com")
                .password("SecretPass123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_INACTIVE"));
    }

    @Test
    @DisplayName("4. Access with Expired/Invalid JWT Token returns 401 UNAUTHORIZED")
    void testAccessWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid.expired.token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("5. Cross-Tenant Access is rejected with 403 TENANT_MISMATCH")
    void testCrossTenantAccessRejected() throws Exception {
        // Token belongs to User in Org 1
        String org1Token = "Bearer " + jwtTokenProvider.generateAccessToken(
                activeUserOrg1.getId(),
                org1.getId(),
                activeUserOrg1.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("ORG_READ", "ORG_WRITE", "USER_READ")
        );

        // User from Org 1 tries to access User details from Org 2
        mockMvc.perform(get("/api/v1/users/" + userOrg2.getId())
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // ResourceNotFound because tenant-isolated query returns empty

        // User from Org 1 tries to access Organization details of Org 2
        mockMvc.perform(get("/api/v1/organizations/" + org2.getId())
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("TENANT_MISMATCH"));
    }

    @Test
    @DisplayName("6. Current-User Endpoint /api/auth/me returns authenticated identity")
    void testGetCurrentUser() throws Exception {
        String token = "Bearer " + jwtTokenProvider.generateAccessToken(
                activeUserOrg1.getId(),
                org1.getId(),
                activeUserOrg1.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("ORG_READ")
        );

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("rajesh@alphatax.com"))
                .andExpect(jsonPath("$.data.organizationId").value(org1.getId().toString()));
    }

    @Test
    @DisplayName("7. User Registration by Org Admin adds member to admin's tenant")
    void testRegisterUserByOrgAdmin() throws Exception {
        String adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                activeUserOrg1.getId(),
                org1.getId(),
                activeUserOrg1.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("USER_WRITE")
        );

        RegisterUserByAdminRequest request = RegisterUserByAdminRequest.builder()
                .firstName("Pooja")
                .lastName("Sharma")
                .email("pooja@alphatax.com")
                .password("NewMemberPass123!")
                .phone("+919123456789")
                .roleCodes(Set.of("ORG_ADMIN"))
                .build();

        mockMvc.perform(post("/api/auth/register-user")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("pooja@alphatax.com"))
                .andExpect(jsonPath("$.data.organizationId").value(org1.getId().toString()));
    }

    @Test
    @DisplayName("8. Logout blacklists token and subsequent calls return 401")
    void testLogoutAndTokenInvalidation() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(
                activeUserOrg1.getId(),
                org1.getId(),
                activeUserOrg1.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("ORG_READ")
        );
        String authHeader = "Bearer " + token;

        // Verify token works before logout
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk());

        // Perform Logout
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LogoutRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify token is now invalid / blacklisted
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", authHeader))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("9. Refresh Token Endpoint /api/auth/refresh issues fresh tokens")
    void testRefreshToken() throws Exception {
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                activeUserOrg1.getId(),
                org1.getId(),
                activeUserOrg1.getEmail()
        );

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(refreshToken)
                .build();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString());
    }
}
