package com.taxoryn.module.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.module.authentication.dto.ForgotPasswordRequest;
import com.taxoryn.module.authentication.dto.LoginRequest;
import com.taxoryn.module.authentication.dto.ResetPasswordRequest;
import com.taxoryn.module.authentication.entity.PasswordResetTokenEntity;
import com.taxoryn.module.authentication.repository.PasswordResetTokenRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
    private PasswordEncoder passwordEncoder;

    private UserEntity testUser;
    private OrganizationEntity testOrg;

    @BeforeEach
    void setUp() {
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
    @DisplayName("Should successfully request password reset for existing active user")
    void testForgotPassword_ExistingUser() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest(testUser.getEmail());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If an account exists for this email, you will receive password reset instructions."));

        List<PasswordResetTokenEntity> tokens = passwordResetTokenRepository.findAllByUserIdAndUsedAtIsNull(testUser.getId());
        assertThat(tokens).hasSize(1);

        PasswordResetTokenEntity token = tokens.get(0);
        assertThat(token.getTokenHash()).hasSize(64);
        assertThat(token.getExpiresAt()).isAfter(Instant.now());
        assertThat(token.getUsedAt()).isNull();
    }

    @Test
    @DisplayName("Should return generic 200 response for non-existent email (anti-enumeration)")
    void testForgotPassword_NonExistentEmail() throws Exception {
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
    @DisplayName("Should successfully reset password using valid raw token and log in with new password")
    void testResetPassword_Success() throws Exception {
        String rawToken = "my-super-secret-secure-reset-token-1234567890";
        String tokenHash = hashToken(rawToken);

        PasswordResetTokenEntity token = passwordResetTokenRepository.save(PasswordResetTokenEntity.builder()
                .userId(testUser.getId())
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                .createdByIp("127.0.0.1")
                .build());

        ResetPasswordRequest request = new ResetPasswordRequest(rawToken, "NewSecurePassword456!");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password has been reset successfully. You can now log in with your new password."));

        // Verify token is now marked as used
        PasswordResetTokenEntity updatedToken = passwordResetTokenRepository.findById(token.getId()).orElseThrow();
        assertThat(updatedToken.isUsed()).isTrue();
        assertThat(updatedToken.getUsedAt()).isNotNull();

        // Verify user can log in with new password
        LoginRequest loginRequest = new LoginRequest(testUser.getEmail(), "NewSecurePassword456!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("Should reject password reset with invalid token")
    void testResetPassword_InvalidToken() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("completely-invalid-raw-token", "NewSecurePassword456!");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject password reset with expired token")
    void testResetPassword_ExpiredToken() throws Exception {
        String rawToken = "expired-token-123";
        String tokenHash = hashToken(rawToken);

        passwordResetTokenRepository.save(PasswordResetTokenEntity.builder()
                .userId(testUser.getId())
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().minus(5, ChronoUnit.MINUTES)) // expired 5 mins ago
                .createdByIp("127.0.0.1")
                .build());

        ResetPasswordRequest request = new ResetPasswordRequest(rawToken, "NewSecurePassword456!");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject password reset with already used token (single-use)")
    void testResetPassword_AlreadyUsedToken() throws Exception {
        String rawToken = "used-token-123";
        String tokenHash = hashToken(rawToken);

        passwordResetTokenRepository.save(PasswordResetTokenEntity.builder()
                .userId(testUser.getId())
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                .usedAt(Instant.now().minus(2, ChronoUnit.MINUTES)) // already used
                .createdByIp("127.0.0.1")
                .build());

        ResetPasswordRequest request = new ResetPasswordRequest(rawToken, "NewSecurePassword456!");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject reset password when password does not meet complexity standards")
    void testResetPassword_WeakPassword() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("some-valid-token", "weak"); // too short, missing requirements

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
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