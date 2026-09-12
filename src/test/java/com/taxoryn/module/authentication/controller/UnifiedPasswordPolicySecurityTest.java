package com.taxoryn.module.authentication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.PasswordSecurityUtils;
import com.taxoryn.core.security.validation.StrongPassword;
import com.taxoryn.core.security.validation.StrongPasswordValidator;
import com.taxoryn.module.authentication.dto.ActivateOrganizationRequest;
import com.taxoryn.module.authentication.dto.ChangePasswordRequest;
import com.taxoryn.module.authentication.dto.RegisterOrganizationRequest;
import com.taxoryn.module.authentication.dto.RegisterUserByAdminRequest;
import com.taxoryn.module.authentication.dto.ResetPasswordRequest;
import com.taxoryn.module.authentication.entity.PasswordResetTokenEntity;
import com.taxoryn.module.authentication.repository.PasswordResetTokenRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.marketplace.dto.RegisterCustomerRequest;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.portal.dto.RegisterClientPortalUserRequest;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import jakarta.validation.ConstraintValidatorContext;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UnifiedPasswordPolicySecurityTest {

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
    private ClientRepository clientRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity testOrg;
    private UserEntity testAdmin;
    private ClientEntity testClient;
    private String adminToken;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Unified Password Org " + unique)
                .email("unified.pwd.org." + unique + "@taxoryn.in")
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
                .email("admin." + unique + "@unifiedpwd.in")
                .firstName("Admin")
                .lastName("Tester")
                .passwordHash(passwordEncoder.encode("Tx9#SecureP@ss2026!"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build());

        testClient = clientRepository.save(ClientEntity.builder()
                .organizationId(testOrg.getId())
                .displayName("Client Corp " + unique)
                .email("client." + unique + "@corp.in")
                .clientType(ClientEntity.ClientType.COMPANY)
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
    // 1. CANONICAL VALIDATOR UNIT TESTS
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {
            "short1!",                // < 12 characters (7 chars)
            "Short1!Abc",             // < 12 characters (10 chars)
            "alllowercasepassword123!", // No uppercase
            "ALLUPPERCASEPASSWORD123!", // No lowercase
            "NoSpecialCharacters12345", // No special character
            "1234567890123456",       // Digits only
            "password123!",           // Known weak dictionary
            "Password123!",           // Known weak dictionary
            "admin123!",              // Known weak dictionary
            "superadmin123!",         // Known weak dictionary
            "taxoryn123!"             // Known weak dictionary
    })
    @DisplayName("PasswordSecurityUtils and StrongPasswordValidator reject invalid / weak passwords")
    void testPasswordSecurityUtils_RejectsNonCompliantPasswords(String invalidPassword) {
        assertFalse(PasswordSecurityUtils.isStrongProductionPassword(invalidPassword),
                "Password must be rejected: " + invalidPassword);

        StrongPasswordValidator validator = new StrongPasswordValidator();
        StrongPassword annotation = mock(StrongPassword.class);
        validator.initialize(annotation);

        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        assertFalse(validator.isValid(invalidPassword, context),
                "StrongPasswordValidator must reject: " + invalidPassword);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Tx9#SecureP@ss2026!",
            "ValidStrongPass123!",
            "BrandNewSecurePass456!",
            "K#9xL2$vM8!pQz7@",
            "AnotherVeryStrongPassword2026$"
    })
    @DisplayName("PasswordSecurityUtils and StrongPasswordValidator accept valid 12+ compliant passwords")
    void testPasswordSecurityUtils_AcceptsCompliantPasswords(String validPassword) {
        assertTrue(PasswordSecurityUtils.isStrongProductionPassword(validPassword),
                "Password must be accepted: " + validPassword);

        StrongPasswordValidator validator = new StrongPasswordValidator();
        StrongPassword annotation = mock(StrongPassword.class);
        validator.initialize(annotation);

        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        assertTrue(validator.isValid(validPassword, context),
                "StrongPasswordValidator must accept: " + validPassword);
    }

    // =========================================================================
    // 2. INTEGRATION TESTS: ORGANIZATION REGISTRATION
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {
            "Short1!",
            "alllowercase123!",
            "ALLUPPERCASE123!",
            "NoSpecial123456",
            "password123!"
    })
    @DisplayName("POST /api/auth/register rejects non-compliant admin passwords")
    void testOrgRegistration_RejectsNonCompliantPasswords(String weakPassword) throws Exception {
        RegisterOrganizationRequest request = RegisterOrganizationRequest.builder()
                .organizationName("Security Test Practice")
                .organizationEmail("sec." + UUID.randomUUID() + "@practice.in")
                .adminFirstName("Rajesh")
                .adminLastName("Sharma")
                .adminEmail("admin." + UUID.randomUUID() + "@practice.in")
                .adminPassword(weakPassword)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register accepts valid 12+ compliant password")
    void testOrgRegistration_AcceptsValidPassword() throws Exception {
        RegisterOrganizationRequest request = RegisterOrganizationRequest.builder()
                .organizationName("Security Test Practice " + UUID.randomUUID().toString().substring(0, 6))
                .organizationEmail("valid.sec." + UUID.randomUUID() + "@practice.in")
                .adminFirstName("Rajesh")
                .adminLastName("Sharma")
                .adminEmail("valid.admin." + UUID.randomUUID() + "@practice.in")
                .adminPassword("Tx9#SecureP@ss2026!")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // 3. INTEGRATION TESTS: PASSWORD RESET FLOW
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {
            "Short1!",
            "alllowercase123!",
            "NoSpecial123456",
            "password123!"
    })
    @DisplayName("POST /api/auth/reset-password rejects non-compliant reset passwords")
    void testResetPassword_RejectsNonCompliantPasswords(String weakPassword) throws Exception {
        String rawToken = "unified-token-" + UUID.randomUUID();
        passwordResetTokenRepository.save(PasswordResetTokenEntity.builder()
                .userId(testAdmin.getId())
                .tokenHash(PasswordSecurityUtils.hashSha256(rawToken))
                .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                .createdByIp("127.0.0.1")
                .build());

        ResetPasswordRequest request = new ResetPasswordRequest(rawToken, weakPassword);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/reset-password accepts valid 12+ compliant password")
    void testResetPassword_AcceptsValidPassword() throws Exception {
        String rawToken = "unified-token-" + UUID.randomUUID();
        passwordResetTokenRepository.save(PasswordResetTokenEntity.builder()
                .userId(testAdmin.getId())
                .tokenHash(PasswordSecurityUtils.hashSha256(rawToken))
                .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                .createdByIp("127.0.0.1")
                .build());

        ResetPasswordRequest request = new ResetPasswordRequest(rawToken, "BrandNewSecurePass456!");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // 4. INTEGRATION TESTS: CHANGE PASSWORD FLOW
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {
            "Short1!",
            "alllowercase123!",
            "NoSpecial123456",
            "password123!"
    })
    @DisplayName("POST /api/auth/change-password rejects non-compliant new passwords")
    void testChangePassword_RejectsNonCompliantPasswords(String weakPassword) throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "Tx9#SecureP@ss2026!",
                weakPassword,
                weakPassword
        );

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // 5. INTEGRATION TESTS: MARKETPLACE CUSTOMER REGISTRATION
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {
            "Short1!",
            "alllowercase123!",
            "NoSpecial123456",
            "password123!"
    })
    @DisplayName("POST /api/v1/marketplace/customer/register rejects non-compliant customer passwords")
    void testMarketplaceCustomerRegistration_RejectsNonCompliantPasswords(String weakPassword) throws Exception {
        RegisterCustomerRequest request = RegisterCustomerRequest.builder()
                .firstName("Ramesh")
                .lastName("Gupta")
                .email("ramesh." + UUID.randomUUID() + "@custtest.in")
                .phone("9876543210")
                .password(weakPassword)
                .build();

        mockMvc.perform(post("/api/v1/marketplace/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/marketplace/customer/register accepts valid 12+ compliant password")
    void testMarketplaceCustomerRegistration_AcceptsValidPassword() throws Exception {
        RegisterCustomerRequest request = RegisterCustomerRequest.builder()
                .firstName("Ramesh")
                .lastName("Gupta")
                .email("ramesh.valid." + UUID.randomUUID() + "@custtest.in")
                .phone("9876543210")
                .password("SecureCustPass123!")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // 6. INTEGRATION TESTS: CLIENT PORTAL USER CREATION
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {
            "Short1!",
            "alllowercase123!",
            "NoSpecial123456",
            "password123!"
    })
    @DisplayName("POST /api/v1/portal/users rejects non-compliant initial portal user passwords")
    void testClientPortalUserRegistration_RejectsNonCompliantPasswords(String weakPassword) throws Exception {
        RegisterClientPortalUserRequest request = RegisterClientPortalUserRequest.builder()
                .clientId(testClient.getId())
                .email("portal.user." + UUID.randomUUID() + "@clientcorp.in")
                .firstName("Aditya")
                .lastName("Verma")
                .password(weakPassword)
                .build();

        mockMvc.perform(post("/api/v1/portal/users")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // 7. INTEGRATION TESTS: REGISTER USER BY ADMIN
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {
            "Short1!",
            "alllowercase123!",
            "NoSpecial123456",
            "password123!"
    })
    @DisplayName("POST /api/auth/register-user rejects non-compliant member passwords")
    void testRegisterUserByAdmin_RejectsNonCompliantPasswords(String weakPassword) throws Exception {
        RegisterUserByAdminRequest request = RegisterUserByAdminRequest.builder()
                .firstName("Suresh")
                .lastName("Kumar")
                .email("suresh." + UUID.randomUUID() + "@practice.in")
                .password(weakPassword)
                .roleCodes(Set.of("ORG_ADMIN"))
                .build();

        mockMvc.perform(post("/api/auth/register-user")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
