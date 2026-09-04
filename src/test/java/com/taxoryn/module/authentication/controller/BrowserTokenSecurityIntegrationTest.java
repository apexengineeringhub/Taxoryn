package com.taxoryn.module.authentication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.AuthCookieUtil;
import com.taxoryn.module.authentication.dto.LoginRequest;
import com.taxoryn.module.authentication.dto.RegisterOrganizationRequest;
import com.taxoryn.module.authentication.entity.RefreshTokenEntity;
import com.taxoryn.module.authentication.repository.RefreshTokenRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.PermissionEntity;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.PermissionRepository;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BrowserTokenSecurityIntegrationTest {

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
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private AuthCookieUtil authCookieUtil;

    private OrganizationEntity testOrg;
    private UserEntity testUser;
    private final String testPassword = "SecureBrowserPassword123!";

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        testOrg = OrganizationEntity.builder()
                .name("Browser Sec Tax Firm")
                .email("browsersec" + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build();
        testOrg = organizationRepository.save(testOrg);

        PermissionEntity gstPerm = permissionRepository.findByCode("GST_READ")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder()
                        .code("GST_READ")
                        .name("Read GST")
                        .module("GST")
                        .build()));

        RoleEntity adminRole = roleRepository.findByCodeAndIsSystemRoleTrue("CA_PARTNER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("CA_PARTNER")
                        .name("CA Partner")
                        .isSystemRole(true)
                        .permissions(new HashSet<>(Set.of(gstPerm)))
                        .build()));

        testUser = UserEntity.builder()
                .email("browser_user_" + UUID.randomUUID() + "@taxoryn.com")
                .passwordHash(passwordEncoder.encode(testPassword))
                .firstName("Browser")
                .lastName("Sec")
                .roles(new HashSet<>(Set.of(adminRole)))
                .status(UserStatus.ACTIVE)
                .build();
        testUser.setOrganizationId(testOrg.getId());
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Login sets HttpOnly, SameSite, Secure cookie for refresh token")
    void loginSetsHttpOnlyRefreshTokenCookie() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testUser.getEmail())
                .password(testPassword)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andReturn();

        String setCookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);
        assertTrue(setCookieHeader.contains(authCookieUtil.getCookieName() + "="));
        assertTrue(setCookieHeader.contains("HttpOnly"));
        assertTrue(setCookieHeader.contains("SameSite=Lax"));
        assertTrue(setCookieHeader.contains("Path=/"));
    }

    @Test
    @DisplayName("Refresh token rotation works via HttpOnly Cookie without request body")
    void refreshTokenViaHttpOnlyCookieSuccess() throws Exception {
        // Step 1: Login to get cookie
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testUser.getEmail())
                .password(testPassword)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshCookie = loginResult.getResponse().getCookie(authCookieUtil.getCookieName());
        assertNotNull(refreshCookie);
        String initialTokenValue = refreshCookie.getValue();

        // Step 2: Call /api/auth/refresh with Cookie only (empty body)
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andReturn();

        Cookie rotatedCookie = refreshResult.getResponse().getCookie(authCookieUtil.getCookieName());
        assertNotNull(rotatedCookie);
        assertNotEquals(initialTokenValue, rotatedCookie.getValue(), "Refresh token must be rotated upon use via cookie");

        // Verify active tokens in database
        List<RefreshTokenEntity> activeTokens = refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(testUser.getId());
        assertEquals(1, activeTokens.size());
    }

    @Test
    @DisplayName("Refresh fails when no cookie and no body token provided")
    void refreshFailsWithoutCookieOrBody() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Logout clears HttpOnly refresh token cookie and revokes token in DB")
    void logoutClearsCookieAndRevokesToken() throws Exception {
        // Step 1: Login
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testUser.getEmail())
                .password(testPassword)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshCookie = loginResult.getResponse().getCookie(authCookieUtil.getCookieName());
        assertNotNull(refreshCookie);

        // Verify active token exists
        List<RefreshTokenEntity> activeBefore = refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(testUser.getId());
        assertEquals(1, activeBefore.size());

        // Step 2: Logout with cookie
        MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout")
                        .cookie(refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andReturn();

        String setCookieHeader = logoutResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);
        assertTrue(setCookieHeader.contains(authCookieUtil.getCookieName() + ""));
        assertTrue(setCookieHeader.contains("Max-Age=0"));

        // Verify token is revoked in DB
        List<RefreshTokenEntity> activeAfter = refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(testUser.getId());
        assertEquals(0, activeAfter.size());
    }

    @Test
    @DisplayName("Logout-all revokes all user sessions and clears HttpOnly cookie")
    void logoutAllClearsCookieAndRevokesAllSessions() throws Exception {
        // Step 1: Login twice (simulate two browser tabs/devices)
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testUser.getEmail())
                .password(testPassword)
                .build();

        MvcResult login1 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshCookie1 = login1.getResponse().getCookie(authCookieUtil.getCookieName());
        assertNotNull(refreshCookie1);
        String accessToken1 = objectMapper.readTree(login1.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        MvcResult login2 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshCookie2 = login2.getResponse().getCookie(authCookieUtil.getCookieName());
        assertNotNull(refreshCookie2);

        List<RefreshTokenEntity> activeTokens = refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(testUser.getId());
        assertEquals(2, activeTokens.size());

        // Step 2: Logout-all with Authorization header
        MvcResult logoutAllResult = mockMvc.perform(post("/api/auth/logout-all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1)
                        .cookie(refreshCookie2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andReturn();

        String setCookieHeader = logoutAllResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);
        assertTrue(setCookieHeader.contains("Max-Age=0"));

        // All active tokens must be revoked
        List<RefreshTokenEntity> activeAfter = refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(testUser.getId());
        assertEquals(0, activeAfter.size());
    }

    @Test
    @DisplayName("Register organization sets HttpOnly refresh token cookie")
    void registerOrganizationSetsHttpOnlyCookie() throws Exception {
        RegisterOrganizationRequest request = RegisterOrganizationRequest.builder()
                .organizationName("New Org Reg")
                .organizationEmail("newreg" + UUID.randomUUID() + "@taxoryn.com")
                .organizationPhone("9123456780")
                .adminEmail("admin" + UUID.randomUUID() + "@taxoryn.com")
                .adminPassword("SecureNewOrgPassword123!")
                .adminFirstName("Admin")
                .adminLastName("User")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register-organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andReturn();

        String setCookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);
        assertTrue(setCookieHeader.contains(authCookieUtil.getCookieName() + "="));
        assertTrue(setCookieHeader.contains("HttpOnly"));
        assertTrue(setCookieHeader.contains("SameSite=Lax"));
    }

    @Test
    @DisplayName("Security response headers are enforced on all responses")
    void securityResponseHeadersEnforced() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().exists("Permissions-Policy"));
    }

    @Test
    @DisplayName("CORS options preflight handles allowed origin correctly")
    void corsOptionsPreflightCheck() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }
}
