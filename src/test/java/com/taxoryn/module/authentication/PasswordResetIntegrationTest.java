package com.taxoryn.module.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.module.authentication.dto.ForgotPasswordRequest;
import com.taxoryn.module.authentication.dto.LoginRequest;
import com.taxoryn.module.authentication.dto.RefreshTokenRequest;
import com.taxoryn.module.authentication.dto.ResetPasswordRequest;
import com.taxoryn.module.authentication.entity.PasswordResetTokenEntity;
import com.taxoryn.module.authentication.entity.RefreshTokenEntity;
import com.taxoryn.module.authentication.repository.PasswordResetTokenRepository;
import com.taxoryn.module.authentication.repository.RefreshTokenRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import com.taxoryn.module.notification.email.service.EmailNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailNotificationService emailNotificationService;

    private UserEntity testUser;
    private OrganizationEntity testOrg;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Recovery Test Practice")
                .email("contact@recoverypractice.com")
                .phone("+919876543210")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .organizationId(testOrg.getId())
                .email("practitioner@recoverypractice.com")
                .passwordHash(passwordEncoder.encode("OldPassword123!"))
                .firstName("Aditya")
                .lastName("Verma")
                .phone("+919876543210")
                .status(UserStatus.ACTIVE)
                .build());
    }

    @Test
    @DisplayName("A. Happy path: Request password reset and complete reset with new password")
    void testForgotPasswordAndReset_HappyPath() throws Exception {
        // 1. Request password reset
        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest(testUser.getEmail());
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If an account exists for this email, you will receive password reset instructions."));

        List<PasswordResetTokenEntity> tokens = passwordResetTokenRepository.findAllByUserIdAndUsedAtIsNull(testUser.getId());
        assertThat(tokens).hasSize(1);
        PasswordResetTokenEntity token = tokens.get(0);
        assertThat(token.getTokenHash()).hasSize(64);
        assertThat(token.getExpiresAt()).isAfter(Instant.now());
        assertThat(token.getUsedAt()).isNull();

        // 2. Perform reset using a known raw token
        String rawToken = "happy-path-test-raw-token-1234567890";
        String tokenHash = hashToken(rawToken);
        token.setTokenHash(tokenHash);
        passwordResetTokenRepository.save(token);

        ResetPasswordRequest resetRequest = new ResetPasswordRequest(rawToken, "BrandNewSecurePassword456!");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password has been reset successfully. You can now log in with your new password."));

        // Verify token is now marked as used
        PasswordResetTokenEntity updatedToken = passwordResetTokenRepository.findById(token.getId()).orElseThrow();
        assertThat(updatedToken.isUsed()).isTrue();
        assertThat(updatedToken.getUsedAt()).isNotNull();

        // 3. Verify login succeeds with new password
        LoginRequest loginRequest = new LoginRequest(testUser.getEmail(), "BrandNewSecurePassword456!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("B. Old password rejection: Old password fails login immediately after reset")
    void testResetPassword_OldPasswordFails() throws Exception {
        String rawToken = "old-pwd-test-token-12345";
        passwordResetTokenRepository.save(PasswordResetTokenEntity.builder()
                .userId(testUser.getId())
                .tokenHash(hashToken(rawToken))
                .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                .createdByIp("127.0.0.1")
                .build());

        ResetPasswordRequest resetRequest = new ResetPasswordRequest(rawToken, "BrandNewSecurePassword456!");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk());

        // Attempting to log in with old password MUST fail
        LoginRequest oldLoginRequest = new LoginRequest(testUser.getEmail(), "OldPassword123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldLoginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("C. Token reuse rejection: Reusing an already consumed token fails (single-use)")
    void testResetPassword_TokenReuseRejected() throws Exception {
        String rawToken = "reuse-test-token-12345";
        passwordResetTokenRepository.save(PasswordResetTokenEntity.builder()
                .userId(testUser.getId())
                .tokenHash(hashToken(rawToken))
                .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                .createdByIp("127.0.0.1")
                .build());

        ResetPasswordRequest resetRequest = new ResetPasswordRequest(rawToken, "BrandNewSecurePassword456!");
        // First reset -> SUCCESS
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk());

        // Second reset with SAME token -> MUST FAIL (401 Unauthorized)
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("D. Expired token rejection: Using an expired token fails")
    void testResetPassword_ExpiredTokenRejected() throws Exception {
        String rawToken = "expired-token-12345";
        passwordResetTokenRepository.save(PasswordResetTokenEntity.builder()
                .userId(testUser.getId())
                .tokenHash(hashToken(rawToken))
                .expiresAt(Instant.now().minus(10, ChronoUnit.MINUTES)) // expired
                .createdByIp("127.0.0.1")
                .build());

        ResetPasswordRequest request = new ResetPasswordRequest(rawToken, "BrandNewSecurePassword456!");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("E. Invalid token rejection: Random, truncated, or modified tokens are rejected")
    void testResetPassword_InvalidTokenRejected() throws Exception {
        ResetPasswordRequest randomRequest = new ResetPasswordRequest("completely-non-existent-random-token", "BrandNewSecurePassword456!");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(randomRequest)))
                .andExpect(status().isUnauthorized());

        ResetPasswordRequest blankTokenRequest = new ResetPasswordRequest("   ", "BrandNewSecurePassword456!");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankTokenRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("F. Multiple reset requests: Generating Token B invalidates Token A")
    void testForgotPassword_MultipleRequests_InvalidatesPreviousTokens() throws Exception {
        // Request 1
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(testUser.getEmail()))))
                .andExpect(status().isOk());

        List<PasswordResetTokenEntity> firstTokens = passwordResetTokenRepository.findAll();
        assertThat(firstTokens).hasSize(1);
        PasswordResetTokenEntity tokenA = firstTokens.get(0);
        String rawTokenA = "raw-token-A-12345";
        tokenA.setTokenHash(hashToken(rawTokenA));
        passwordResetTokenRepository.save(tokenA);

        // Request 2 (generates Token B and invalidates Token A)
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(testUser.getEmail()))))
                .andExpect(status().isOk());

        List<PasswordResetTokenEntity> allTokens = passwordResetTokenRepository.findAll();
        assertThat(allTokens).hasSize(2);

        PasswordResetTokenEntity tokenB = allTokens.stream()
                .filter(t -> t.getUsedAt() == null)
                .findFirst()
                .orElseThrow();
        String rawTokenB = "raw-token-B-67890";
        tokenB.setTokenHash(hashToken(rawTokenB));
        passwordResetTokenRepository.save(tokenB);

        // Verify Token A is marked used/invalidated
        PasswordResetTokenEntity refreshedTokenA = passwordResetTokenRepository.findById(tokenA.getId()).orElseThrow();
        assertThat(refreshedTokenA.isUsed()).isTrue();

        // Reset with Token A MUST FAIL
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(rawTokenA, "NewSecurePassword456!"))))
                .andExpect(status().isUnauthorized());

        // Reset with Token B MUST SUCCEED
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(rawTokenB, "NewSecurePassword456!"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("G. Concurrent reset race condition: Exactly 1 concurrent request succeeds and 1 fails")
    void testResetPassword_ConcurrentAtomicConsumption_PreventsRaceCondition() throws Exception {
        String rawToken = "concurrent-race-test-token-12345";
        passwordResetTokenRepository.save(PasswordResetTokenEntity.builder()
                .userId(testUser.getId())
                .tokenHash(hashToken(rawToken))
                .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                .createdByIp("127.0.0.1")
                .build());

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        List<Integer> statusCodes = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // wait for simultaneous launch
                    ResetPasswordRequest request = new ResetPasswordRequest(rawToken, "NewSecurePassword456!");
                    MvcResult result = mockMvc.perform(post("/api/auth/reset-password")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andReturn();
                    statusCodes.add(result.getResponse().getStatus());
                } catch (Exception e) {
                    statusCodes.add(500);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // trigger simultaneous requests
        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(statusCodes).hasSize(2);
        // Exactly ONE request must succeed (200) and ONE must fail (401)
        assertThat(statusCodes).containsExactlyInAnyOrder(200, 401);
    }

    @Test
    @DisplayName("H. Password validation: Weak, short, or invalid passwords are rejected")
    void testResetPassword_WeakPasswordRejected() throws Exception {
        String rawToken = "weak-pwd-token-12345";
        passwordResetTokenRepository.save(PasswordResetTokenEntity.builder()
                .userId(testUser.getId())
                .tokenHash(hashToken(rawToken))
                .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                .createdByIp("127.0.0.1")
                .build());

        // Too short (< 8 chars)
        ResetPasswordRequest shortRequest = new ResetPasswordRequest(rawToken, "Short1!");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortRequest)))
                .andExpect(status().isBadRequest());

        // Missing special character
        ResetPasswordRequest noSpecialRequest = new ResetPasswordRequest(rawToken, "NoSpecialChar123");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noSpecialRequest)))
                .andExpect(status().isBadRequest());

        // Known weak dictionary password (password123!)
        ResetPasswordRequest weakDictionaryRequest = new ResetPasswordRequest(rawToken, "password123!");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(weakDictionaryRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("I. Anti-enumeration: Non-existent email returns generic success without creating tokens")
    void testForgotPassword_NonExistentEmail_AntiEnumeration() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("doesnotexist@nowhere.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If an account exists for this email, you will receive password reset instructions."));

        assertThat(passwordResetTokenRepository.count()).isZero();
    }

    @Test
    @DisplayName("J. Session invalidation: Existing active refresh tokens are revoked after password reset")
    void testResetPassword_RevokesActiveSessions() throws Exception {
        // 1. Create an active refresh token session before password reset
        String rawRefreshToken = "initial-session-refresh-token-12345";
        RefreshTokenEntity activeSession = refreshTokenRepository.save(RefreshTokenEntity.builder()
                .userId(testUser.getId())
                .organizationId(testOrg.getId())
                .tokenHash(hashToken(rawRefreshToken))
                .familyId(UUID.randomUUID())
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .createdByIp("127.0.0.1")
                .userAgent("Mozilla/5.0")
                .build());

        // 2. Perform password reset with valid reset token
        String rawResetToken = "session-revocation-reset-token-12345";
        passwordResetTokenRepository.save(PasswordResetTokenEntity.builder()
                .userId(testUser.getId())
                .tokenHash(hashToken(rawResetToken))
                .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                .createdByIp("127.0.0.1")
                .build());

        ResetPasswordRequest resetRequest = new ResetPasswordRequest(rawResetToken, "NewSecurePassword456!");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk());

        // 3. Verify the previous session refresh token was revoked
        RefreshTokenEntity revokedSession = refreshTokenRepository.findById(activeSession.getId()).orElseThrow();
        assertThat(revokedSession.isRevoked()).isTrue();
        assertThat(revokedSession.getRevokedReason()).isEqualTo("PASSWORD_RESET");

        // 4. Attempting to refresh tokens with the old session MUST fail (401 Unauthorized)
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(rawRefreshToken);
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized());
    }

    @Autowired
    private com.taxoryn.module.authentication.service.AuthService authService;

    @Test
    @DisplayName("K1. Security: Allowed Origin header does not alter reset URL; server configuration is strictly used")
    void testForgotPassword_AllowedOriginHeader_UsesConfiguredUrl() throws Exception {
        reset(emailNotificationService);

        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest(testUser.getEmail());
        mockMvc.perform(post("/api/auth/forgot-password")
                        .header("Origin", "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailNotificationService).sendPasswordResetEmail(
                eq(testUser.getEmail()),
                anyString(),
                urlCaptor.capture(),
                anyLong()
        );

        String generatedUrl = urlCaptor.getValue();
        assertThat(generatedUrl).isNotNull();
        assertThat(generatedUrl).startsWith("http://localhost:5173/reset-password?token=");
    }

    @Test
    @DisplayName("K2. Security: Cross-origin attacker Origin is rejected by CORS with 403 Forbidden")
    void testForgotPassword_AttackerOriginHeader_BlockedByCors() throws Exception {
        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest(testUser.getEmail());
        mockMvc.perform(post("/api/auth/forgot-password")
                        .header("Origin", "https://attacker.example")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("K3. Security: Direct service invocation strictly uses server configured reset URL")
    void testForgotPassword_DirectServiceCall_UsesConfiguredServerUrl() {
        reset(emailNotificationService);

        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest(testUser.getEmail());
        authService.forgotPassword(forgotRequest, "198.51.100.25");

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailNotificationService).sendPasswordResetEmail(
                eq(testUser.getEmail()),
                anyString(),
                urlCaptor.capture(),
                anyLong()
        );

        String generatedUrl = urlCaptor.getValue();
        assertThat(generatedUrl).isNotNull();
        assertThat(generatedUrl).startsWith("http://localhost:5173/reset-password?token=");
    }

    @Test
    @DisplayName("L. Security: Attacker Referer header is completely ignored and reset URL uses server configuration")
    void testForgotPassword_AttackerRefererHeader_Ignored() throws Exception {
        reset(emailNotificationService);

        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest(testUser.getEmail());
        mockMvc.perform(post("/api/auth/forgot-password")
                        .header("Referer", "https://evil-phishing.com/account/login?redirect=true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailNotificationService).sendPasswordResetEmail(
                eq(testUser.getEmail()),
                anyString(),
                urlCaptor.capture(),
                anyLong()
        );

        String generatedUrl = urlCaptor.getValue();
        assertThat(generatedUrl).isNotNull();
        assertThat(generatedUrl).doesNotContain("evil-phishing.com");
        assertThat(generatedUrl).startsWith("http://localhost:5173/reset-password?token=");
    }

    @Test
    @DisplayName("M. Security: Attacker Host header is completely ignored and reset URL uses server configuration")
    void testForgotPassword_AttackerHostHeader_Ignored() throws Exception {
        reset(emailNotificationService);

        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest(testUser.getEmail());
        mockMvc.perform(post("/api/auth/forgot-password")
                        .header("Host", "attacker-controlled-host.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailNotificationService).sendPasswordResetEmail(
                eq(testUser.getEmail()),
                anyString(),
                urlCaptor.capture(),
                anyLong()
        );

        String generatedUrl = urlCaptor.getValue();
        assertThat(generatedUrl).isNotNull();
        assertThat(generatedUrl).doesNotContain("attacker-controlled-host.com");
        assertThat(generatedUrl).startsWith("http://localhost:5173/reset-password?token=");
    }

    @Test
    @DisplayName("N. Security: Attacker X-Forwarded-Host header is completely ignored and reset URL uses server configuration")
    void testForgotPassword_AttackerXForwardedHostHeader_Ignored() throws Exception {
        reset(emailNotificationService);

        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest(testUser.getEmail());
        mockMvc.perform(post("/api/auth/forgot-password")
                        .header("X-Forwarded-Host", "malicious-forwarded-host.com")
                        .header("X-Forwarded-Proto", "https")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailNotificationService).sendPasswordResetEmail(
                eq(testUser.getEmail()),
                anyString(),
                urlCaptor.capture(),
                anyLong()
        );

        String generatedUrl = urlCaptor.getValue();
        assertThat(generatedUrl).isNotNull();
        assertThat(generatedUrl).doesNotContain("malicious-forwarded-host.com");
        assertThat(generatedUrl).startsWith("http://localhost:5173/reset-password?token=");
    }

    @Test
    @DisplayName("O. Security: Missing Origin/Referer headers strictly uses configured server reset URL")
    void testForgotPassword_MissingOriginAndReferer_UsesCanonicalConfiguredUrl() throws Exception {
        reset(emailNotificationService);

        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest(testUser.getEmail());
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailNotificationService).sendPasswordResetEmail(
                eq(testUser.getEmail()),
                anyString(),
                urlCaptor.capture(),
                anyLong()
        );

        String generatedUrl = urlCaptor.getValue();
        assertThat(generatedUrl).startsWith("http://localhost:5173/reset-password?token=");
    }

    @Test
    @DisplayName("P. Security: Missing reset-password-url configuration fails fast with IllegalStateException")
    void testForgotPassword_MissingResetPasswordUrlConfig_ThrowsException() {
        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest(testUser.getEmail());
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "resetPasswordBaseUrl", "");

        try {
            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> {
                authService.forgotPassword(forgotRequest, "127.0.0.1");
            });
        } finally {
            // Restore configuration for subsequent tests
            org.springframework.test.util.ReflectionTestUtils.setField(authService, "resetPasswordBaseUrl", "http://localhost:5173/reset-password");
        }
    }

    private String hashToken(String rawToken) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}