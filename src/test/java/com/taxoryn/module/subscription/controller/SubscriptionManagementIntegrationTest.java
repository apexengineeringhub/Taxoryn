package com.taxoryn.module.subscription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.subscription.dto.ChangePlanRequest;
import com.taxoryn.module.subscription.entity.SubscriptionEntity;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.BillingInterval;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionStatus;
import com.taxoryn.module.subscription.repository.SubscriptionRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
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

import java.math.BigDecimal;
import java.time.LocalDate;
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
class SubscriptionManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity tenant;
    private UserEntity adminUser;
    private String adminToken;

    @BeforeEach
    void setUp() {
        subscriptionRepository.deleteAll();
        userRepository.deleteAll();
        clientRepository.deleteAll();
        organizationRepository.deleteAll();

        // 1. Create Tenant
        tenant = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex CA Practice")
                .email("admin@apexcapractice.com")
                .status(OrganizationStatus.ACTIVE)
                .subscriptionPlan(OrganizationEntity.SubscriptionPlan.STARTER)
                .build());

        // 2. Create Initial Subscription (STARTER: max 5 users, max 2 clients for tight testing)
        subscriptionRepository.save(SubscriptionEntity.builder()
                .organizationId(tenant.getId())
                .plan(SubscriptionPlan.STARTER)
                .status(SubscriptionStatus.ACTIVE)
                .billingInterval(BillingInterval.MONTHLY)
                .startDate(LocalDate.now())
                .renewalDate(LocalDate.now().plusDays(30))
                .maxUsers(5)
                .maxClients(2) // limit to 2 clients for easy limit enforcement test
                .maxStorageBytes(5L * 1024 * 1024 * 1024)
                .price(new BigDecimal("999.00"))
                .autoRenew(true)
                .build());

        // 3. Create Admin User
        TenantContext.setTenantId(tenant.getId());
        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().code("ORG_ADMIN").name("Org Admin").isSystemRole(true).build()));

        adminUser = UserEntity.builder()
                .email("admin@apexcapractice.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Rajesh")
                .lastName("Sharma")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build();
        adminUser.setOrganizationId(tenant.getId());
        adminUser = userRepository.save(adminUser);

        adminToken = jwtTokenProvider.generateAccessToken(
                adminUser.getId(), tenant.getId(), null, adminUser.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("ORGANIZATION_VIEW", "ORGANIZATION_UPDATE", "CLIENT_CREATE", "CLIENT_VIEW", "USER_CREATE", "USER_VIEW")
        );

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Subscription Lifecycle: Get Current -> Get Usage -> Upgrade Plan -> Renew -> Cancel")
    void testSubscriptionLifecycle() throws Exception {
        // 1. Get Current Subscription
        mockMvc.perform(get("/api/v1/subscriptions/current")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plan").value("STARTER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.maxUsers").value(5))
                .andExpect(jsonPath("$.data.price").value(999.00));

        // 2. Get Subscription Usage Metrics
        mockMvc.perform(get("/api/v1/subscriptions/usage")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plan").value("STARTER"))
                .andExpect(jsonPath("$.data.currentUsers").value(1))
                .andExpect(jsonPath("$.data.maxUsers").value(5))
                .andExpect(jsonPath("$.data.userLimitReached").value(false));

        // 3. Upgrade Subscription Plan to PROFESSIONAL
        ChangePlanRequest upgradeReq = ChangePlanRequest.builder()
                .plan(SubscriptionPlan.PROFESSIONAL)
                .billingInterval(BillingInterval.MONTHLY)
                .build();

        mockMvc.perform(post("/api/v1/subscriptions/change-plan")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(upgradeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plan").value("PROFESSIONAL"))
                .andExpect(jsonPath("$.data.maxUsers").value(15))
                .andExpect(jsonPath("$.data.maxClients").value(100))
                .andExpect(jsonPath("$.data.price").value(2499.00));

        // 4. Renew Subscription
        mockMvc.perform(post("/api/v1/subscriptions/renew")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // 5. Cancel Subscription
        mockMvc.perform(post("/api/v1/subscriptions/cancel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"))
                .andExpect(jsonPath("$.data.autoRenew").value(false));
    }

    @Test
    @DisplayName("Subscription Limit Enforcement: Adding clients beyond MAX_CLIENTS throws 400 SUBSCRIPTION_LIMIT_EXCEEDED")
    void testClientLimitEnforcement() throws Exception {
        // Create Client 1 (allowed, count becomes 1/2)
        CreateClientRequest client1 = CreateClientRequest.builder()
                .displayName("Client 1")
                .clientType(ClientType.INDIVIDUAL)
                .pan("ABCDE1234F")
                .build();

        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(client1)))
                .andExpect(status().isCreated());

        // Create Client 2 (allowed, count becomes 2/2)
        CreateClientRequest client2 = CreateClientRequest.builder()
                .displayName("Client 2")
                .clientType(ClientType.PROPRIETORSHIP)
                .pan("BCDEF2345G")
                .build();

        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(client2)))
                .andExpect(status().isCreated());

        // Create Client 3 (BLOCKED by MAX_CLIENTS limit of 2)
        CreateClientRequest client3 = CreateClientRequest.builder()
                .displayName("Client 3")
                .clientType(ClientType.PARTNERSHIP)
                .pan("CDEFG3456H")
                .build();

        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(client3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SUBSCRIPTION_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("List available SaaS subscription plans catalog")
    void testGetAvailablePlans() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].plan").value("STARTER"))
                .andExpect(jsonPath("$.data[1].plan").value("PROFESSIONAL"))
                .andExpect(jsonPath("$.data[2].plan").value("BUSINESS"))
                .andExpect(jsonPath("$.data[3].plan").value("ENTERPRISE"));
    }
}
