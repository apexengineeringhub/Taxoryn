package com.taxoryn.module.content.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.module.content.dto.CreateContentRequest;
import com.taxoryn.module.content.dto.RejectContentRequest;
import com.taxoryn.module.content.dto.ScheduleContentRequest;
import com.taxoryn.module.content.entity.ContentEntity;
import com.taxoryn.module.content.entity.ContentStatus;
import com.taxoryn.module.content.entity.ContentType;
import com.taxoryn.module.content.repository.ContentRepository;
import com.taxoryn.module.content.repository.ContentVersionRepository;
import com.taxoryn.module.content.repository.MediaAssetRepository;
import com.taxoryn.module.content.service.ContentService;
import com.taxoryn.module.marketplace.entity.TaxServiceCategoryEntity;
import com.taxoryn.module.marketplace.entity.TaxServiceEntity;
import com.taxoryn.module.marketplace.repository.TaxServiceCategoryRepository;
import com.taxoryn.module.marketplace.repository.TaxServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ContentStudioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private ContentVersionRepository contentVersionRepository;

    @Autowired
    private MediaAssetRepository mediaAssetRepository;

    @Autowired
    private TaxServiceCategoryRepository categoryRepository;

    @Autowired
    private TaxServiceRepository taxServiceRepository;

    @Autowired
    private ContentService contentService;

    private TaxServiceCategoryEntity testCategory;
    private TaxServiceEntity testTaxService;

    @BeforeEach
    void setUp() {
        testCategory = categoryRepository.save(
                TaxServiceCategoryEntity.builder()
                        .code("GST_" + UUID.randomUUID().toString().substring(0, 8))
                        .name("GST Compliance Services")
                        .description("Goods and Services Tax filings")
                        .sortOrder(1)
                        .isActive(true)
                        .build()
        );

        testTaxService = taxServiceRepository.save(
                TaxServiceEntity.builder()
                        .categoryId(testCategory.getId())
                        .code("GST_REG_" + UUID.randomUUID().toString().substring(0, 8))
                        .name("New GST Registration")
                        .description("Complete GST registration with ARN")
                        .sortOrder(1)
                        .isActive(true)
                        .build()
        );
    }

    @Test
    @DisplayName("1. Content Studio Dashboard Stats Endpoint returns operational metrics")
    @WithMockUser(username = "content.studio.admin@taxoryn.com", roles = {"TAXORYN_CONTENT_ADMIN"})
    void testGetDashboardStats() throws Exception {
        mockMvc.perform(get("/api/v1/admin/content/dashboard-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalContent").isNumber())
                .andExpect(jsonPath("$.data.publishedCount").isNumber())
                .andExpect(jsonPath("$.data.draftCount").isNumber())
                .andExpect(jsonPath("$.data.inReviewCount").isNumber())
                .andExpect(jsonPath("$.data.scheduledCount").isNumber())
                .andExpect(jsonPath("$.data.archivedCount").isNumber())
                .andExpect(jsonPath("$.data.needsAttention").isArray())
                .andExpect(jsonPath("$.data.recentActivity").isArray());
    }

    @Test
    @DisplayName("2. Complete Content Lifecycle: Draft -> Submit -> In Review -> Reject with Reason -> Resubmit -> Approve -> Schedule -> Auto Publish -> Archive -> Restore")
    @WithMockUser(username = "content.studio.admin@taxoryn.com", roles = {"TAXORYN_CONTENT_ADMIN"})
    void testFullContentLifecycle() throws Exception {
        // Step A: Create DRAFT content with Media URLs and Tax Services
        CreateContentRequest createReq = CreateContentRequest.builder()
                .contentType(ContentType.ARTICLE)
                .title("Complete 2026 GST Audit Guide for Businesses " + UUID.randomUUID().toString().substring(0, 6))
                .summary("A deep dive into GST statutory audits, reconciliations, and reporting.")
                .body("## Introduction to GST Audit\n\nEvery registered entity meeting the turnover criteria...")
                .thumbnailUrl("https://taxoryn.com/cdn/gst-thumb.png")
                .featuredImageUrl("https://taxoryn.com/cdn/gst-featured.png")
                .altText("GST Audit Guide Banner")
                .categoryId(testCategory.getId())
                .taxServiceId(testTaxService.getId())
                .taxServiceIds(Set.of(testTaxService.getId()))
                .tags(Set.of("GST Audit", "Compliance 2026"))
                .build();

        String createResStr = mockMvc.perform(post("/api/v1/admin/content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.featuredImageUrl").value("https://taxoryn.com/cdn/gst-featured.png"))
                .andExpect(jsonPath("$.data.altText").value("GST Audit Guide Banner"))
                .andReturn().getResponse().getContentAsString();

        UUID contentId = UUID.fromString(objectMapper.readTree(createResStr).path("data").path("id").asText());

        // Step B: Submit for Review
        mockMvc.perform(post("/api/v1/admin/content/{id}/submit-review", contentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));

        // Step C: Start Review
        mockMvc.perform(post("/api/v1/admin/content/{id}/start-review", contentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_REVIEW"));

        // Step D: Reject with mandatory reason
        RejectContentRequest rejectReq = RejectContentRequest.builder()
                .reason("Please clarify Section 35(5) reconciliation requirements.")
                .build();

        mockMvc.perform(post("/api/v1/admin/content/{id}/reject", contentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReason").value("Please clarify Section 35(5) reconciliation requirements."));

        // Verify rejection in database
        ContentEntity rejectedEntity = contentRepository.findById(contentId).orElseThrow();
        assertThat(rejectedEntity.getStatus()).isEqualTo(ContentStatus.REJECTED);
        assertThat(rejectedEntity.getRejectionReason()).isEqualTo("Please clarify Section 35(5) reconciliation requirements.");

        // Step E: Resubmit after author updates
        mockMvc.perform(post("/api/v1/admin/content/{id}/submit-review", contentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.rejectionReason").doesNotExist());

        // Step F: Approve Content
        mockMvc.perform(post("/api/v1/admin/content/{id}/approve", contentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // Step G: Schedule Publication for the future
        Instant scheduledTime = Instant.now().plus(2, ChronoUnit.HOURS);
        ScheduleContentRequest scheduleReq = ScheduleContentRequest.builder()
                .scheduledPublishAt(scheduledTime)
                .build();

        mockMvc.perform(post("/api/v1/admin/content/{id}/schedule", contentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(scheduleReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"));

        ContentEntity scheduledEntity = contentRepository.findById(contentId).orElseThrow();
        assertThat(scheduledEntity.getStatus()).isEqualTo(ContentStatus.SCHEDULED);

        // Step H: Test background schedule runner auto-publishing when schedule arrives
        scheduledEntity.setScheduledPublishAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        contentRepository.save(scheduledEntity);

        int publishedCount = contentService.publishScheduledContent();
        assertThat(publishedCount).isGreaterThanOrEqualTo(1);

        ContentEntity autoPublishedEntity = contentRepository.findById(contentId).orElseThrow();
        assertThat(autoPublishedEntity.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(autoPublishedEntity.getPublishedAt()).isNotNull();

        // Step I: Verify Version Snapshot was recorded
        mockMvc.perform(get("/api/v1/admin/content/{id}/versions", contentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));

        // Step J: Archive Content
        mockMvc.perform(post("/api/v1/admin/content/{id}/archive", contentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        // Step K: Restore Archived Content to DRAFT
        mockMvc.perform(post("/api/v1/admin/content/{id}/restore", contentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        ContentEntity restoredEntity = contentRepository.findById(contentId).orElseThrow();
        assertThat(restoredEntity.getStatus()).isEqualTo(ContentStatus.DRAFT);
    }

    @Test
    @DisplayName("3. Media Library: Upload valid image, stream binary content, and update alt text")
    @WithMockUser(username = "content.studio.admin@taxoryn.com", roles = {"TAXORYN_CONTENT_ADMIN"})
    void testMediaLibraryWorkflow() throws Exception {
        byte[] dummyPngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample-infographic.png",
                "image/png",
                dummyPngBytes
        );

        // 1. Upload
        String uploadRes = mockMvc.perform(multipart("/api/v1/admin/content/media/upload")
                        .file(file)
                        .param("altText", "GST Filing Infographic"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.filename").value("sample-infographic.png"))
                .andExpect(jsonPath("$.data.contentType").value("image/png"))
                .andExpect(jsonPath("$.data.altText").value("GST Filing Infographic"))
                .andReturn().getResponse().getContentAsString();

        UUID mediaId = UUID.fromString(objectMapper.readTree(uploadRes).path("data").path("id").asText());

        // 2. Stream Public Binary Media
        mockMvc.perform(get("/api/v1/public/media/{id}", mediaId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("image/png")))
                .andExpect(content().bytes(dummyPngBytes));

        // 3. Update Alt Text
        mockMvc.perform(put("/api/v1/admin/content/media/{id}", mediaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"altText\": \"Updated GST Filing Infographic 2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.altText").value("Updated GST Filing Infographic 2026"));

        // 4. List Media Assets
        mockMvc.perform(get("/api/v1/admin/content/media"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("4. Media Upload Validation: Reject invalid file type (e.g. application/pdf or text/plain)")
    @WithMockUser(username = "content.studio.admin@taxoryn.com", roles = {"TAXORYN_CONTENT_ADMIN"})
    void testMediaUploadInvalidType() throws Exception {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "document.exe",
                "application/x-msdownload",
                "binary-content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/admin/content/media/upload")
                        .file(invalidFile)
                        .param("altText", "Executable"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("5. Read-Only Controlled Tax Service Master Reference endpoint returns active catalog")
    @WithMockUser(username = "content.studio.admin@taxoryn.com", roles = {"TAXORYN_CONTENT_ADMIN"})
    void testGetControlledTaxServices() throws Exception {
        mockMvc.perform(get("/api/v1/admin/content/tax-services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].code").isNotEmpty());
    }

    @Test
    @DisplayName("6. Security: Unauthorized practice staff cannot access Content Studio endpoints")
    @WithMockUser(username = "staff@practice.com", roles = {"PRACTICE_EMPLOYEE"})
    void testSecurityUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/content/dashboard-stats"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/content/review-queue"))
                .andExpect(status().isForbidden());
    }
}
