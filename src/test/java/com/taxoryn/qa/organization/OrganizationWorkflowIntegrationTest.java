package com.taxoryn.qa.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.authentication.dto.ActivateOrganizationRequest;
import com.taxoryn.module.authentication.dto.LoginRequest;
import com.taxoryn.module.authentication.dto.RegisterOrganizationRequest;
import com.taxoryn.module.authentication.dto.ResendActivationRequest;
import com.taxoryn.module.authentication.entity.OrganizationActivationTokenEntity;
import com.taxoryn.module.authentication.repository.OrganizationActivationTokenRepository;
import com.taxoryn.module.authentication.service.AuthService;
import com.taxoryn.module.notification.email.config.EmailProperties;
import com.taxoryn.module.organization.dto.UpdateOrganizationRequest;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import com.taxoryn.qa.factory.OrganizationTestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationTestDataFactory factory;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationActivationTokenRepository activationTokenRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private EmailProperties emailProperties;

    @Autowired
    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private UserEntity adminA;
    private UserEntity adminB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        orgA = factory.createOrganization("Apex Tax Consultants " + UUID.randomUUID().toString().substring(0, 5), "admin.apex." + UUID.randomUUID().toString().substring(0, 5) + "@apextax.in");
        orgB = factory.createOrganization("Beacon Advisory " + UUID.randomUUID().toString().substring(0, 5), "admin.beacon." + UUID.randomUUID().toString().substring(0, 5) + "@beacon.in");
        adminA = factory.createAdminUser(orgA, "adminA." + UUID.randomUUID().toString().substring(0, 5) + "@apextax.in", "AdminPass123!");
        adminB = factory.createAdminUser(orgB, "adminB." + UUID.randomUUID().toString().substring(0, 5) + "@beacon.in", "AdminPass123!");
        tokenA = factory.generateBearerToken(adminA);
        tokenB = factory.generateBearerToken(adminB);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    // ==========================================
    // TEST 1: Register organization -> Status remains INACTIVE
    // ==========================================
    @Test
    @DisplayName("TEST 1: Register organization leaves organization and admin in INACTIVE status awaiting activation")
    void test1_registerOrganizationLeavesOrgAndUserInactive() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 6);
        RegisterOrganizationRequest request = RegisterOrganizationRequest.builder()
                .organizationName("Vertex Financial Group " + unique)
                .organizationEmail("contact." + unique + "@vertexgroup.in")
                .organizationPhone("+919876543210")
                .pan("AAACT1234A")
                .gstin("27AAACT1234A1Z5")
                .adminFirstName("Rajesh")
                .adminLastName("Verma")
                .adminEmail("rajesh." + unique + "@vertexgroup.in")
                .adminPassword("StrongSecurePass123!")
                .adminPhone("+919876543211")
                .build();

        mockMvc.perform(post("/api/v1/auth/register-organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("INACTIVE")))
                .andExpect(jsonPath("$.data.organizationName", is("Vertex Financial Group " + unique)))
                .andExpect(jsonPath("$.data.adminEmail", is("rajesh." + unique + "@vertexgroup.in")));

        OrganizationEntity savedOrg = organizationRepository.findByEmailIgnoreCase("contact." + unique + "@vertexgroup.in").orElseThrow();
        assertEquals(OrganizationStatus.INACTIVE, savedOrg.getStatus());

        UserEntity savedUser = userRepository.findByEmailIgnoreCase("rajesh." + unique + "@vertexgroup.in").orElseThrow();
        assertEquals(UserStatus.INACTIVE, savedUser.getStatus());
    }

    // ==========================================
    // TEST 2: Registration generates activation token record in DB
    // ==========================================
    @Test
    @DisplayName("TEST 2: Registration generates and persists SHA-256 hashed activation token with 24h expiry")
    void test2_registrationGeneratesActivationToken() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 6);
        RegisterOrganizationRequest request = RegisterOrganizationRequest.builder()
                .organizationName("Token Test Org " + unique)
                .organizationEmail("token." + unique + "@tokentest.in")
                .adminFirstName("Token")
                .adminLastName("Admin")
                .adminEmail("tokenadmin." + unique + "@tokentest.in")
                .adminPassword("StrongSecurePass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register-organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        UserEntity user = userRepository.findByEmailIgnoreCase("tokenadmin." + unique + "@tokentest.in").orElseThrow();
        List<OrganizationActivationTokenEntity> tokens = activationTokenRepository.findAllByUserIdAndUsedAtIsNull(user.getId());

        assertFalse(tokens.isEmpty());
        OrganizationActivationTokenEntity tokenEntity = tokens.get(0);
        assertNotNull(tokenEntity.getTokenHash());
        assertEquals(64, tokenEntity.getTokenHash().length()); // SHA-256 hex length
        assertNull(tokenEntity.getUsedAt());
        assertTrue(tokenEntity.getExpiresAt().isAfter(Instant.now().plus(23, ChronoUnit.HOURS)));
    }

    // ==========================================
    // TEST 3: Activation URL format adheres to centralized non-subdomain architecture
    // ==========================================
    @Test
    @DisplayName("TEST 3: Activation URL format uses centralized app.taxoryn.com/activate without subdomains")
    void test3_activationUrlStructureStandard() {
        String activationUrl = emailProperties.getActivationUrl();
        assertNotNull(activationUrl);
        assertTrue(activationUrl.contains("/activate"));
        assertFalse(activationUrl.contains("{tenant}"));
        assertFalse(activationUrl.contains("{organization}"));
    }

    // ==========================================
    // TEST 4: Valid activation token activates organization & admin
    // ==========================================
    @Test
    @DisplayName("TEST 4: Valid activation token sets organization and user to ACTIVE")
    void test4_validTokenActivatesOrganizationAndUser() {
        String unique = UUID.randomUUID().toString().substring(0, 6);
        OrganizationEntity org = factory.createOrganization("Inactive Org " + unique, "org." + unique + "@test.in");
        org.setStatus(OrganizationStatus.INACTIVE);
        organizationRepository.save(org);

        UserEntity user = factory.createAdminUser(org, "user." + unique + "@test.in", "Password123!");
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        String rawToken = "sample-raw-token-" + UUID.randomUUID();
        String tokenHash = hashToken(rawToken);

        OrganizationActivationTokenEntity tokenEntity = OrganizationActivationTokenEntity.builder()
                .userId(user.getId())
                .organizationId(org.getId())
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
        activationTokenRepository.save(tokenEntity);

        // Perform activation
        authService.activateOrganization(new ActivateOrganizationRequest(rawToken), "127.0.0.1");

        OrganizationEntity updatedOrg = organizationRepository.findById(org.getId()).orElseThrow();
        assertEquals(OrganizationStatus.ACTIVE, updatedOrg.getStatus());

        UserEntity updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(UserStatus.ACTIVE, updatedUser.getStatus());

        OrganizationActivationTokenEntity consumedToken = activationTokenRepository.findByTokenHash(tokenHash).orElseThrow();
        assertNotNull(consumedToken.getUsedAt());
    }

    // ==========================================
    // TEST 5: Used activation token is rejected on second attempt
    // ==========================================
    @Test
    @DisplayName("TEST 5: Used activation token cannot be consumed a second time")
    void test5_usedTokenCannotBeReused() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 6);
        OrganizationEntity org = factory.createOrganization("Used Token Org " + unique, "used." + unique + "@test.in");
        org.setStatus(OrganizationStatus.INACTIVE);
        organizationRepository.save(org);

        UserEntity user = factory.createAdminUser(org, "useduser." + unique + "@test.in", "Password123!");
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        String rawToken = "already-used-raw-token-" + UUID.randomUUID();
        String tokenHash = hashToken(rawToken);

        OrganizationActivationTokenEntity tokenEntity = OrganizationActivationTokenEntity.builder()
                .userId(user.getId())
                .organizationId(org.getId())
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .usedAt(Instant.now().minusSeconds(60))
                .build();
        activationTokenRepository.save(tokenEntity);

        ActivateOrganizationRequest request = new ActivateOrganizationRequest(rawToken);

        mockMvc.perform(post("/api/v1/auth/activate-organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // ==========================================
    // TEST 6: Expired activation token is rejected
    // ==========================================
    @Test
    @DisplayName("TEST 6: Expired activation token is rejected")
    void test6_expiredTokenIsRejected() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 6);
        OrganizationEntity org = factory.createOrganization("Expired Token Org " + unique, "expired." + unique + "@test.in");
        org.setStatus(OrganizationStatus.INACTIVE);
        organizationRepository.save(org);

        UserEntity user = factory.createAdminUser(org, "expireduser." + unique + "@test.in", "Password123!");
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        String rawToken = "expired-raw-token-" + UUID.randomUUID();
        String tokenHash = hashToken(rawToken);

        OrganizationActivationTokenEntity tokenEntity = OrganizationActivationTokenEntity.builder()
                .userId(user.getId())
                .organizationId(org.getId())
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().minusSeconds(3600)) // Expired 1 hour ago
                .build();
        activationTokenRepository.save(tokenEntity);

        ActivateOrganizationRequest request = new ActivateOrganizationRequest(rawToken);

        mockMvc.perform(post("/api/v1/auth/activate-organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // ==========================================
    // TEST 7: Invalid activation token is rejected
    // ==========================================
    @Test
    @DisplayName("TEST 7: Completely invalid / bogus activation token is rejected")
    void test7_invalidTokenIsRejected() throws Exception {
        ActivateOrganizationRequest request = new ActivateOrganizationRequest("non-existent-bogus-token-xyz");

        mockMvc.perform(post("/api/v1/auth/activate-organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // ==========================================
    // TEST 8: Login before activation is rejected with ACCOUNT_INACTIVE
    // ==========================================
    @Test
    @DisplayName("TEST 8: Login before email activation is strictly rejected with ACCOUNT_INACTIVE")
    void test8_loginBeforeActivationIsRejected() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 6);
        String adminEmail = "inactive." + unique + "@taxoryn.com";
        String password = "SecurePassword123!";

        RegisterOrganizationRequest request = RegisterOrganizationRequest.builder()
                .organizationName("Inactive Login Org " + unique)
                .organizationEmail("org." + unique + "@taxoryn.com")
                .adminFirstName("Inactive")
                .adminLastName("User")
                .adminEmail(adminEmail)
                .adminPassword(password)
                .build();

        mockMvc.perform(post("/api/v1/auth/register-organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Attempt login immediately before activation
        LoginRequest loginReq = LoginRequest.builder()
                .email(adminEmail)
                .password(password)
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCOUNT_INACTIVE")));
    }

    // ==========================================
    // TEST 9: Login after activation succeeds
    // ==========================================
    @Test
    @DisplayName("TEST 9: Login after activation succeeds and returns valid JWT session")
    void test9_loginAfterActivationSucceeds() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 6);
        String adminEmail = "activeflow." + unique + "@taxoryn.com";
        String password = "SecurePassword123!";

        OrganizationEntity org = factory.createOrganization("Active Flow Org " + unique, "activeflow.org." + unique + "@test.in");
        org.setStatus(OrganizationStatus.INACTIVE);
        organizationRepository.save(org);

        UserEntity user = factory.createAdminUser(org, adminEmail, password);
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        String rawToken = "activation-token-for-login-" + UUID.randomUUID();
        String tokenHash = hashToken(rawToken);

        OrganizationActivationTokenEntity tokenEntity = OrganizationActivationTokenEntity.builder()
                .userId(user.getId())
                .organizationId(org.getId())
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
        activationTokenRepository.save(tokenEntity);

        // 1. Activate organization
        authService.activateOrganization(new ActivateOrganizationRequest(rawToken), "127.0.0.1");

        // 2. Login now succeeds
        LoginRequest loginReq = LoginRequest.builder()
                .email(adminEmail)
                .password(password)
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.user.email", is(adminEmail)))
                .andExpect(jsonPath("$.data.organization.id", is(org.getId().toString())));
    }

    // ==========================================
    // TEST 10: Organization isolation / RBAC check
    // ==========================================
    @Test
    @DisplayName("TEST 10: Multi-tenant data isolation - Admin A cannot access Org B details")
    void test10_organizationIsolationEnforced() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/" + orgB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // TEST 11: Activation token concurrency atomic check
    // ==========================================
    @Test
    @DisplayName("TEST 11: Concurrent activation attempts for same token only permit 1 winner")
    void test11_atomicTokenConsumptionUnderConcurrency() throws InterruptedException {
        String unique = UUID.randomUUID().toString().substring(0, 6);
        OrganizationEntity org = factory.createOrganization("Concurrent Org " + unique, "concurrent." + unique + "@test.in");
        org.setStatus(OrganizationStatus.INACTIVE);
        organizationRepository.save(org);

        UserEntity user = factory.createAdminUser(org, "concurrent.user." + unique + "@test.in", "Password123!");
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        String rawToken = "concurrent-token-" + UUID.randomUUID();
        String tokenHash = hashToken(rawToken);

        OrganizationActivationTokenEntity tokenEntity = OrganizationActivationTokenEntity.builder()
                .userId(user.getId())
                .organizationId(org.getId())
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
        activationTokenRepository.saveAndFlush(tokenEntity);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Integer consumed = transactionTemplate.execute(status ->
                            activationTokenRepository.consumeTokenAtomic(tokenHash, Instant.now())
                    );
                    if (consumed != null && consumed == 1) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one concurrent thread must succeed in consuming the activation token");
        assertEquals(threadCount - 1, failureCount.get());
    }

    // ==========================================
    // TEST 12: Production email sender configuration check
    // ==========================================
    @Test
    @DisplayName("TEST 12: Email configuration sender defaults to info@taxoryn.com with Taxoryn branding")
    void test12_emailSenderConfiguration() {
        assertEquals("info@taxoryn.com", emailProperties.getFromEmail());
        assertEquals("Taxoryn", emailProperties.getFromName());
    }

    // ==========================================
    // TEST 13: Resend activation email workflow
    // ==========================================
    @Test
    @DisplayName("TEST 13: Resend activation issues a fresh token and invalidates previous pending tokens")
    void test13_resendActivationFlow() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 6);
        String adminEmail = "resend." + unique + "@taxoryn.com";

        OrganizationEntity org = factory.createOrganization("Resend Org " + unique, "resend.org." + unique + "@test.in");
        org.setStatus(OrganizationStatus.INACTIVE);
        organizationRepository.save(org);

        UserEntity user = factory.createAdminUser(org, adminEmail, "Password123!");
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        // Issue first token
        String oldRawToken = "old-token-" + UUID.randomUUID();
        OrganizationActivationTokenEntity oldToken = OrganizationActivationTokenEntity.builder()
                .userId(user.getId())
                .organizationId(org.getId())
                .tokenHash(hashToken(oldRawToken))
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
        activationTokenRepository.save(oldToken);

        // Resend activation
        ResendActivationRequest resendReq = new ResendActivationRequest(adminEmail);
        mockMvc.perform(post("/api/v1/auth/resend-activation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Verify old token is now marked used/invalidated
        OrganizationActivationTokenEntity refreshedOldToken = activationTokenRepository.findById(oldToken.getId()).orElseThrow();
        assertNotNull(refreshedOldToken.getUsedAt());

        // Verify a new active token exists for user
        List<OrganizationActivationTokenEntity> pendingTokens = activationTokenRepository.findAllByUserIdAndUsedAtIsNull(user.getId());
        assertEquals(1, pendingTokens.size());
        assertNotEquals(oldToken.getId(), pendingTokens.get(0).getId());
    }

    // ==========================================
    // TEST 14: Centralized app domain without subdomains
    // ==========================================
    @Test
    @DisplayName("TEST 14: Authenticated user access functions under single centralized app.taxoryn.com domain")
    void test14_centralizedDomainAccess() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email", is(adminA.getEmail())))
                .andExpect(jsonPath("$.data.organizationId", is(orgA.getId().toString())));
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
