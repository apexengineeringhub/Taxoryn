package com.taxoryn.module.content.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.module.content.dto.CreateContentRequest;
import com.taxoryn.module.content.entity.ContentEntity;
import com.taxoryn.module.content.entity.ContentStatus;
import com.taxoryn.module.content.entity.ContentType;
import com.taxoryn.module.content.repository.ContentRepository;
import com.taxoryn.module.marketplace.entity.TaxServiceCategoryEntity;
import com.taxoryn.module.marketplace.repository.TaxServiceCategoryRepository;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Video & YouTube Integration Tests")
class VideoContentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private TaxServiceCategoryRepository categoryRepository;

    private UserEntity contentAdminUser;
    private TaxServiceCategoryEntity testCategory;

    @BeforeEach
    void setUp() {
        contentRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        OrganizationEntity platformOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Taxoryn Platform Operations")
                .legalName("Taxoryn Global Inc")
                .email("admin@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        contentAdminUser = userRepository.save(UserEntity.builder()
                .organizationId(platformOrg.getId())
                .email("content.admin@taxoryn.platform")
                .passwordHash("SecuredPass123!")
                .firstName("Content")
                .lastName("Administrator")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>())
                .build());

        testCategory = categoryRepository.save(TaxServiceCategoryEntity.builder()
                .code("GST_TEST_VID")
                .name("GST Video Category")
                .description("Category for GST videos")
                .sortOrder(1)
                .isActive(true)
                .build());
    }

    @AfterEach
    void tearDown() {
        contentRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "content.admin@taxoryn.platform", roles = {"TAXORYN_CONTENT_ADMIN"})
    @DisplayName("Admin can create VIDEO content with valid YouTube URL")
    void testCreateVideoContentWithValidYouTubeUrl() throws Exception {
        CreateContentRequest request = CreateContentRequest.builder()
                .contentType(ContentType.VIDEO)
                .title("GST Return Filing Explained in 5 Minutes")
                .slug("gst-return-filing-explained-in-5-minutes")
                .summary("Understand GST return filing easily.")
                .body("Step 1: Reconcile GSTR-2B. Step 2: Fill GSTR-3B.")
                .youtubeUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
                .videoDurationSeconds(300)
                .categoryId(testCategory.getId())
                .tags(Set.of("GST", "VideoGuide"))
                .build();

        mockMvc.perform(post("/api/v1/admin/content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.contentType").value("VIDEO"))
                .andExpect(jsonPath("$.data.youtubeVideoId").value("dQw4w9WgXcQ"))
                .andExpect(jsonPath("$.data.youtubeEmbedUrl").value("https://www.youtube.com/embed/dQw4w9WgXcQ"))
                .andExpect(jsonPath("$.data.youtubeWatchUrl").value("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
                .andExpect(jsonPath("$.data.videoDurationSeconds").value(300))
                .andExpect(jsonPath("$.data.videoDurationFormatted").value("5 min"))
                .andExpect(jsonPath("$.data.thumbnailUrl").value("https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @WithMockUser(username = "content.admin@taxoryn.platform", roles = {"TAXORYN_CONTENT_ADMIN"})
    @DisplayName("Creating VIDEO with invalid YouTube URL fails validation")
    void testCreateVideoWithInvalidYouTubeUrlFails() throws Exception {
        CreateContentRequest request = CreateContentRequest.builder()
                .contentType(ContentType.VIDEO)
                .title("Invalid Video Test")
                .body("Some body text")
                .youtubeUrl("https://vimeo.com/987654321")
                .build();

        mockMvc.perform(post("/api/v1/admin/content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("valid YouTube video link")));
    }

    @Test
    @WithMockUser(username = "content.admin@taxoryn.platform", roles = {"TAXORYN_CONTENT_ADMIN"})
    @DisplayName("Admin can preview unpublished draft video")
    void testAdminCanPreviewDraftVideo() throws Exception {
        ContentEntity draftVideo = contentRepository.save(ContentEntity.builder()
                .contentType(ContentType.VIDEO)
                .title("Draft Video Guide")
                .slug("draft-video-guide")
                .body("Draft transcript")
                .youtubeVideoId("abc123xyz89")
                .videoDurationSeconds(120)
                .status(ContentStatus.DRAFT)
                .authorId(contentAdminUser.getId())
                .build());

        mockMvc.perform(get("/api/v1/admin/content/" + draftVideo.getId() + "/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Draft Video Guide"))
                .andExpect(jsonPath("$.data.youtubeVideoId").value("abc123xyz89"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @DisplayName("Customer cannot access unpublished draft video by slug")
    void testCustomerCannotAccessDraftVideo() throws Exception {
        contentRepository.save(ContentEntity.builder()
                .contentType(ContentType.VIDEO)
                .title("Secret Draft Video")
                .slug("secret-draft-video")
                .body("Unpublished video body")
                .youtubeVideoId("abc123xyz89")
                .status(ContentStatus.DRAFT)
                .authorId(contentAdminUser.getId())
                .build());

        mockMvc.perform(get("/api/v1/public/content/secret-draft-video"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "content.admin@taxoryn.platform", roles = {"TAXORYN_CONTENT_ADMIN"})
    @DisplayName("Admin lifecycle: Submit review -> Approve -> Publish -> Customer view")
    void testVideoPublishingAndCustomerViewing() throws Exception {
        ContentEntity video = contentRepository.save(ContentEntity.builder()
                .contentType(ContentType.VIDEO)
                .title("ITR Filing Masterclass")
                .slug("itr-filing-masterclass")
                .summary("Complete ITR filing tutorial.")
                .body("Detailed walkthrough of AY 2026-27 ITR forms.")
                .youtubeVideoId("dQw4w9WgXcQ")
                .videoDurationSeconds(600)
                .status(ContentStatus.DRAFT)
                .categoryId(testCategory.getId())
                .authorId(contentAdminUser.getId())
                .build());

        // 1. Submit for review
        mockMvc.perform(post("/api/v1/admin/content/" + video.getId() + "/submit-review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(is(oneOf("SUBMITTED", "UNDER_REVIEW"))));

        // 2. Approve
        mockMvc.perform(post("/api/v1/admin/content/" + video.getId() + "/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // 3. Publish
        mockMvc.perform(post("/api/v1/admin/content/" + video.getId() + "/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        // 4. Customer accesses published video without authentication
        mockMvc.perform(get("/api/v1/public/content/itr-filing-masterclass"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("ITR Filing Masterclass"))
                .andExpect(jsonPath("$.data.contentType").value("VIDEO"))
                .andExpect(jsonPath("$.data.youtubeVideoId").value("dQw4w9WgXcQ"))
                .andExpect(jsonPath("$.data.youtubeEmbedUrl").value("https://www.youtube.com/embed/dQw4w9WgXcQ"))
                .andExpect(jsonPath("$.data.videoDurationFormatted").value("10 min"))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        // 5. Customer filters by contentType=VIDEO
        mockMvc.perform(get("/api/v1/public/content")
                        .param("contentType", "VIDEO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].slug").value("itr-filing-masterclass"));
    }
}
