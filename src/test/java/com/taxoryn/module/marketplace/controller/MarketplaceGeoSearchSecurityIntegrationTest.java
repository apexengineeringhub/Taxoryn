package com.taxoryn.module.marketplace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.ProfessionalType;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.VerificationStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.VisibilityStatus;
import com.taxoryn.module.marketplace.entity.PracticeLocationEntity;
import com.taxoryn.module.marketplace.repository.MarketplaceProfileRepository;
import com.taxoryn.module.marketplace.repository.PracticeLocationRepository;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MarketplaceGeoSearchSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private MarketplaceProfileRepository profileRepository;

    @Autowired
    private PracticeLocationRepository locationRepository;

    @Autowired
    private com.taxoryn.core.security.RateLimitingService rateLimitingService;

    private OrganizationEntity orgBangalore;
    private OrganizationEntity orgMumbai;
    private MarketplaceProfileEntity profileBangalore;
    private MarketplaceProfileEntity profileMumbai;
    private MarketplaceProfileEntity profilePrivate;

    @BeforeEach
    void setUp() {
        if (rateLimitingService != null) {
            rateLimitingService.reset();
        }
        locationRepository.deleteAll();
        profileRepository.deleteAll();
        organizationRepository.deleteAll();

        // 1. Organization Bangalore
        orgBangalore = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Tax Bangalore LLP")
                .legalName("Apex Tax Bangalore LLP")
                .email("admin@apexbangalore.com")
                .city("Bengaluru")
                .state("Karnataka")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 2. Organization Mumbai
        orgMumbai = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Tax Mumbai LLP")
                .legalName("Apex Tax Mumbai LLP")
                .email("admin@apexmumbai.com")
                .city("Mumbai")
                .state("Maharashtra")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 3. Public Bangalore Practice (MG Road & Indiranagar branches)
        profileBangalore = profileRepository.save(MarketplaceProfileEntity.builder()
                .organizationId(orgBangalore.getId())
                .displayName("Apex Bangalore Advisors")
                .slug("apex-bangalore-advisors")
                .headline("Premier GST & Corporate Tax Specialists in Bangalore")
                .bio("Full service direct and indirect tax practice in Bangalore")
                .email("contact@apexbangalore.com")
                .phone("+919811122233")
                .professionalType(ProfessionalType.CHARTERED_ACCOUNTANT)
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560001")
                .specializations("GST_FILING, ITR_FILING, TAX_AUDIT")
                .verificationStatus(VerificationStatus.VERIFIED)
                .visibilityStatus(VisibilityStatus.PUBLIC)
                .isPublished(true)
                .build());

        // Location 1: Bangalore Indiranagar (approx 4.5 km from MG Road 12.9716, 77.5946)
        locationRepository.save(PracticeLocationEntity.builder()
                .organizationId(orgBangalore.getId())
                .marketplaceProfileId(profileBangalore.getId())
                .locationName("Indiranagar Hub")
                .addressLine1("100 Feet Road, Indiranagar")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560038")
                .latitude(new BigDecimal("12.978400"))
                .longitude(new BigDecimal("77.640800"))
                .isPrimary(true)
                .isActive(true)
                .build());

        // Location 2: Bangalore Whitefield (approx 17 km from MG Road)
        locationRepository.save(PracticeLocationEntity.builder()
                .organizationId(orgBangalore.getId())
                .marketplaceProfileId(profileBangalore.getId())
                .locationName("Whitefield Tech Hub")
                .addressLine1("ITPL Main Road")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560066")
                .latitude(new BigDecimal("12.969800"))
                .longitude(new BigDecimal("77.749900"))
                .isPrimary(false)
                .isActive(true)
                .build());

        // 4. Public Mumbai Practice (Bandra branch ~840 km from Bangalore)
        profileMumbai = profileRepository.save(MarketplaceProfileEntity.builder()
                .organizationId(orgMumbai.getId())
                .displayName("Apex Mumbai Advisory")
                .slug("apex-mumbai-advisory")
                .headline("Corporate Tax & M&A Advisors in Mumbai")
                .bio("Chartered Accountants firm in Mumbai")
                .email("contact@apexmumbai.com")
                .phone("+919844455566")
                .professionalType(ProfessionalType.CHARTERED_ACCOUNTANT)
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400050")
                .specializations("COMPANY_INCORPORATION, AUDIT_ASSURANCE")
                .verificationStatus(VerificationStatus.VERIFIED)
                .visibilityStatus(VisibilityStatus.PUBLIC)
                .isPublished(true)
                .build());

        locationRepository.save(PracticeLocationEntity.builder()
                .organizationId(orgMumbai.getId())
                .marketplaceProfileId(profileMumbai.getId())
                .locationName("Bandra West Office")
                .addressLine1("Hill Road, Bandra West")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400050")
                .latitude(new BigDecimal("19.059600"))
                .longitude(new BigDecimal("72.829500"))
                .isPrimary(true)
                .isActive(true)
                .build());

        // 5. Private Bangalore Practice (Should never appear in public discovery)
        profilePrivate = profileRepository.save(MarketplaceProfileEntity.builder()
                .organizationId(UUID.randomUUID())
                .displayName("Secret Private Practice")
                .slug("secret-private-practice")
                .email("secret@practice.com")
                .professionalType(ProfessionalType.CHARTERED_ACCOUNTANT)
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560001")
                .visibilityStatus(VisibilityStatus.PRIVATE)
                .isPublished(false)
                .build());

        locationRepository.save(PracticeLocationEntity.builder()
                .organizationId(profilePrivate.getOrganizationId())
                .marketplaceProfileId(profilePrivate.getId())
                .locationName("Private Office")
                .addressLine1("MG Road 10")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560001")
                .latitude(new BigDecimal("12.971600"))
                .longitude(new BigDecimal("77.594600"))
                .isActive(true)
                .build());
    }

    @Test
    @DisplayName("Public Geo Search: customer in Bangalore finds nearby practice within 10 km, Mumbai is excluded")
    void testPublicGeoSearch_FindsNearbyAndExcludesFarPractices() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/search")
                        .param("latitude", "12.9716")
                        .param("longitude", "77.5946")
                        .param("radiusKm", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].displayName").value("Apex Bangalore Advisors"))
                .andExpect(jsonPath("$.data.content[0].distanceKm", notNullValue()))
                .andExpect(jsonPath("$.data.content[0].nearestLocation.locationName").value("Indiranagar Hub"))
                .andExpect(jsonPath("$.data.content[*].displayName", not(hasItem("Apex Mumbai Advisory"))))
                .andExpect(jsonPath("$.data.content[*].displayName", not(hasItem("Secret Private Practice"))));
    }

    @Test
    @DisplayName("Public Geo Search: multiple branches return practice once with nearest distance")
    void testPublicGeoSearch_MultipleBranches_ReturnsSinglePracticeWithNearestDistance() throws Exception {
        // Radius 25 km covers both Indiranagar (~4.5 km) and Whitefield (~17 km)
        mockMvc.perform(get("/api/v1/marketplace/search")
                        .param("latitude", "12.9716")
                        .param("longitude", "77.5946")
                        .param("radiusKm", "25")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].displayName").value("Apex Bangalore Advisors"))
                .andExpect(jsonPath("$.data.content[0].nearestLocation.locationName").value("Indiranagar Hub"));
    }

    @Test
    @DisplayName("Public Administrative Search: search by pincode works without coordinates")
    void testAdministrativeSearch_ByPincode() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/search")
                        .param("pincode", "560038")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].displayName").value("Apex Bangalore Advisors"));
    }

    @Test
    @DisplayName("Public Administrative Search: search by city works without coordinates")
    void testAdministrativeSearch_ByCity() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/search")
                        .param("city", "Mumbai")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].displayName").value("Apex Mumbai Advisory"));
    }

    @Test
    @DisplayName("Validation: missing longitude when latitude is present is rejected with 400 Bad Request")
    void testGeoSearchValidation_MissingLongitude() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/search")
                        .param("latitude", "12.9716")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Validation: radius above maximum (e.g. 250 km) is rejected with 400 Bad Request")
    void testGeoSearchValidation_ExcessiveRadius() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/search")
                        .param("latitude", "12.9716")
                        .param("longitude", "77.5946")
                        .param("radiusKm", "250")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
