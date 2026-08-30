package com.taxoryn.module.marketplace.repository;

import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.VisibilityStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.VerificationStatus;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MarketplaceDatabaseIntegrationTest {

    @Autowired
    private MarketplaceProfileRepository marketplaceProfileRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private com.taxoryn.module.marketplace.repository.PracticeServiceRepository practiceServiceRepository;

    @Autowired
    private com.taxoryn.module.marketplace.repository.PracticeLocationRepository practiceLocationRepository;

    @Autowired
    private com.taxoryn.module.marketplace.repository.MarketplaceReviewRepository marketplaceReviewRepository;

    @Autowired
    private com.taxoryn.module.marketplace.repository.MarketplaceLeadRepository marketplaceLeadRepository;

    @Autowired
    private com.taxoryn.module.marketplace.repository.MarketplaceProfileSlugRedirectRepository slugRedirectRepository;

    private OrganizationEntity org;

    @BeforeEach
    void setUp() {
        // The H2 in-memory test database is shared across the whole test JVM run
        // (see application-test.yml: DB_CLOSE_DELAY=-1). Some marketplace controller
        // integration tests are not @Transactional and commit rows referencing
        // marketplace_profiles (practice services, locations, reviews, leads, slug
        // redirects). Those child rows must be cleared before profiles are deleted
        // here, or the FK constraint on marketplace_practice_services (and similar)
        // fails deleteAll() below.
        marketplaceLeadRepository.deleteAll();
        marketplaceReviewRepository.deleteAll();
        practiceServiceRepository.deleteAll();
        practiceLocationRepository.deleteAll();
        slugRedirectRepository.deleteAll();
        marketplaceProfileRepository.deleteAll();
        organizationRepository.deleteAll();

        org = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Database Test Firm")
                .email("dbtest@apextax.com")
                .city("Bangalore")
                .state("Karnataka")
                .status(OrganizationStatus.ACTIVE)
                .build());
    }

    @Test
    @DisplayName("Database: Save practice profile and retrieve by organization ID")
    void testSaveAndRetrieveByOrganizationId() {
        MarketplaceProfileEntity profile = MarketplaceProfileEntity.builder()
                .organizationId(org.getId())
                .displayName("Apex Database Test Firm")
                .slug("apex-database-test-firm")
                .bio("Specialized direct and indirect tax practice")
                .professionalType(MarketplaceProfileEntity.ProfessionalType.CHARTERED_ACCOUNTANT)
                .experienceYears(15)
                .city("Bangalore")
                .state("Karnataka")
                .pincode("560001")
                .phone("+919876543210")
                .email("contact@apextax.com")
                .websiteUrl("https://apextax.com")
                .startingFee(new BigDecimal("1500.00"))
                .hourlyRate(new BigDecimal("2500.00"))
                .visibilityStatus(VisibilityStatus.PUBLIC)
                .verificationStatus(VerificationStatus.VERIFIED)
                .isPublished(true)
                .build();

        MarketplaceProfileEntity saved = marketplaceProfileRepository.save(profile);
        assertNotNull(saved.getId());

        Optional<MarketplaceProfileEntity> retrieved = marketplaceProfileRepository.findByOrganizationId(org.getId());
        assertTrue(retrieved.isPresent());
        assertEquals("Apex Database Test Firm", retrieved.get().getDisplayName());
        assertEquals("apex-database-test-firm", retrieved.get().getSlug());
        assertEquals(VisibilityStatus.PUBLIC, retrieved.get().getVisibilityStatus());
        assertEquals(VerificationStatus.VERIFIED, retrieved.get().getVerificationStatus());
        assertEquals(15, retrieved.get().getExperienceYears());
    }

    @Test
    @DisplayName("Database: Find profile by unique public vanity slug")
    void testFindBySlug() {
        MarketplaceProfileEntity profile = MarketplaceProfileEntity.builder()
                .organizationId(org.getId())
                .displayName("Apex Global Tax")
                .slug("apex-global-tax")
                .city("Mumbai")
                .state("Maharashtra")
                .visibilityStatus(VisibilityStatus.PUBLIC)
                .build();

        marketplaceProfileRepository.save(profile);

        Optional<MarketplaceProfileEntity> found = marketplaceProfileRepository.findBySlug("apex-global-tax");
        assertTrue(found.isPresent());
        assertEquals(org.getId(), found.get().getOrganizationId());

        Optional<MarketplaceProfileEntity> notFound = marketplaceProfileRepository.findBySlug("non-existent-slug");
        assertTrue(notFound.isEmpty());
    }

    @Test
    @DisplayName("Database: existsBySlugAndIdNot detects collisions excluding own profile ID")
    void testExistsBySlugAndIdNot() {
        MarketplaceProfileEntity profileA = marketplaceProfileRepository.save(MarketplaceProfileEntity.builder()
                .organizationId(org.getId())
                .displayName("Alpha Practice")
                .slug("shared-slug")
                .city("Delhi")
                .state("Delhi")
                .build());

        UUID anotherId = UUID.randomUUID();

        // Another profile cannot use profileA's slug
        boolean collisionDetected = marketplaceProfileRepository.existsBySlugAndIdNot("shared-slug", anotherId);
        assertTrue(collisionDetected);

        // Profile A can keep its own slug
        boolean selfCollision = marketplaceProfileRepository.existsBySlugAndIdNot("shared-slug", profileA.getId());
        assertFalse(selfCollision);
    }
}
