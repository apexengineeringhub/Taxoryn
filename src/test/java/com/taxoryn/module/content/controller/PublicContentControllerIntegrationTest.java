package com.taxoryn.module.content.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.content.entity.ContentEntity;
import com.taxoryn.module.content.entity.ContentOwnershipScope;
import com.taxoryn.module.content.entity.ContentStatus;
import com.taxoryn.module.content.entity.ContentType;
import com.taxoryn.module.content.repository.ContentRepository;
import com.taxoryn.module.content.repository.ContentTagRepository;
import com.taxoryn.module.marketplace.entity.TaxServiceCategoryEntity;
import com.taxoryn.module.marketplace.entity.TaxServiceEntity;
import com.taxoryn.module.marketplace.repository.TaxServiceCategoryRepository;
import com.taxoryn.module.marketplace.repository.TaxServiceRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicContentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private ContentTagRepository tagRepository;

    @Autowired
    private TaxServiceCategoryRepository categoryRepository;

    @Autowired
    private TaxServiceRepository taxServiceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private TaxServiceCategoryEntity gstCategory;
    private TaxServiceCategoryEntity itrCategory;
    private TaxServiceEntity gstFilingService;
    private UserEntity authorUser;

    @BeforeEach
    void setUp() {
        contentRepository.deleteAll();
        tagRepository.deleteAll();
        taxServiceRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        OrganizationEntity platformOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Taxoryn Platform Operations")
                .legalName("Taxoryn Global Inc")
                .email("admin@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        authorUser = userRepository.save(UserEntity.builder()
                .organizationId(platformOrg.getId())
                .email("editor@taxoryn.com")
                .firstName("Rohan")
                .lastName("Verma")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>())
                .build());

        gstCategory = categoryRepository.save(TaxServiceCategoryEntity.builder()
                .code("GST")
                .name("Goods and Services Tax")
                .description("GST registration and compliance")
                .sortOrder(1)
                .isActive(true)
                .build());

        itrCategory = categoryRepository.save(TaxServiceCategoryEntity.builder()
                .code("ITR")
                .name("Income Tax Returns")
                .description("ITR-1 through ITR-7 filing")
                .sortOrder(2)
                .isActive(true)
                .build());

        gstFilingService = taxServiceRepository.save(TaxServiceEntity.builder()
                .categoryId(gstCategory.getId())
                .code("GST_RETURN_FILING")
                .name("GST Return Filing")
                .description("Monthly GSTR-1 and GSTR-3B filings")
                .sortOrder(1)
                .isActive(true)
                .build());

        // 1. Seed 1 PUBLISHED Article (GST)
        contentRepository.save(ContentEntity.builder()
                .contentType(ContentType.ARTICLE)
                .title("Complete Guide to GST Invoicing Rules")
                .slug("complete-guide-gst-invoicing-rules")
                .summary("Understanding mandatory B2B e-invoicing thresholds in India")
                .body("Detailed article on e-invoicing...")
                .status(ContentStatus.PUBLISHED)
                .scope(ContentOwnershipScope.PLATFORM)
                .authorId(authorUser.getId())
                .categoryId(gstCategory.getId())
                .taxServiceId(gstFilingService.getId())
                .publishedAt(Instant.now().minus(2, ChronoUnit.DAYS))
                .build());

        // 2. Seed 1 PUBLISHED Video (GST)
        contentRepository.save(ContentEntity.builder()
                .contentType(ContentType.VIDEO)
                .title("How to Reconcile GSTR-2B with Purchase Register")
                .slug("how-to-reconcile-gstr-2b")
                .summary("5-minute walkthrough of ITC reconciliation")
                .body("Video script and overview...")
                .status(ContentStatus.PUBLISHED)
                .scope(ContentOwnershipScope.PLATFORM)
                .authorId(authorUser.getId())
                .categoryId(gstCategory.getId())
                .taxServiceId(gstFilingService.getId())
                .publishedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build());

        // 3. Seed 1 PUBLISHED FAQ (ITR)
        contentRepository.save(ContentEntity.builder()
                .contentType(ContentType.FAQ)
                .title("What is the penalty for late ITR filing?")
                .slug("penalty-for-late-itr-filing")
                .summary("Section 234F mandates fee up to Rs. 5000.")
                .body("Late filing fee under Section 234F...")
                .status(ContentStatus.PUBLISHED)
                .scope(ContentOwnershipScope.PLATFORM)
                .authorId(authorUser.getId())
                .categoryId(itrCategory.getId())
                .publishedAt(Instant.now().minus(12, ChronoUnit.HOURS))
                .build());

        // 4. Seed DRAFT, UNDER_REVIEW, APPROVED, and ARCHIVED content (must never be returned in public API)
        contentRepository.save(ContentEntity.builder()
                .contentType(ContentType.ARTICLE)
                .title("Secret Draft Article on Tax Avoidance")
                .slug("secret-draft-article")
                .summary("Unpublished internal draft")
                .body("Confidential text...")
                .status(ContentStatus.DRAFT)
                .scope(ContentOwnershipScope.PLATFORM)
                .authorId(authorUser.getId())
                .build());

        contentRepository.save(ContentEntity.builder()
                .contentType(ContentType.GUIDE)
                .title("Pending Review Guide")
                .slug("pending-review-guide")
                .summary("Under review by editor")
                .body("Pending review...")
                .status(ContentStatus.UNDER_REVIEW)
                .scope(ContentOwnershipScope.PLATFORM)
                .authorId(authorUser.getId())
                .build());

        contentRepository.save(ContentEntity.builder()
                .contentType(ContentType.ARTICLE)
                .title("Archived Old Budget 2020 Rules")
                .slug("archived-old-budget-2020")
                .summary("Obsolete tax rules")
                .body("Archived text...")
                .status(ContentStatus.ARCHIVED)
                .scope(ContentOwnershipScope.PLATFORM)
                .authorId(authorUser.getId())
                .build());
    }

    @AfterEach
    void tearDown() {
        contentRepository.deleteAll();
        tagRepository.deleteAll();
        taxServiceRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        TenantContext.clear();
    }

    @Test
    @DisplayName("1. Public content endpoint returns only PUBLISHED items without authentication")
    void testPublicContentListOnlyPublished() throws Exception {
        mockMvc.perform(get("/api/v1/public/content")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.content", hasSize(3)))
                .andExpect(jsonPath("$.data.content[*].status", everyItem(is("PUBLISHED"))))
                .andExpect(jsonPath("$.data.content[*].slug", not(hasItem("secret-draft-article"))))
                .andExpect(jsonPath("$.data.content[*].slug", not(hasItem("pending-review-guide"))))
                .andExpect(jsonPath("$.data.content[*].slug", not(hasItem("archived-old-budget-2020"))));
    }

    @Test
    @DisplayName("2. Public content retrieval by slug succeeds for PUBLISHED content")
    void testGetPublicContentBySlugSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/public/content/complete-guide-gst-invoicing-rules")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Complete Guide to GST Invoicing Rules"))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.categoryCode").value("GST"))
                .andExpect(jsonPath("$.data.taxServiceCode").value("GST_RETURN_FILING"))
                .andExpect(jsonPath("$.data.publicReady").value(true));
    }

    @Test
    @DisplayName("3. Public content retrieval by slug returns 404 for DRAFT or ARCHIVED content")
    void testGetPublicContentBySlugRejectsNonPublished() throws Exception {
        // DRAFT slug returns 404
        mockMvc.perform(get("/api/v1/public/content/secret-draft-article")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        // ARCHIVED slug returns 404
        mockMvc.perform(get("/api/v1/public/content/archived-old-budget-2020")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("4. Public categories returns active categories with published content counts")
    void testGetPublicCategoriesWithCounts() throws Exception {
        mockMvc.perform(get("/api/v1/public/content/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].code").value("GST"))
                .andExpect(jsonPath("$.data[0].publishedContentCount").value(2))
                .andExpect(jsonPath("$.data[1].code").value("ITR"))
                .andExpect(jsonPath("$.data[1].publishedContentCount").value(1));
    }

    @Test
    @DisplayName("5. Public related content endpoint returns related published items")
    void testGetRelatedPublicContent() throws Exception {
        mockMvc.perform(get("/api/v1/public/content/complete-guide-gst-invoicing-rules/related?limit=2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", not(empty())))
                .andExpect(jsonPath("$.data[0].slug").value("how-to-reconcile-gstr-2b"));
    }

    @Test
    @DisplayName("6. Public search and category filter work on published content")
    void testPublicSearchAndFilter() throws Exception {
        // Filter by category ITR
        mockMvc.perform(get("/api/v1/public/content?categoryId=" + itrCategory.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].slug").value("penalty-for-late-itr-filing"));

        // Search by keyword "reconcile"
        mockMvc.perform(get("/api/v1/public/content?search=reconcile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].slug").value("how-to-reconcile-gstr-2b"));
    }
}
