package com.taxoryn.module.marketplace.service;

import com.taxoryn.module.marketplace.dto.ProfileCompletenessDto;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.ProfessionalType;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProfileCompletenessCalculatorTest {

    private ProfileCompletenessCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ProfileCompletenessCalculator();
    }

    @Test
    @DisplayName("Profile Completeness: fully populated profile achieves 100% with all items completed")
    void testCalculate_FullyPopulatedProfile() {
        MarketplaceProfileEntity profile = MarketplaceProfileEntity.builder()
                .organizationId(UUID.randomUUID())
                .displayName("Apex Corporate & Tax Advisors")
                .headline("Leading GST & Corporate Tax Specialists")
                .bio("Comprehensive tax consultancy and advisory practice serving corporate enterprises and MSMEs across India.")
                .phone("9820011223")
                .email("contact@apextax.com")
                .city("Mumbai")
                .state("Maharashtra")
                .professionalType(ProfessionalType.CHARTERED_ACCOUNTANT)
                .experienceYears(12)
                .websiteUrl("https://apextax.com")
                .build();

        ProfileCompletenessDto result = calculator.calculate(profile, null);

        assertNotNull(result);
        assertEquals(100, result.getPercentage());
        assertTrue(result.getCompletedItems().contains("Practice name"));
        assertTrue(result.getCompletedItems().contains("Description"));
        assertTrue(result.getCompletedItems().contains("Phone"));
        assertTrue(result.getCompletedItems().contains("Email"));
        assertTrue(result.getCompletedItems().contains("Location"));
        assertTrue(result.getCompletedItems().contains("Experience"));
        assertTrue(result.getCompletedItems().contains("Website"));
        assertTrue(result.getMissingItems().isEmpty());
    }

    @Test
    @DisplayName("Profile Completeness: partially completed profile returns exact percentage, completedItems, and missingItems")
    void testCalculate_PartiallyCompletedProfile() {
        MarketplaceProfileEntity profile = MarketplaceProfileEntity.builder()
                .organizationId(UUID.randomUUID())
                .displayName("Sharma & Associates")
                .phone("9820099887")
                .bio("Short bio") // < 20 chars, so Description is not satisfied
                .professionalType(ProfessionalType.CHARTERED_ACCOUNTANT)
                .experienceYears(5)
                .build();

        OrganizationEntity org = OrganizationEntity.builder()
                .name("Sharma & Associates")
                .city("Pune")
                .state("Maharashtra")
                .email("sharma@associates.com")
                .build();

        ProfileCompletenessDto result = calculator.calculate(profile, org);

        assertNotNull(result);
        assertTrue(result.getPercentage() > 0 && result.getPercentage() < 100);
        assertTrue(result.getCompletedItems().contains("Practice name"));
        assertTrue(result.getCompletedItems().contains("Phone"));
        assertTrue(result.getCompletedItems().contains("Email")); // fallback from org
        assertTrue(result.getCompletedItems().contains("Location")); // fallback from org
        assertTrue(result.getCompletedItems().contains("Experience"));
        assertTrue(result.getMissingItems().contains("Description"));
        assertTrue(result.getMissingItems().contains("Website"));
    }

    @Test
    @DisplayName("Profile Completeness: empty profile returns 0% with all items in missingItems")
    void testCalculate_EmptyProfile() {
        ProfileCompletenessDto result = calculator.calculate(null, null);

        assertNotNull(result);
        assertEquals(0, result.getPercentage());
        assertTrue(result.getCompletedItems().isEmpty());
        assertFalse(result.getMissingItems().isEmpty());
    }
}
