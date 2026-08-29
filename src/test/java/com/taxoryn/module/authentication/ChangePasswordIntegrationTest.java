package com.taxoryn.module.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.authentication.dto.ChangePasswordRequest;
import com.taxoryn.module.authentication.dto.LoginRequest;
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

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChangePasswordIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private UserEntity testUser;
    private OrganizationEntity testOrg;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Security Test Practice")
                .email("security@taxpractice.com")
                .phone("+919876543210")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .organizationId(testOrg.getId())
                .email("practitioner@taxpractice.com")
                .passwordHash(passwordEncoder.encode("CurrentPassword123!"))
                .firstName("Rohan")
                .lastName("Sharma")
                .phone("+919876543210")
                .status(UserStatus.ACTIVE)
                .build());

        jwtToken = jwtTokenProvider.generateAccessToken(
                testUser.getId(),
                testOrg.getId(),
                null,
                testUser.getEmail(),
                Set.of("PRACTITIONER"),
                Collections.emptySet()
        );
    }

    @Test
    @DisplayName("Should successfully change password with valid current and new passwords")
    void testChangePassword_Success() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "CurrentPassword123!",
                "BrandNewSecurePass456!",
                "BrandNewSecurePass456!"
        );

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Your password has been changed successfully."));

        // Verify password hash updated in DB
        UserEntity updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("BrandNewSecurePass456!", updatedUser.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("CurrentPassword123!", updatedUser.getPasswordHash())).isFalse();

        // Verify login succeeds with new password
        LoginRequest loginRequest = new LoginRequest(testUser.getEmail(), "BrandNewSecurePass456!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Should reject change password when current password is incorrect")
    void testChangePassword_IncorrectCurrentPassword() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "WrongCurrentPassword999!",
                "BrandNewSecurePass456!",
                "BrandNewSecurePass456!"
        );

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        // Verify original password is unchanged
        UserEntity user = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("CurrentPassword123!", user.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Should reject change password when new password and confirm password do not match")
    void testChangePassword_PasswordMismatch() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "CurrentPassword123!",
                "BrandNewSecurePass456!",
                "DifferentConfirmPass789!"
        );

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject change password when new password is same as current password")
    void testChangePassword_SamePassword() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "CurrentPassword123!",
                "CurrentPassword123!",
                "CurrentPassword123!"
        );

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject change password when new password does not meet complexity standards")
    void testChangePassword_WeakPassword() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "CurrentPassword123!",
                "weak",
                "weak"
        );

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when request is unauthenticated")
    void testChangePassword_Unauthenticated() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "CurrentPassword123!",
                "BrandNewSecurePass456!",
                "BrandNewSecurePass456!"
        );

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should record PASSWORD_CHANGED audit event upon successful password change")
    void testChangePassword_AuditRecorded() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "CurrentPassword123!",
                "BrandNewSecurePass456!",
                "BrandNewSecurePass456!"
        );

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> "PASSWORD_CHANGED".equals(log.getAction())
                        && testUser.getId().equals(log.getUserId()));
    }
}