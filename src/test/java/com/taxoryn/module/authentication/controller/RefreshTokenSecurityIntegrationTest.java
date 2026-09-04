package com.taxoryn.module.authentication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.authentication.dto.ChangePasswordRequest;
import com.taxoryn.module.authentication.dto.LoginRequest;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RefreshTokenSecurityIntegrationTest {

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

    private OrganizationEntity testOrg;
    private UserEntity testUser;
    private RoleEntity partnerRole;
    private PermissionEntity gstWritePerm;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        testOrg = OrganizationEntity.builder()
                .name("Apex Practice Management")
                .email("info@apexpractice.com")
                .status(OrganizationStatus.ACTIVE)
                .build();
        testOrg = organizationRepository.save(testOrg);

        gstWritePerm = permissionRepository.findByCode("GST_WRITE")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder()
                        .code("GST_WRITE")
                        .name("Write GST")
                        .module("GST")
                        .build()));

        partnerRole = roleRepository.findByCodeAndIsSystemRoleTrue("CA_PARTNER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("CA_PARTNER")
                        .name("CA Partner")
                        .isSystemRole(true)
                        .permissions(new HashSet<>(Set.of(gstWritePerm)))
                        .build()));

        testUser = UserEntity.builder()
                .email("partner@apexpractice.com")
                .passwordHash(passwordEncoder.encode("SecurePass123!"))
                .firstName("Vikram")
                .lastName("Aditya")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(partnerRole)))
                .build();
        testUser.setOrganizationId(testOrg.getId());
        testUser = userRepository.save(testUser);
    }

    private String performLoginAndGetRefreshToken(String email, String password) throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        String responseStr = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(responseStr).path("data").path("refreshToken").asText();
    }

    private String performLoginAndGetAccessToken(String email, String password) throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        String responseStr = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(responseStr).path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("SECURITY 1: Sequential Refresh Token Rotation (R1 -> R2 -> R3 -> R4)")
    void testSequentialRefreshTokenRotation() throws Exception {
        String r1 = performLoginAndGetRefreshToken("partner@apexpractice.com", "SecurePass123!");
        assertNotNull(r1);

        // Rotate R1 -> R2
        RefreshTokenRequest req1 = RefreshTokenRequest.builder().refreshToken(r1).build();
        String resp1 = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String r2 = objectMapper.readTree(resp1).path("data").path("refreshToken").asText();
        assertNotEquals(r1, r2);

        // Rotate R2 -> R3
        RefreshTokenRequest req2 = RefreshTokenRequest.builder().refreshToken(r2).build();
        String resp2 = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String r3 = objectMapper.readTree(resp2).path("data").path("refreshToken").asText();
        assertNotEquals(r2, r3);

        // Rotate R3 -> R4
        RefreshTokenRequest req3 = RefreshTokenRequest.builder().refreshToken(r3).build();
        String resp3 = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req3)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String r4 = objectMapper.readTree(resp3).path("data").path("refreshToken").asText();
        assertNotEquals(r3, r4);

        // Check DB: all 4 tokens exist under the same family ID
        List<RefreshTokenEntity> tokens = refreshTokenRepository.findAll();
        assertEquals(4, tokens.size());
        UUID familyId = tokens.get(0).getFamilyId();
        assertTrue(tokens.stream().allMatch(t -> t.getFamilyId().equals(familyId)));
    }

    @Test
    @DisplayName("SECURITY 2: Deep Token Reuse Attack Invalidation (Replaying R1 after R3 invalidates R4)")
    void testDeepTokenReuseAttackInvalidation() throws Exception {
        String r1 = performLoginAndGetRefreshToken("partner@apexpractice.com", "SecurePass123!");

        // Rotate R1 -> R2 -> R3
        String r2 = objectMapper.readTree(mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(r1))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").path("refreshToken").asText();

        String r3 = objectMapper.readTree(mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(r2))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").path("refreshToken").asText();

        // Attacker replays ancient token R1
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(r1))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        // Now legitimate client attempts to use active R3 -> REJECTED (401) because family was revoked
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(r3))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("SECURITY 3: Concurrent Refresh Race Condition Protection (Only 1 succeeds)")
    void testConcurrentRefreshRaceCondition() throws Exception {
        String r1 = performLoginAndGetRefreshToken("partner@apexpractice.com", "SecurePass123!");

        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Callable<Integer>> tasks = List.of(
                () -> executeRefreshWithLatch(r1, startLatch, successCount, failureCount),
                () -> executeRefreshWithLatch(r1, startLatch, successCount, failureCount),
                () -> executeRefreshWithLatch(r1, startLatch, successCount, failureCount),
                () -> executeRefreshWithLatch(r1, startLatch, successCount, failureCount)
        );

        List<Future<Integer>> futures = tasks.stream().map(executor::submit).toList();
        startLatch.countDown(); // Release all threads concurrently

        for (Future<Integer> f : futures) {
            f.get();
        }
        executor.shutdown();

        // At most 1 request can succeed. All concurrent competitors must fail safely.
        assertEquals(1, successCount.get(), "Exactly one concurrent refresh request must succeed");
        assertEquals(3, failureCount.get(), "All other concurrent requests must fail");
    }

    private Integer executeRefreshWithLatch(String refreshToken, CountDownLatch latch, AtomicInteger success, AtomicInteger failure) {
        try {
            latch.await();
            int status = mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                    .andReturn().getResponse().getStatus();

            if (status == 200) {
                success.incrementAndGet();
            } else {
                failure.incrementAndGet();
            }
            return status;
        } catch (Exception e) {
            failure.incrementAndGet();
            return 500;
        }
    }

    @Test
    @DisplayName("SECURITY 4: Password Reset immediately invalidates active refresh tokens")
    void testPasswordResetInvalidatesRefreshTokens() throws Exception {
        String refreshToken = performLoginAndGetRefreshToken("partner@apexpractice.com", "SecurePass123!");

        // Set up password reset token
        String rawResetToken = "test-raw-reset-token-123456789";
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
                .newPassword("NewBrandPass123!")
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk());

        // Pre-reset refresh token must now be rejected
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("SECURITY 5: Password Change immediately invalidates active refresh tokens")
    void testPasswordChangeInvalidatesRefreshTokens() throws Exception {
        String accessToken = performLoginAndGetAccessToken("partner@apexpractice.com", "SecurePass123!");
        String refreshToken = performLoginAndGetRefreshToken("partner@apexpractice.com", "SecurePass123!");

        ChangePasswordRequest changeReq = ChangePasswordRequest.builder()
                .currentPassword("SecurePass123!")
                .newPassword("BrandNewPass456!")
                .confirmPassword("BrandNewPass456!")
                .build();

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeReq)))
                .andExpect(status().isOk());

        // Refresh token from before password change must be rejected
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("SECURITY 6: Server-side Role update is immediately re-derived upon token refresh")
    void testServerSideRoleChangeReflectedOnRefresh() throws Exception {
        String refreshToken = performLoginAndGetRefreshToken("partner@apexpractice.com", "SecurePass123!");

        // Add a new role directly in DB (e.g. ORG_ADMIN)
        RoleEntity adminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Org Admin")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        testUser.getRoles().add(adminRole);
        userRepository.save(testUser);

        // Perform refresh
        String refreshResp = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String newAccessToken = objectMapper.readTree(refreshResp).path("data").path("accessToken").asText();

        // Verify the newly issued access token contains ORG_ADMIN role re-derived from DB
        Set<String> roles = jwtTokenProvider.getRolesFromToken(newAccessToken);
        assertTrue(roles.contains("ORG_ADMIN"), "Rotated access token must reflect newly assigned server-side roles");
        assertTrue(roles.contains("CA_PARTNER"));
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
