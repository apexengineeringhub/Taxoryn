package com.taxoryn.module.content.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.module.content.dto.CreateContentRequest;
import com.taxoryn.module.content.dto.UpdateContentRequest;
import com.taxoryn.module.content.entity.ContentEntity;
import com.taxoryn.module.content.entity.ContentStatus;
import com.taxoryn.module.content.entity.ContentType;
import com.taxoryn.module.content.repository.ContentRepository;
import com.taxoryn.module.content.repository.ContentSlugRedirectRepository;
import com.taxoryn.module.content.service.ContentService;
import com.taxoryn.module.marketplace.entity.TaxServiceCategoryEntity;
import com.taxoryn.module.marketplace.entity.TaxServiceEntity;
import com.taxoryn.module.marketplace.repository.TaxServiceCategoryRepository;
import com.taxoryn.module.marketplace.repository.TaxServiceRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class LearnSeoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private ContentSlugRedirectRepository redirectRepository;

    @Autowired
    private TaxServiceCategoryRepository categoryRepository;

    @Autowired
    private TaxServiceRepository taxServiceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContentService contentService;

    private TaxServiceCategoryEntity testCategory;
    private TaxServiceEntity testTaxService;

    @BeforeEach
    void setUp() {
        redirectRepository.deleteAll();
        contentRepository.deleteAll();

        testCategory = categoryRepository.findByCodeIgnoreCase("GST").orElseGet(() -> {
            TaxServiceCategoryEntity cat = TaxServiceCategoryEntity.builder()
                    .code("GST")
                    .name("Goods and Services Tax")
                    .description("GST compliance and return filing")
                    .isActive(true)
                    .sortOrder(1)
                    .build();
            return categoryRepository.save(cat);
        });

        testTaxService = taxServiceRepository.findByCodeIgnoreCase("GST_RETURN_FILING").orElseGet(() -> {
            TaxServiceEntity svc = TaxServiceEntity.builder()
                    .categoryId(testCategory.getId())
                    .code("GST_RETURN_FILING")
                    .name("Monthly GSTR-3B & GSTR-1 Filing")
                    .description("Expert GST return filing")
                    .isActive(true)
                    .sortOrder(1)
                    .build();
            return taxServiceRepository.save(svc);
        });
    }

    @Test
    @DisplayName("Public crawler check: /robots.txt is accessible without authentication")
    void testRobotsTxtIsPublicAndCorrectlyConfigured() throws Exception {
        mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/plain")))
                .andExpect(content().string(containsString("Allow: /learn")))
                .andExpect(content().string(containsString("Allow: /marketplace")))
                .andExpect(content().string(containsString("Disallow: /api/v1/admin/")))
                .andExpect(content().string(containsString("Disallow: /api/v1/practice/")))
                .andExpect(content().string(containsString("Sitemap: https://taxoryn.com/sitemap.xml")));
    }

    @Test
    @DisplayName("Dynamic sitemap check: /sitemap.xml generates valid XML containing only published content")
    void testDynamicSitemapContainsOnlyPublishedContent() throws Exception {
        // 1. Create a published article
        ContentEntity publishedArticle = ContentEntity.builder()
                .contentType(ContentType.ARTICLE)
                .title("Complete GST Return Filing Guide")
                .slug("complete-gst-return-filing-guide")
                .summary("Step-by-step GST filing guidance for Indian businesses.")
                .body("## Filing Steps\n1. Login to GST Portal...")
                .status(ContentStatus.PUBLISHED)
                .scope(com.taxoryn.module.content.entity.ContentOwnershipScope.PLATFORM)
                .publishedAt(java.time.Instant.now())
                .categoryId(testCategory.getId())
                .taxServiceId(testTaxService.getId())
                .build();
        contentRepository.save(publishedArticle);

        // 2. Create a draft article (Must NOT appear in sitemap)
        ContentEntity draftArticle = ContentEntity.builder()
                .contentType(ContentType.ARTICLE)
                .title("Draft Internal Secret Guide")
                .slug("draft-internal-secret-guide")
                .summary("Draft content")
                .body("Draft body...")
                .status(ContentStatus.DRAFT)
                .scope(com.taxoryn.module.content.entity.ContentOwnershipScope.PLATFORM)
                .build();
        contentRepository.save(draftArticle);

        // 3. Create an archived article (Must NOT appear in sitemap)
        ContentEntity archivedArticle = ContentEntity.builder()
                .contentType(ContentType.ARTICLE)
                .title("Outdated Old Tax Circular 2018")
                .slug("outdated-old-tax-circular-2018")
                .summary("Archived content")
                .body("Archived body...")
                .status(ContentStatus.ARCHIVED)
                .scope(com.taxoryn.module.content.entity.ContentOwnershipScope.PLATFORM)
                .build();
        contentRepository.save(archivedArticle);

        // 4. Request /sitemap.xml
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/xml")))
                .andExpect(content().string(containsString("<loc>https://taxoryn.com/learn</loc>")))
                .andExpect(content().string(containsString("<loc>https://taxoryn.com/learn/articles</loc>")))
                .andExpect(content().string(containsString("<loc>https://taxoryn.com/learn/videos</loc>")))
                .andExpect(content().string(containsString("<loc>https://taxoryn.com/learn/complete-gst-return-filing-guide</loc>")))
                .andExpect(content().string(not(containsString("draft-internal-secret-guide"))))
                .andExpect(content().string(not(containsString("outdated-old-tax-circular-2018"))));
    }

    @Test
    @DisplayName("Slug stability: Updating title does NOT automatically change published slug")
    @WithMockUser(username = "editor@taxoryn.com", authorities = {"CONTENT_CREATE", "CONTENT_EDIT", "CONTENT_PUBLISH"})
    void testSlugStabilityOnTitleUpdate() throws Exception {
        CreateContentRequest createReq = CreateContentRequest.builder()
                .contentType(ContentType.ARTICLE)
                .title("Original GST Filing Title")
                .slug("original-gst-filing-title")
                .body("Detailed guide on GST filings.")
                .categoryId(testCategory.getId())
                .build();

        var created = contentService.createContent(createReq);
        contentService.submitForReview(created.getId());
        contentService.approveContent(created.getId());
        contentService.publishContent(created.getId());

        // Update Title only (slug is omitted / stable)
        UpdateContentRequest updateReq = UpdateContentRequest.builder()
                .title("Brand New Updated GST Master Title for 2026")
                .build();

        var updated = contentService.updateContent(created.getId(), updateReq);

        // Assert slug did not change automatically
        assertEquals("original-gst-filing-title", updated.getSlug());
        assertEquals("Brand New Updated GST Master Title for 2026", updated.getTitle());
    }

    @Test
    @DisplayName("Permanent 301 alias redirect: Explicit slug change creates redirect without chains")
    @WithMockUser(username = "editor@taxoryn.com", authorities = {"CONTENT_CREATE", "CONTENT_EDIT", "CONTENT_PUBLISH"})
    void testExplicitSlugChangeCreatesRedirectWithoutChains() throws Exception {
        // 1. Create and publish content with slug A
        CreateContentRequest createReq = CreateContentRequest.builder()
                .contentType(ContentType.ARTICLE)
                .title("Initial GST Guide")
                .slug("initial-gst-guide")
                .body("Initial body content.")
                .build();
        var created = contentService.createContent(createReq);
        contentService.submitForReview(created.getId());
        contentService.approveContent(created.getId());
        contentService.publishContent(created.getId());

        // 2. Change slug from "initial-gst-guide" to "updated-gst-guide"
        UpdateContentRequest update1 = UpdateContentRequest.builder()
                .slug("updated-gst-guide")
                .build();
        contentService.updateContent(created.getId(), update1);

        // 3. Change slug again from "updated-gst-guide" to "final-gst-guide"
        UpdateContentRequest update2 = UpdateContentRequest.builder()
                .slug("final-gst-guide")
                .build();
        contentService.updateContent(created.getId(), update2);

        // Verify redirect repository has flattened the chain directly:
        // "initial-gst-guide" -> "final-gst-guide" (NOT to intermediate!)
        var initialRedirect = redirectRepository.findByOldSlug("initial-gst-guide").orElseThrow();
        assertEquals("final-gst-guide", initialRedirect.getNewSlug());

        var intermediateRedirect = redirectRepository.findByOldSlug("updated-gst-guide").orElseThrow();
        assertEquals("final-gst-guide", intermediateRedirect.getNewSlug());

        // 4. Test public API querying old slug returns 200 with target redirectSlug
        mockMvc.perform(get("/api/v1/public/content/initial-gst-guide"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("final-gst-guide"))
                .andExpect(jsonPath("$.data.redirectSlug").value("final-gst-guide"));
    }

    @Test
    @DisplayName("SEO Metadata: Custom SEO title, meta description, and canonical URL are returned in public API")
    @WithMockUser(username = "editor@taxoryn.com", authorities = {"CONTENT_CREATE", "CONTENT_EDIT", "CONTENT_PUBLISH"})
    void testSeoMetadataFieldsReturnedInPublicContent() throws Exception {
        CreateContentRequest createReq = CreateContentRequest.builder()
                .contentType(ContentType.ARTICLE)
                .title("Comprehensive TDS Filing Guide")
                .slug("comprehensive-tds-filing-guide")
                .summary("Brief summary of TDS returns.")
                .body("Full TDS return filing guide instructions...")
                .seoTitle("Best TDS Filing Guide for Indian Startups | Taxoryn")
                .metaDescription("Learn essential TDS return deadlines, Form 26Q procedures, and avoid late filing penalties with Taxoryn.")
                .canonicalUrl("https://taxoryn.com/learn/comprehensive-tds-filing-guide")
                .build();

        var created = contentService.createContent(createReq);
        contentService.submitForReview(created.getId());
        contentService.approveContent(created.getId());
        contentService.publishContent(created.getId());

        mockMvc.perform(get("/api/v1/public/content/comprehensive-tds-filing-guide"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Comprehensive TDS Filing Guide"))
                .andExpect(jsonPath("$.data.seoTitle").value("Best TDS Filing Guide for Indian Startups | Taxoryn"))
                .andExpect(jsonPath("$.data.metaDescription").value("Learn essential TDS return deadlines, Form 26Q procedures, and avoid late filing penalties with Taxoryn."))
                .andExpect(jsonPath("$.data.canonicalUrl").value("https://taxoryn.com/learn/comprehensive-tds-filing-guide"));
    }

    @Test
    @DisplayName("Security & 404: Draft or non-existent slugs return 404 in public API")
    void testArchivedAndDraftContentReturns404() throws Exception {
        ContentEntity draft = ContentEntity.builder()
                .contentType(ContentType.ARTICLE)
                .title("Draft Unpublished Article")
                .slug("draft-unpublished-article")
                .body("Draft secret text...")
                .status(ContentStatus.DRAFT)
                .scope(com.taxoryn.module.content.entity.ContentOwnershipScope.PLATFORM)
                .build();
        contentRepository.save(draft);

        mockMvc.perform(get("/api/v1/public/content/draft-unpublished-article"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/public/content/completely-non-existent-topic"))
                .andExpect(status().isNotFound());
    }
}
