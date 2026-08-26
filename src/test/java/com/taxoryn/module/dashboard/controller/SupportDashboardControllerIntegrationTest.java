package com.taxoryn.module.dashboard.controller;

import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.feedback.entity.ApplicationFeedbackActorType;
import com.taxoryn.module.feedback.entity.ApplicationFeedbackCategory;
import com.taxoryn.module.feedback.entity.ApplicationFeedbackContextType;
import com.taxoryn.module.feedback.entity.ApplicationFeedbackEntity;
import com.taxoryn.module.feedback.entity.ApplicationFeedbackPriority;
import com.taxoryn.module.feedback.entity.ApplicationFeedbackStatus;
import com.taxoryn.module.feedback.entity.ApplicationFeedbackType;
import com.taxoryn.module.feedback.repository.ApplicationFeedbackRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SupportDashboardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ApplicationFeedbackRepository feedbackRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity platformOrg;
    private UserEntity supportUser;
    private UserEntity clientUser;
    private String supportToken;
    private String clientToken;

    @BeforeEach
    void setUp() {
        feedbackRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        // 1. Create Organization
        platformOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Taxoryn Platform Operations")
                .legalName("Taxoryn Global Inc")
                .email("admin@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 2. Roles
        RoleEntity supportRole = roleRepository.findByCodeAndIsSystemRoleTrue("TAXORYN_SUPPORT_ADMIN").orElseGet(() ->
                roleRepository.save(RoleEntity.builder()
                        .code("TAXORYN_SUPPORT_ADMIN")
                        .name("Taxoryn Support Admin")
                        .isSystemRole(true)
                        .build()));

        RoleEntity clientRole = roleRepository.findByCodeAndIsSystemRoleTrue("CLIENT_USER").orElseGet(() ->
                roleRepository.save(RoleEntity.builder()
                        .code("CLIENT_USER")
                        .name("Client User")
                        .isSystemRole(true)
                        .build()));

        // 3. Users
        supportUser = userRepository.save(UserEntity.builder()
                .organizationId(platformOrg.getId())
                .email("support@taxoryn.com")
                .firstName("Rahul")
                .lastName("Verma")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(supportRole)))
                .build());

        clientUser = userRepository.save(UserEntity.builder()
                .organizationId(platformOrg.getId())
                .email("client@taxpayer.com")
                .firstName("Vijay")
                .lastName("Mallya")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(clientRole)))
                .build());

        // 4. Seed Support Feedback Records
        feedbackRepository.save(ApplicationFeedbackEntity.builder()
                .userId(clientUser.getId())
                .actorType(ApplicationFeedbackActorType.CUSTOMER)
                .contextType(ApplicationFeedbackContextType.CUSTOMER_PORTAL)
                .type(ApplicationFeedbackType.PROBLEM)
                .category(ApplicationFeedbackCategory.BILLING)
                .title("Cannot download GST payment invoice")
                .description("Getting 500 error when clicking download invoice in portal")
                .status(ApplicationFeedbackStatus.NEW)
                .priority(ApplicationFeedbackPriority.HIGH)
                .build());

        feedbackRepository.save(ApplicationFeedbackEntity.builder()
                .userId(clientUser.getId())
                .actorType(ApplicationFeedbackActorType.PRACTICE_EMPLOYEE)
                .contextType(ApplicationFeedbackContextType.PRACTICE_PORTAL)
                .type(ApplicationFeedbackType.SUGGESTION)
                .category(ApplicationFeedbackCategory.TASKS)
                .title("Need earlier advance tax reminder notifications")
                .description("Would be great to receive SMS alerts 5 days before due date")
                .status(ApplicationFeedbackStatus.UNDER_REVIEW)
                .priority(ApplicationFeedbackPriority.MEDIUM)
                .build());

        // 5. Tokens
        supportToken = jwtTokenProvider.generateAccessToken(
                supportUser.getId(), platformOrg.getId(), null, supportUser.getEmail(),
                Set.of("TAXORYN_SUPPORT_ADMIN"),
                Set.of("PLATFORM_VIEW", "SUPPORT_VIEW", "FEEDBACK_VIEW", "FEEDBACK_RESPOND", "PRACTICE_VIEW")
        );

        clientToken = jwtTokenProvider.generateAccessToken(
                clientUser.getId(), platformOrg.getId(), null, clientUser.getEmail(),
                Set.of("CLIENT_USER"),
                Set.of("PORTAL_VIEW")
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Support Admin successfully retrieves dedicated support workspace metrics")
    void testSupportAdminRetrievesSupportOverview() throws Exception {
        mockMvc.perform(get("/api/v1/admin/support/overview")
                        .header("Authorization", "Bearer " + supportToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.kpis.openCases").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.kpis.waitingForCustomer").value(1))
                .andExpect(jsonPath("$.data.kpis.highPriority").value(1))
                .andExpect(jsonPath("$.data.supportAttention").isArray())
                .andExpect(jsonPath("$.data.recentActivity").isArray());
    }

    @Test
    @DisplayName("Client user is rejected with 403 Forbidden when calling support overview")
    void testClientUserDeniedSupportOverview() throws Exception {
        mockMvc.perform(get("/api/v1/admin/support/overview")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }
}
