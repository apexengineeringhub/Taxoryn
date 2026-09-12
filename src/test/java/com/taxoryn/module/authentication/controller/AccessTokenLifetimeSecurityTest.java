package com.taxoryn.module.authentication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.AuthCookieUtil;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.authentication.dto.ChangePasswordRequest;
import com.taxoryn.module.authentication.dto.LoginRequest;
import com.taxoryn.module.authentication.dto.LogoutRequest;
import com.taxoryn.module.authentication.dto.RefreshTokenRequest;
import com.taxoryn.module.authentication.dto.ResetPasswordRequest;
import com.taxoryn.module.authentication.entity.PasswordResetTokenEntity;
import com.taxoryn.module.authentication.entity.RefreshTokenEntity;
import com.taxoryn.module.authentication.repository.PasswordResetTokenRepository;
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
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccessTokenLifetimeSecurityTest {

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
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AuthCookieUtil authCookieUtil;

    @Value("${taxoryn.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    private OrganizationEntity testOrg;
    private UserEntity testUser;
    private final String testPassword = "SecureAccessTokenTest123!";

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        testOrg = OrganizationEntity.builder()
                .name("Access Token Hardening Org")
                .email("info.lifetime" + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build();
        testOrg = organizationRepository.save(testOrg);

        PermissionEntity viewPerm = permissionRepository.findByCode("NOTICE_VIEW")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder()
                        .code("NOTICE_VIEW")
                        .name("View Notices")
                        .module("NOTICE")
                        .build()));

        RoleEntity partnerRole = roleRepository.findByCodeAndIsSystemRoleTrue("CA_PARTNER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("CA_PARTNER")
                        .name("CA Partner")
                        .isSystemRole(true)
                        .permissions(new HashSet<>(Set.of(viewPerm)))
                        .build()));

        testUser = UserEntity.builder()
                .email("lifetime_user_" + UUID.randomUUID() + "@taxoryn.com")
                .passwordHash(passwordEncoder.encode(testPassword))
                .firstName("Lifetime")
                .lastName("Tester")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(partnerRole)))
                .build();
        testUser.setOrganizationId(testOrg.getId());
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("1. Access token expiration is configured to ~15 minutes (900,000 ms) instead of legacy 24 hours")
    void testAccessTokenLifetimeIs15Minutes() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testUser.getEmail())
                .password(testPassword)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        String accessToken = objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("accessToken").asText();
        assertNotNull(accessToken);

        Date expiration = jwtTokenProvider.getExpirationFromToken(accessToken);
        assertNotNull(expiration);

        long diffMs = expiration.getTime() - System.currentTimeMillis();
        // 15 minutes = 900,000 ms. Allow small execution window (890,000 - 905,000 ms)
        assertTrue(diffMs > 880000 && diffMs <= 905000,
                "Access token lifetime should be ~15 minutes (900,000 ms), but was: " + diffMs + " ms");
    }

    @Test
    @DisplayName("2. Active access token allows authorized access to protected endpoints")
    void testActiveAccessTokenAccessProtectedEndpoint() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testUser.getEmail())
                .password(testPassword)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.data.organizationId").value(testOrg.getId().toString()));
    }

    @Test
    @DisplayName("3. Expired access token is rejected with HTTP 401 Unauthorized")
    void testExpiredAccessTokenRejected() throws Exception {
        // Generate an expired access token
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Date past = new Date(System.currentTimeMillis() - 5000); // 5s ago
        Date pastIssued = new Date(past.getTime() - 900000);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", testUser.getId().toString());
        claims.put("organizationId", testOrg.getId().toString());
        claims.put("email", testUser.getEmail());
        claims.put("roles", Set.of("CA_PARTNER"));
        claims.put("permissions", Set.of("NOTICE_VIEW"));
        claims.put("tokenType", "ACCESS");

        String expiredToken = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer("taxoryn-platform")
                .subject(testUser.getId().toString())
                .claims(claims)
                .issuedAt(pastIssued)
                .expiration(past)
                .signWith(key)
                .compact();

        // Must return 401 Unauthorized
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("4. Expired access token is seamlessly recovered via silent refresh token rotation")
    void testSeamlessExpiredAccessTokenRecoveryViaRefresh() throws Exception {
        // Step 1: Login to obtain access token and HttpOnly refresh token cookie
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testUser.getEmail())
                .password(testPassword)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshCookie = loginResult.getResponse().getCookie(authCookieUtil.getCookieName());
        assertNotNull(refreshCookie);
        String bodyRefreshToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).path("data").path("refreshToken").asText();

        // Step 2: Simulate access token expiration -> client calls refresh endpoint
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(bodyRefreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andReturn();

        String newAccessToken = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).path("data").path("accessToken").asText();
        Cookie newRefreshCookie = refreshResult.getResponse().getCookie(authCookieUtil.getCookieName());
        assertNotNull(newRefreshCookie);
        assertNotEquals(refreshCookie.getValue(), newRefreshCookie.getValue(), "Refresh token must be rotated");

        // Verify new access token has 15-minute lifespan
        Date newExp = jwtTokenProvider.getExpirationFromToken(newAccessToken);
        long diffMs = newExp.getTime() - System.currentTimeMillis();
        assertTrue(diffMs > 880000 && diffMs <= 905000);

        // Step 3: Use fresh access token to access protected endpoint -> Success
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(testUser.getEmail()));
    }

    @Test
    @DisplayName("5. Refresh token reuse attack invalidates the entire session family")
    void testRefreshTokenReuseAttackRevokesSessionFamily() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testUser.getEmail())
                .password(testPassword)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String r1 = objectMapper.readTree(loginResult.getResponse().getContentAsString()).path("data").path("refreshToken").asText();

        // Legitimate rotation R1 -> R2
        MvcResult refresh1 = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(r1))))
                .andExpect(status().isOk())
                .andReturn();
        String r2 = objectMapper.readTree(refresh1.getResponse().getContentAsString()).path("data").path("refreshToken").asText();

        // Attacker replays consumed R1 -> REJECTED (401) and family revoked
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(r1))))
                .andExpect(status().isUnauthorized());

        // Legitimate client attempts to use active R2 -> REJECTED (401) because family was compromised
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(r2))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("6. Logout immediately invalidates access token and revokes refresh token")
    void testLogoutInvalidatesTokens() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testUser.getEmail())
                .password(testPassword)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).path("data").path("accessToken").asText();
        String refreshToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).path("data").path("refreshToken").asText();
        Cookie refreshCookie = loginResult.getResponse().getCookie(authCookieUtil.getCookieName());

        // Logout
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .cookie(refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LogoutRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));

        // Blacklisted access token cannot access protected endpoints
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());

        // Revoked refresh token cannot be refreshed
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("7. Password change immediately invalidates active refresh tokens")
    void testPasswordChangeInvalidatesRefreshTokens() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testUser.getEmail())
                .password(testPassword)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).path("data").path("accessToken").asText();
        String refreshToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).path("data").path("refreshToken").asText();

        // Change password
        ChangePasswordRequest changeReq = ChangePasswordRequest.builder()
                .currentPassword(testPassword)
                .newPassword("BrandNewPass123456!")
                .confirmPassword("BrandNewPass123456!")
                .build();

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeReq)))
                .andExpect(status().isOk());

        // Refresh token from before password change must be rejected
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("8. Password reset immediately invalidates active refresh tokens")
    void testPasswordResetInvalidatesRefreshTokens() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testUser.getEmail())
                .password(testPassword)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).path("data").path("refreshToken").asText();

        // Set up password reset token
        String rawResetToken = "raw-reset-token-lifetime-test-123";
        String tokenHash = hashSha256(rawResetToken);
        PasswordResetTokenEntity resetEntity = PasswordResetTokenEntity.builder()
                .userId(testUser.getId())
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                .build();
        passwordResetTokenRepository.save(resetEntity);

        // Perform password reset
        ResetPasswordRequest resetReq = ResetPasswordRequest.builder()
                .token(rawResetToken)
                .newPassword("FreshResetPass123456!")
                .build();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk());

        // Old refresh token must be rejected
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    private String hashSha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
