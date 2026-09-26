package com.taxoryn.module.organization.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.organization.dto.AssignLocationEmployeesRequest;
import com.taxoryn.module.organization.dto.CreateLocationRequest;
import com.taxoryn.module.organization.dto.UpdateLocationRequest;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationType;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MultiLocationAndLimitEnforcementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity orgStarter;
    private OrganizationEntity orgBusiness;
    private String starterAdminToken;
    private String businessAdminToken;
    private UUID starterUserId;
    private UUID businessUserId;

    @BeforeEach
    void setUp() {
        locationRepository.deleteAll();
        subscriptionRepository.deleteAll();
        organizationRepository.deleteAll();

        orgStarter = organizationRepository.save(OrganizationEntity.builder()
                .name("Solo Tax Consultant")
                .legalName("Solo Tax Consultant")
                .email("admin@solotax.com")
                .organizationType(OrganizationType.SOLO)
                .build());

        orgBusiness = organizationRepository.save(OrganizationEntity.builder()
                .name("Grand CA Firm")
                .legalName("Grand CA Firm LLP")
                .email("admin@grandfirm.com")
                .organizationType(OrganizationType.FIRM)
                .build());

        subscriptionRepository.save(SubscriptionEntity.builder()
                .organizationId(orgStarter.getId())
                .plan(SubscriptionPlan.STARTER)
                .startDate(LocalDate.now())
                .renewalDate(LocalDate.now().plusMonths(1))
                .build());

        subscriptionRepository.save(SubscriptionEntity.builder()
                .organizationId(orgBusiness.getId())
                .plan(SubscriptionPlan.BUSINESS)
                .startDate(LocalDate.now())
                .renewalDate(LocalDate.now().plusMonths(1))
                .build());

        starterUserId = UUID.randomUUID();
        businessUserId = UUID.randomUUID();

        starterAdminToken = jwtTokenProvider.generateAccessToken(starterUserId, orgStarter.getId(), "admin@solotax.com",
                Set.of("ROLE_ORG_ADMIN"), Set.of("ORGANIZATION_VIEW", "ORGANIZATION_UPDATE", "ORG_READ", "ORG_WRITE"));

        businessAdminToken = jwtTokenProvider.generateAccessToken(businessUserId, orgBusiness.getId(), "admin@grandfirm.com",
                Set.of("ROLE_ORG_ADMIN"), Set.of("ORGANIZATION_VIEW", "ORGANIZATION_UPDATE", "ORG_READ", "ORG_WRITE"));
    }

    @Test
    @DisplayName("Should create head office location for Starter organization")
    void testCreateInitialLocation() throws Exception {
        CreateLocationRequest request = CreateLocationRequest.builder()
                .name("Main Office - Mumbai")
                .city("Mumbai")
                .state("Maharashtra")
                .addressLine1("Bandra Kurla Complex")
                .pincode("400051")
                .isHeadOffice(true)
                .build();

        mockMvc.perform(post("/api/v1/locations")
                        .header("Authorization", "Bearer " + starterAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Main Office - Mumbai"))
                .andExpect(jsonPath("$.data.isHeadOffice").value(true));
    }

    @Test
    @DisplayName("Should reject 2nd location for Starter plan (multi-location not enabled)")
    void testStarterPlanMultiLocationEnforcement() throws Exception {
        // 1st location succeeds
        CreateLocationRequest loc1 = CreateLocationRequest.builder()
                .name("Mumbai Branch")
                .city("Mumbai")
                .state("Maharashtra")
                .isHeadOffice(true)
                .build();

        mockMvc.perform(post("/api/v1/locations")
                        .header("Authorization", "Bearer " + starterAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loc1)))
                .andExpect(status().isCreated());

        // 2nd location must be rejected
        CreateLocationRequest loc2 = CreateLocationRequest.builder()
                .name("Pune Branch")
                .city("Pune")
                .state("Maharashtra")
                .build();

        mockMvc.perform(post("/api/v1/locations")
                        .header("Authorization", "Bearer " + starterAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loc2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should allow multiple locations for Business plan up to max limit")
    void testBusinessPlanMultiLocationAllowed() throws Exception {
        CreateLocationRequest loc1 = CreateLocationRequest.builder()
                .name("Delhi Head Office")
                .city("New Delhi")
                .state("Delhi")
                .isHeadOffice(true)
                .build();

        mockMvc.perform(post("/api/v1/locations")
                        .header("Authorization", "Bearer " + businessAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loc1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isHeadOffice").value(true));

        CreateLocationRequest loc2 = CreateLocationRequest.builder()
                .name("Bangalore Office")
                .city("Bangalore")
                .state("Karnataka")
                .build();

        mockMvc.perform(post("/api/v1/locations")
                        .header("Authorization", "Bearer " + businessAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loc2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Bangalore Office"));

        // List locations
        mockMvc.perform(get("/api/v1/locations")
                        .header("Authorization", "Bearer " + businessAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("Tenant Isolation: Starter org cannot access Business org location")
    void testTenantIsolationOnLocations() throws Exception {
        LocationEntity businessLocation = LocationEntity.builder()
                .name("Hyderabad Office")
                .city("Hyderabad")
                .state("Telangana")
                .build();
        businessLocation.setOrganizationId(orgBusiness.getId());
        businessLocation = locationRepository.save(businessLocation);

        mockMvc.perform(get("/api/v1/locations/" + businessLocation.getId())
                        .header("Authorization", "Bearer " + starterAdminToken))
                .andExpect(status().isNotFound());
    }
}
