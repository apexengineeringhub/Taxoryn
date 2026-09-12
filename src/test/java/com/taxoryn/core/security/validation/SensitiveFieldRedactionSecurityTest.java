package com.taxoryn.core.security.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.authentication.dto.ChangePasswordRequest;
import com.taxoryn.module.authentication.dto.RegisterOrganizationRequest;
import com.taxoryn.module.authentication.dto.ResetPasswordRequest;
import com.taxoryn.module.marketplace.dto.RegisterCustomerRequest;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SensitiveFieldRedactionSecurityTest {

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

    private OrganizationEntity testOrg;
    private UserEntity testAdmin;
    private String adminToken;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Redaction Org " + unique)
                .email("redaction.org." + unique + "@taxoryn.in")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Organization Admin")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        testAdmin = userRepository.save(UserEntity.builder()
                .organizationId(testOrg.getId())
                .email("admin." + unique + "@redactiontest.in")
                .firstName("Admin")
                .lastName("Tester")
                .passwordHash(passwordEncoder.encode("Tx9#SecureP@ss2026!"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build());

        adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                testAdmin.getId(),
                testOrg.getId(),
                testAdmin.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_READ", "CLIENT_WRITE", "USER_READ", "USER_WRITE")
        );
    }

    // =========================================================================
    // 1. UNIT TESTS: SensitiveFieldSanitizer
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {
            "password",
            "adminPassword",
            "currentPassword",
            "newPassword",
            "confirmPassword",
            "temporaryPassword",
            "token",
            "refreshToken",
            "resetToken",
            "activationToken",
            "secret",
            "clientSecret",
            "apiKey",
            "accessToken",
            "user.password",
            "request.items[0].apiKey",
            "pin",
            "otp"
    })
    @DisplayName("SensitiveFieldSanitizer identifies sensitive fields and sanitizes rejectedValue to null")
    void testSensitiveFieldSanitizer_IdentifiesAndRedacts(String fieldName) {
        assertTrue(SensitiveFieldSanitizer.isSensitiveField(fieldName),
                "Field must be identified as sensitive: " + fieldName);

        Object sanitized = SensitiveFieldSanitizer.sanitizeRejectedValue(fieldName, "TopSecretValue123!");
        assertNull(sanitized, "Rejected value for sensitive field must be sanitized to null");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "email",
            "organizationEmail",
            "adminEmail",
            "phone",
            "pan",
            "gstin",
            "firstName",
            "lastName",
            "organizationName",
            "status",
            "city",
            "state"
    })
    @DisplayName("SensitiveFieldSanitizer preserves non-sensitive rejected values for user guidance")
    void testSensitiveFieldSanitizer_PreservesNonSensitiveFields(String fieldName) {
        assertFalse(SensitiveFieldSanitizer.isSensitiveField(fieldName),
                "Field must not be identified as sensitive: " + fieldName);

        Object original = "normal-input-value";
        Object sanitized = SensitiveFieldSanitizer.sanitizeRejectedValue(fieldName, original);
        assertThat(sanitized).isEqualTo(original);
    }

    // =========================================================================
    // 2. INTEGRATION TESTS: Error Responses Redact Secrets
    // =========================================================================

    @Test
    @DisplayName("SEC-008: Organization registration validation error redacts adminPassword but preserves invalid email")
    void testOrgRegistration_RedactsSecretPassword() throws Exception {
        String sensitivePasswordAttempt = "MySecretWeakPass123!";
        String invalidEmail = "not-a-valid-email";

        RegisterOrganizationRequest request = RegisterOrganizationRequest.builder()
                .organizationName("Acme Advisors")
                .organizationEmail(invalidEmail)
                .adminFirstName("Rajesh")
                .adminLastName("Sharma")
                .adminEmail(invalidEmail)
                .adminPassword(sensitivePasswordAttempt)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register-organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();

        // Ensure the submitted password is NEVER present anywhere in the response JSON
        assertThat(responseJson).doesNotContain(sensitivePasswordAttempt);

        // Ensure non-sensitive field email retains rejectedValue for user guidance
        assertThat(responseJson).contains(invalidEmail);
    }

    @Test
    @DisplayName("SEC-008: Reset password validation error redacts newPassword and token secrets")
    void testResetPassword_RedactsNewPasswordAndToken() throws Exception {
        String sensitiveAttempt = "SecretBadPwd123!";
        ResetPasswordRequest request = new ResetPasswordRequest("   ", sensitiveAttempt);

        MvcResult result = mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();

        // The sensitive password value must NOT be present
        assertThat(responseJson).doesNotContain(sensitiveAttempt);
    }

    @Test
    @DisplayName("SEC-008: Change password validation error redacts currentPassword, newPassword, and confirmPassword")
    void testChangePassword_RedactsAllPasswordFields() throws Exception {
        String currentAttempt = "SecretCurrentPwd123!";
        String newAttempt = "SecretNewPwd123!";
        String confirmAttempt = "SecretConfirmPwd123!";

        ChangePasswordRequest request = new ChangePasswordRequest(
                currentAttempt,
                newAttempt,
                confirmAttempt
        );

        MvcResult result = mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();

        // NONE of the submitted passwords may appear in the response payload
        assertThat(responseJson).doesNotContain(currentAttempt);
        assertThat(responseJson).doesNotContain(newAttempt);
        assertThat(responseJson).doesNotContain(confirmAttempt);
    }

    @Test
    @DisplayName("SEC-008: Marketplace Customer registration validation error redacts password")
    void testMarketplaceCustomerRegistration_RedactsPassword() throws Exception {
        String sensitivePassword = "CustomerRawSecret123!";
        String invalidEmail = "invalid-cust-email";

        RegisterCustomerRequest request = RegisterCustomerRequest.builder()
                .firstName("Aditi")
                .lastName("Sharma")
                .email(invalidEmail)
                .password(sensitivePassword)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/marketplace/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();

        // Password MUST NOT be present in response
        assertThat(responseJson).doesNotContain(sensitivePassword);

        // Invalid email value is preserved
        assertThat(responseJson).contains(invalidEmail);
    }
}
