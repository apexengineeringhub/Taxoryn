package com.taxoryn.module.organization.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.organization.dto.PracticeProfileDto;
import com.taxoryn.module.organization.dto.PracticeSetupOnboardingRequest;
import com.taxoryn.module.organization.dto.UpdatePracticeProfileRequest;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationType;
import com.taxoryn.module.organization.entity.PracticeType;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.organization.repository.PracticeProfileRepository;
import com.taxoryn.module.subscription.entity.SubscriptionEntity;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;
import com.taxoryn.module.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PracticeProfileAndPlanRecommendationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PracticeProfileRepository practiceProfileRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private String orgAAdminToken;
    private String orgBAdminToken;
    private UUID userAId;
    private UUID userBId;

    @BeforeEach
    void setUp() {
        practiceProfileRepository.deleteAll();
        subscriptionRepository.deleteAll();
        organizationRepository.deleteAll();

        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Tax Consultants")
                .legalName("Apex Tax Consultants LLP")
                .email("admin@apextax.com")
                .organizationType(OrganizationType.SOLO)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Zenith Global Tax Advisors")
                .legalName("Zenith Global Tax Advisors Private Limited")
                .email("admin@zenithglobal.com")
                .organizationType(OrganizationType.FIRM)
                .build());

        subscriptionRepository.save(SubscriptionEntity.builder()
                .organizationId(orgA.getId())
                .plan(SubscriptionPlan.STARTER)
                .startDate(LocalDate.now())
                .renewalDate(LocalDate.now().plusMonths(1))
                .build());

        subscriptionRepository.save(SubscriptionEntity.builder()
                .organizationId(orgB.getId())
                .plan(SubscriptionPlan.PROFESSIONAL)
                .startDate(LocalDate.now())
                .renewalDate(LocalDate.now().plusMonths(1))
                .build());

        userAId = UUID.randomUUID();
        userBId = UUID.randomUUID();

        orgAAdminToken = jwtTokenProvider.generateAccessToken(userAId, orgA.getId(), "admin@apextax.com",
                Set.of("ROLE_ORG_ADMIN"), Set.of("ORGANIZATION_VIEW", "ORGANIZATION_UPDATE", "ORG_READ", "ORG_WRITE"));

        orgBAdminToken = jwtTokenProvider.generateAccessToken(userBId, orgB.getId(), "admin@zenithglobal.com",
                Set.of("ROLE_ORG_ADMIN"), Set.of("ORGANIZATION_VIEW", "ORGANIZATION_UPDATE", "ORG_READ", "ORG_WRITE"));
    }

    @Test
    @DisplayName("Should retrieve default practice profile for organization")
    void testGetPracticeProfile() throws Exception {
        mockMvc.perform(get("/api/v1/practice-profile")
                        .header("Authorization", "Bearer " + orgAAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.organizationId").value(orgA.getId().toString()))
                .andExpect(jsonPath("$.data.practiceType").value("SOLO"))
                .andExpect(jsonPath("$.data.recommendedPlan").value("STARTER"));
    }

    @Test
    @DisplayName("Should complete practice setup onboarding and generate recommendation")
    void testCompleteOnboarding() throws Exception {
        PracticeSetupOnboardingRequest request = PracticeSetupOnboardingRequest.builder()
                .practiceType(PracticeType.FIRM)
                .yearsInPractice(8)
                .approximateClientCount(80)
                .practitionerCount(4)
                .employeeCount(12)
                .locationCount(2)
                .servicesOffered(List.of("GST", "ITR", "TDS", "TAX_NOTICES", "BILLING"))
                .primaryTaxServices(List.of("GST_COMPLIANCE", "ITR_COMPLIANCE"))
                .build();

        mockMvc.perform(post("/api/v1/practice-profile/onboarding")
                        .header("Authorization", "Bearer " + orgAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.onboardingCompleted").value(true))
                .andExpect(jsonPath("$.data.practiceType").value("FIRM"))
                .andExpect(jsonPath("$.data.recommendedPlan").value("BUSINESS"));
    }

    @Test
    @DisplayName("Should generate accurate plan recommendation without altering authorization")
    void testPlanRecommendation() throws Exception {
        // Update profile to Enterprise scale
        UpdatePracticeProfileRequest updateReq = UpdatePracticeProfileRequest.builder()
                .practiceType(PracticeType.ENTERPRISE)
                .locationCount(5)
                .practitionerCount(15)
                .employeeCount(35)
                .approximateClientCount(600)
                .build();

        mockMvc.perform(put("/api/v1/practice-profile")
                        .header("Authorization", "Bearer " + orgAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendedPlan").value("ENTERPRISE"));

        // Get recommendation endpoint
        mockMvc.perform(get("/api/v1/practice-profile/recommendation")
                        .header("Authorization", "Bearer " + orgAAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recommendedPlan").value("ENTERPRISE"))
                .andExpect(jsonPath("$.data.multiLocationEnabled").value(true))
                .andExpect(jsonPath("$.data.maxLocations").value(50));
    }

    @Test
    @DisplayName("Should confirm chosen plan and update organization subscription")
    void testConfirmPlan() throws Exception {
        PracticeProfileController.ConfirmPlanPayload payload = new PracticeProfileController.ConfirmPlanPayload();
        payload.setPlan("BUSINESS");

        mockMvc.perform(post("/api/v1/practice-profile/confirm-plan")
                        .header("Authorization", "Bearer " + orgAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.confirmedPlan").value("BUSINESS"));

        SubscriptionEntity sub = subscriptionRepository.findByOrganizationId(orgA.getId()).orElseThrow();
        assertThat(sub.getPlan()).isEqualTo(SubscriptionPlan.BUSINESS);
    }

    @Test
    @DisplayName("Should retrieve consolidated practice admin overview hub metrics")
    void testPracticeAdminOverview() throws Exception {
        mockMvc.perform(get("/api/v1/practice-profile/overview")
                        .header("Authorization", "Bearer " + orgAAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.organizationId").value(orgA.getId().toString()))
                .andExpect(jsonPath("$.data.organizationName").value(orgA.getName()))
                .andExpect(jsonPath("$.data.subscriptionPlan").value("STARTER"))
                .andExpect(jsonPath("$.data.subscriptionStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.multiLocationEnabled").value(false))
                .andExpect(jsonPath("$.data.maxLocations").value(1));
    }
}
