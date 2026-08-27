package com.taxoryn.module.content.controller;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.content.entity.ContentEntity;
import com.taxoryn.module.content.entity.ContentOwnershipScope;
import com.taxoryn.module.content.entity.ContentSlugRedirectEntity;
import com.taxoryn.module.content.entity.ContentStatus;
import com.taxoryn.module.content.entity.ContentType;
import com.taxoryn.module.content.repository.ContentRepository;
import com.taxoryn.module.content.repository.ContentSlugRedirectRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicSeoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private ContentSlugRedirectRepository slugRedirectRepository;

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

    private ContentEntity publishedArticle;
    private ContentEntity publishedUpdate;

    @BeforeEach
    void setUp() {
        slugRedirectRepository.deleteAll();
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

        UserEntity authorUser = userRepository.save(UserEntity.builder()
                .organizationId(platformOrg.getId())
                .email("editor@taxoryn.com")
                .firstName("Rohan")
                .lastName("Verma")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>())
                .build());

        TaxServiceCategoryEntity gstCategory = categoryRepository.save(TaxServiceCategoryEntity.builder()
                .code("GST")
                .name("Goods and Services Tax")
                .description("GST registration and compliance")
                .sortOrder(1)
                .isActive(true)
                .build());

        TaxServiceEntity gstFilingService = taxServiceRepository.save(TaxServiceEntity.builder()
                .categoryId(gstCategory.getId())
                .code("GST_RETURN_FILING")
                .name("GST Return Filing")
                .description("Monthly GSTR-1 and GSTR-3B filings")
                .sortOrder(1)
                .isActive(true)
                .build());

        // 1. Published Article with custom SEO fields
        publishedArticle = contentRepository.save(ContentEntity.builder()
                .contentType(ContentType.ARTICLE)
                .title("Complete GST Return Filing Guide")
                .slug("gst-return-filing-guide")
                .summary("Comprehensive guide to filing GSTR-1 and GSTR-3B.")
                .body("Step by step instructions for GST filing...")
                .seoTitle("GST Return Filing Guide | Expert Tax Steps")
                .metaDescription("Learn step-by-step how to file GST returns on time and avoid penalties.")
                .canonicalUrl("https://taxoryn.com/learn/gst-return-filing-guide")
                .status(ContentStatus.PUBLISHED)
                .scope(ContentOwnershipScope.PLATFORM)
                .authorId(authorUser.getId())
                .categoryId(gstCategory.getId())
                .taxServiceId(gstFilingService.getId())
                .publishedAt(Instant.now().minus(3, ChronoUnit.DAYS))
                .build());

        // 2. Published Tax Update
        publishedUpdate = contentRepository.save(ContentEntity.builder()
                .contentType(ContentType.TAX_UPDATE)
                .title("CBIC Notification 12/2026 E-Invoicing Threshold")
                .slug("cbic-notification-12-2026-e-invoicing")
                .summary("New mandatory threshold notification for B2B e-invoicing.")
                .body("CBIC issues new circular...")
                .status(ContentStatus.PUBLISHED)
                .scope(ContentOwnershipScope.PLATFORM)
                .authorId(authorUser.getId())
                .categoryId(gstCategory.getId())
                .taxServiceId(gstFilingService.getId())
                .publishedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build());

        // 3. Draft Article (Must NOT appear in sitemap or public APIs)
        contentRepository.save(ContentEntity.builder()
                .contentType(ContentType.ARTICLE)
                .title("Internal Draft Not For Search Engines")
                .slug("internal-draft-not-for-search-engines")
                .summary("Draft summary")
                .body("Draft body...")
                .status(ContentStatus.DRAFT)
                .scope(ContentOwnershipScope.PLATFORM)
                .authorId(authorUser.getId())
                .build());

        // 4. Archived Content (Must NOT appear in sitemap or public APIs)
        contentRepository.save(ContentEntity.builder()
                .contentType(ContentType.GUIDE)
                .title("Archived Old TDS Rates 2019")
                .slug("archived-old-tds-rates-2019")
                .summary("Obsolete TDS rules")
                .body("Archived body...")
                .status(ContentStatus.ARCHIVED)
                .scope(ContentOwnershipScope.PLATFORM)
                .authorId(authorUser.getId())
                .build());

        // 5. Slug Redirect: old slug -> new slug
        slugRedirectRepository.save(ContentSlugRedirectEntity.builder()
                .oldSlug("old-gst-filing-guide-2025")
                .newSlug("gst-return-filing-guide")
                .contentId(publishedArticle.getId())
                .build());
    }

    @AfterEach
    void tearDown() {
        slugRedirectRepository.deleteAll();
        contentRepository.deleteAll();
        tagRepository.deleteAll();
        taxServiceRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        TenantContext.clear();
    }

    @Test
    @DisplayName("1. /robots.txt allows public Learn & Marketplace paths and provides sitemap reference")
    void testRobotsTxt() throws Exception {
        mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(containsString("Allow: /learn")))
                .andExpect(content().string(containsString("Allow: /learn/*")))
                .andExpect(content().string(containsString("Allow: /marketplace")))
                .andExpect(content().string(containsString("Disallow: /admin/")))
                .andExpect(content().string(containsString("Disallow: /api/v1/admin/")))
                .andExpect(content().string(containsString("Disallow: /portal/")))
                .andExpect(content().string(containsString("Sitemap: https://taxoryn.com/sitemap.xml")));
    }

    @Test
    @DisplayName("2. /sitemap.xml generates standard XML with static routes, published content, and excludes non-published")
    void testSitemapXml() throws Exception {
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                // Verify static routes
                .andExpect(content().string(containsString("<loc>https://taxoryn.com/learn</loc>")))
                .andExpect(content().string(containsString("<loc>https://taxoryn.com/learn/articles</loc>")))
                .andExpect(content().string(containsString("<loc>https://taxoryn.com/learn/videos</loc>")))
                .andExpect(content().string(containsString("<loc>https://taxoryn.com/learn/guides</loc>")))
                .andExpect(content().string(containsString("<loc>https://taxoryn.com/learn/faqs</loc>")))
                .andExpect(content().string(containsString("<loc>https://taxoryn.com/learn/tax-updates</loc>")))
                .andExpect(content().string(containsString("<loc>https://taxoryn.com/marketplace</loc>")))
                // Verify published dynamic content
                .andExpect(content().string(containsString("<loc>https://taxoryn.com/learn/gst-return-filing-guide</loc>")))
                .andExpect(content().string(containsString("<loc>https://taxoryn.com/learn/cbic-notification-12-2026-e-invoicing</loc>")))
                // Verify exclusions of draft and archived
                .andExpect(content().string(not(containsString("internal-draft-not-for-search-engines"))))
                .andExpect(content().string(not(containsString("archived-old-tds-rates-2019"))));
    }

    @Test
    @DisplayName("3. Structured sitemap JSON endpoint returns published items with metadata")
    void testSitemapJson() throws Exception {
        mockMvc.perform(get("/api/v1/public/seo/sitemap")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].loc", hasItem("https://taxoryn.com/learn/gst-return-filing-guide")))
                .andExpect(jsonPath("$.data[*].loc", hasItem("https://taxoryn.com/learn/cbic-notification-12-2026-e-invoicing")))
                .andExpect(jsonPath("$.data[*].loc", not(hasItem(containsString("internal-draft")))))
                .andExpect(jsonPath("$.data[*].loc", not(hasItem(containsString("archived-old")))));
    }

    @Test
    @DisplayName("4. Public content by slug returns SEO fields (seoTitle, metaDescription, canonicalUrl)")
    void testGetPublicContentReturnsSeoFields() throws Exception {
        mockMvc.perform(get("/api/v1/public/content/gst-return-filing-guide")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value("gst-return-filing-guide"))
                .andExpect(jsonPath("$.data.title").value("Complete GST Return Filing Guide"))
                .andExpect(jsonPath("$.data.seoTitle").value("GST Return Filing Guide | Expert Tax Steps"))
                .andExpect(jsonPath("$.data.metaDescription").value("Learn step-by-step how to file GST returns on time and avoid penalties."))
                .andExpect(jsonPath("$.data.canonicalUrl").value("https://taxoryn.com/learn/gst-return-filing-guide"))
                .andExpect(jsonPath("$.data.taxServiceCode").value("GST_RETURN_FILING"));
    }

    @Test
    @DisplayName("5. Accessing content via old slug returns redirectSlug for 301 alias handling")
    void testOldSlugAliasRedirect() throws Exception {
        mockMvc.perform(get("/api/v1/public/content/old-gst-filing-guide-2025")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value("gst-return-filing-guide"))
                .andExpect(jsonPath("$.data.redirectSlug").value("gst-return-filing-guide"));
    }

    @Test
    @DisplayName("6. Nonexistent slug returns 404 NOT_FOUND")
    void testNonexistentSlugReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/public/content/non-existent-topic-slug-12345")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
