package com.taxoryn.module.organization.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.organization.dto.CreateOrganizationRequest;
import com.taxoryn.module.organization.dto.UpdateOrganizationRequest;
import com.taxoryn.module.organization.dto.UpdateOrganizationSettingsRequest;
import com.taxoryn.module.organization.dto.UpdateOrganizationStatusRequest;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity testOrg;
    private String orgAdminToken;
    private String superAdminToken;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        organizationRepository.deleteAll();

        testOrg = OrganizationEntity.builder()
                .name("Integration Practice LLP")
                .legalName("Integration Practice Tax Advisors LLP")
                .email("contact@integrationpractice.com")
                .phone("+919876543210")
                .city("Bangalore")
                .state("Karnataka")
                .country("India")
                .pincode("560001")
                .pan("ABCDE1234F")
                .gstin("29ABCDE1234F1Z5")
                .taxRegistrationNumber("LLPIN-AAA-1111")
                .status(OrganizationStatus.ACTIVE)
                .build();
        testOrg = organizationRepository.save(testOrg);

        testUserId = UUID.randomUUID();
        orgAdminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                testUserId,
                testOrg.getId(),
                "admin@integrationpractice.com",
                Set.of("ORG_ADMIN"),
                Set.of("ORG_READ", "ORG_WRITE")
        );

        superAdminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                UUID.randomUUID(),
                testOrg.getId(),
                "superadmin@taxoryn.com",
                Set.of("SUPER_ADMIN"),
                Set.of("ORG_READ", "ORG_WRITE")
        );
    }

    @Test
    @DisplayName("GET /api/v1/organizations/current returns authenticated tenant details")
    void testGetCurrentOrganization() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/current")
                        .header("Authorization", orgAdminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Integration Practice LLP"))
                .andExpect(jsonPath("$.data.city").value("Bangalore"))
                .andExpect(jsonPath("$.data.pan").value("ABCDE1234F"))
                .andExpect(jsonPath("$.data.taxRegistrationNumber").value("LLPIN-AAA-1111"));
    }

    @Test
    @DisplayName("GET /api/v1/organizations/current without auth returns 401 UNAUTHORIZED")
    void testGetCurrentOrganizationUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/current")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/v1/organizations/current updates tenant profile")
    void testUpdateCurrentOrganization() throws Exception {
        UpdateOrganizationRequest request = UpdateOrganizationRequest.builder()
                .name("Updated Practice LLP")
                .legalName("Updated Practice Legal LLP")
                .city("Hyderabad")
                .state("Telangana")
                .pincode("500081")
                .pan("ABCDE1234F")
                .gstin("36ABCDE1234F1Z5")
                .taxRegistrationNumber("LLPIN-BBB-2222")
                .build();

        mockMvc.perform(put("/api/v1/organizations/current")
                        .header("Authorization", orgAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Practice LLP"))
                .andExpect(jsonPath("$.data.city").value("Hyderabad"))
                .andExpect(jsonPath("$.data.pincode").value("500081"))
                .andExpect(jsonPath("$.data.taxRegistrationNumber").value("LLPIN-BBB-2222"));
    }

    @Test
    @DisplayName("PUT /api/v1/organizations/current with invalid PAN fails validation with 400")
    void testUpdateCurrentOrganizationInvalidPan() throws Exception {
        UpdateOrganizationRequest request = UpdateOrganizationRequest.builder()
                .name("Updated Practice LLP")
                .pan("INVALID_PAN")
                .build();

        mockMvc.perform(put("/api/v1/organizations/current")
                        .header("Authorization", orgAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("GET & PUT /api/v1/organizations/current/settings manages tenant settings")
    void testGetAndUpdateCurrentOrganizationSettings() throws Exception {
        // GET settings
        mockMvc.perform(get("/api/v1/organizations/current/settings")
                        .header("Authorization", orgAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.timezone").value("Asia/Kolkata"));

        // UPDATE settings
        UpdateOrganizationSettingsRequest updateRequest = UpdateOrganizationSettingsRequest.builder()
                .timezone("Asia/Kolkata")
                .dateFormat("DD/MM/YYYY")
                .currency("INR")
                .financialYearStartMonth(4)
                .enableEmailNotifications(true)
                .enableSmsNotifications(true)
                .enableWhatsappNotifications(true)
                .invoicePrefix("TAX-HYD/")
                .autoRemindersEnabled(true)
                .build();

        mockMvc.perform(put("/api/v1/organizations/current/settings")
                        .header("Authorization", orgAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.invoicePrefix").value("TAX-HYD/"))
                .andExpect(jsonPath("$.data.enableWhatsappNotifications").value(true));
    }

    @Test
    @DisplayName("PATCH /api/v1/organizations/{id}/status changes organization operational state")
    void testUpdateOrganizationStatus() throws Exception {
        UpdateOrganizationStatusRequest statusRequest = UpdateOrganizationStatusRequest.builder()
                .status(OrganizationStatus.INACTIVE)
                .reason("Seasonal suspension")
                .build();

        mockMvc.perform(patch("/api/v1/organizations/" + testOrg.getId() + "/status")
                        .header("Authorization", orgAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/organizations creates new organization (Super Admin)")
    void testCreateOrganizationSuperAdmin() throws Exception {
        CreateOrganizationRequest createRequest = CreateOrganizationRequest.builder()
                .name("New Super Tenant")
                .email("contact@newsupertenant.com")
                .phone("+919999999999")
                .city("Chennai")
                .state("Tamil Nadu")
                .pincode("600001")
                .build();

        mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("New Super Tenant"))
                .andExpect(jsonPath("$.data.city").value("Chennai"));
    }
}
