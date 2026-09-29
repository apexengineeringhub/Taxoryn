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
class SubscriptionEntitlementIntegrationTest {

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

        // 2. Create Initial Subscription (STARTER: max 5 users, max 2 clients for tight limit testing)
        subscriptionRepository.save(SubscriptionEntity.builder()
                .organizationId(tenant.getId())
                .plan(SubscriptionPlan.STARTER)
                .status(SubscriptionStatus.ACTIVE)
                .billingInterval(BillingInterval.MONTHLY)
                .startDate(LocalDate.now())
                .renewalDate(LocalDate.now().plusDays(30))
                .maxUsers(5)
                .maxClients(2)
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
    @DisplayName("GET /api/v1/subscriptions/entitlements returns structured quota evaluation with percentage and warning flags")
    void testGetEntitlements() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions/entitlements")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organizationId").value(tenant.getId().toString()))
                .andExpect(jsonPath("$.data.organizationName").value("Apex CA Practice"))
                .andExpect(jsonPath("$.data.plan").value("STARTER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.entitlements").isArray())
                .andExpect(jsonPath("$.data.entitlements[0].resourceType").value("TEAM_MEMBER"))
                .andExpect(jsonPath("$.data.entitlements[0].currentUsage").value(1))
                .andExpect(jsonPath("$.data.entitlements[0].limit").value(5))
                .andExpect(jsonPath("$.data.entitlements[0].allowed").value(true))
                .andExpect(jsonPath("$.data.entitlements[1].resourceType").value("CLIENT"))
                .andExpect(jsonPath("$.data.entitlements[1].currentUsage").value(0))
                .andExpect(jsonPath("$.data.entitlements[1].limit").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/subscriptions/entitlements/{resourceType} returns single resource entitlement")
    void testGetSingleResourceEntitlement() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions/entitlements/TEAM_MEMBER")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resourceType").value("TEAM_MEMBER"))
                .andExpect(jsonPath("$.data.currentUsage").value(1))
                .andExpect(jsonPath("$.data.limit").value(5))
                .andExpect(jsonPath("$.data.allowed").value(true));
    }

    @Test
    @DisplayName("Client Creation Quota: 1st and 2nd allowed, 3rd blocked by SubscriptionLimitExceededException")
    void testClientCreationQuota() throws Exception {
        // Client 1 (allowed, 1/2)
        CreateClientRequest c1 = CreateClientRequest.builder()
                .displayName("Client Alpha")
                .clientType(ClientType.INDIVIDUAL)
                .pan("ABCDE1234F")
                .build();

        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(c1)))
                .andExpect(status().isCreated());

        // Client 2 (allowed, 2/2 = 100%)
        CreateClientRequest c2 = CreateClientRequest.builder()
                .displayName("Client Beta")
                .clientType(ClientType.COMPANY)
                .pan("BCDEF2345G")
                .build();

        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(c2)))
                .andExpect(status().isCreated());

        // Verify entitlement shows 100% full
        mockMvc.perform(get("/api/v1/subscriptions/entitlements/CLIENT")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentUsage").value(2))
                .andExpect(jsonPath("$.data.limit").value(2))
                .andExpect(jsonPath("$.data.allowed").value(false));

        // Client 3 (BLOCKED)
        CreateClientRequest c3 = CreateClientRequest.builder()
                .displayName("Client Gamma")
                .clientType(ClientType.PARTNERSHIP)
                .pan("CDEFG3456H")
                .build();

        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(c3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SUBSCRIPTION_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("Downgrade Prevention: Plan change is blocked when active clients exceed target plan quota")
    void testDowngradeBlockedWhenUsageExceedsTargetPlan() throws Exception {
        // Upgrade to PROFESSIONAL (100 clients)
        ChangePlanRequest upgradeReq = ChangePlanRequest.builder()
                .plan(SubscriptionPlan.PROFESSIONAL)
                .billingInterval(BillingInterval.MONTHLY)
                .build();

        mockMvc.perform(post("/api/v1/subscriptions/change-plan")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(upgradeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plan").value("PROFESSIONAL"));

        // Create 30 clients (exceeds STARTER limit of 25)
        TenantContext.setTenantId(tenant.getId());
        for (int i = 1; i <= 30; i++) {
            com.taxoryn.module.client.entity.ClientEntity c = com.taxoryn.module.client.entity.ClientEntity.builder()
                    .displayName("Test Client " + i)
                    .clientType(ClientType.INDIVIDUAL)
                    .status(com.taxoryn.module.client.entity.ClientEntity.ClientStatus.ACTIVE)
                    .build();
            c.setOrganizationId(tenant.getId());
            clientRepository.save(c);
        }
        TenantContext.clear();

        // Attempt Downgrade to STARTER (must fail)
        ChangePlanRequest downgradeReq = ChangePlanRequest.builder()
                .plan(SubscriptionPlan.STARTER)
                .billingInterval(BillingInterval.MONTHLY)
                .build();

        mockMvc.perform(post("/api/v1/subscriptions/change-plan")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(downgradeReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SUBSCRIPTION_LIMIT_EXCEEDED"));
    }
}
