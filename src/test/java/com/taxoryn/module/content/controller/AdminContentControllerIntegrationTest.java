package com.taxoryn.module.content.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.content.dto.CreateContentRequest;
import com.taxoryn.module.content.dto.UpdateContentRequest;
import com.taxoryn.module.content.entity.ContentEntity;
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
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminContentControllerIntegrationTest {

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

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity platformOrg;
    private UserEntity contentAdminUser;
    private UserEntity regularUser;
    private TaxServiceCategoryEntity gstCategory;
    private TaxServiceEntity gstFilingService;

    private String contentAdminToken;
    private String regularUserToken;

    @BeforeEach
    void setUp() {
        contentRepository.deleteAll();
        tagRepository.deleteAll();
        taxServiceRepository.deleteAll();
        categoryRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        // 1. Create Organization
        platformOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Taxoryn Platform Operations")
                .legalName("Taxoryn Global Inc")
                .email("admin@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 2. Roles
        RoleEntity contentAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("TAXORYN_CONTENT_ADMIN").orElseGet(() ->
                roleRepository.save(RoleEntity.builder()
                        .code("TAXORYN_CONTENT_ADMIN")
                        .name("Taxoryn Content Admin")
                        .isSystemRole(true)
                        .build()));

        RoleEntity clientRole = roleRepository.findByCodeAndIsSystemRoleTrue("CLIENT_USER").orElseGet(() ->
                roleRepository.save(RoleEntity.builder()
                        .code("CLIENT_USER")
                        .name("Client User")
                        .isSystemRole(true)
                        .build()));

        // 3. Users
        contentAdminUser = userRepository.save(UserEntity.builder()
                .organizationId(platformOrg.getId())
                .email("content.editor@taxoryn.com")
                .firstName("Aarav")
                .lastName("Sharma")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(contentAdminRole)))
                .build());

        regularUser = userRepository.save(UserEntity.builder()
                .organizationId(platformOrg.getId())
                .email("taxpayer@gmail.com")
                .firstName("Suresh")
                .lastName("Kumar")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(clientRole)))
                .build());

        // 4. Tax Service Master Seed
        gstCategory = categoryRepository.save(TaxServiceCategoryEntity.builder()
                .code("GST")
                .name("Goods and Services Tax")
                .description("All GST registration and return filing compliance")
                .sortOrder(1)
                .isActive(true)
                .build());

        gstFilingService = taxServiceRepository.save(TaxServiceEntity.builder()
                .categoryId(gstCategory.getId())
                .code("GST_RETURN_FILING")
                .name("GST Return Filing")
                .description("Monthly & Quarterly GSTR-1, GSTR-3B compliance")
                .sortOrder(1)
                .isActive(true)
                .build());

        // 5. Auth Tokens
        contentAdminToken = jwtTokenProvider.generateAccessToken(
                contentAdminUser.getId(), platformOrg.getId(), null, contentAdminUser.getEmail(),
                Set.of("TAXORYN_CONTENT_ADMIN"),
                Set.of("PLATFORM_VIEW", "CONTENT_VIEW", "CONTENT_CREATE", "CONTENT_EDIT", "CONTENT_SUBMIT_REVIEW", "CONTENT_APPROVE", "CONTENT_PUBLISH", "CONTENT_ARCHIVE")
        );

        regularUserToken = jwtTokenProvider.generateAccessToken(
                regularUser.getId(), platformOrg.getId(), null, regularUser.getEmail(),
                Set.of("CLIENT_USER"),
                Set.of("PORTAL_VIEW")
        );
    }

    @AfterEach
    void tearDown() {
        contentRepository.deleteAll();
        tagRepository.deleteAll();
        taxServiceRepository.deleteAll();
        categoryRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        TenantContext.clear();
    }

    // 1-5. Test Creating All 5 Controlled Content Types
    @Test
    @DisplayName("1-5. Successfully create ARTICLE, VIDEO, GUIDE, FAQ, and TAX_UPDATE content records")
    void testCreateAllContentTypes() throws Exception {
        // 1. Article
        CreateContentRequest articleReq = CreateContentRequest.builder()
                .contentType(ContentType.ARTICLE)
                .title("Complete Guide to GST Invoicing Rules")
                .slug("complete-guide-gst-invoicing-rules")
                .summary("Understanding mandatory B2B e-invoicing thresholds in India")
                .body("Section 31 of the CGST Act mandates tax invoices with IRN and QR code...")
                .categoryId(gstCategory.getId())
                .taxServiceId(gstFilingService.getId())
                .tags(Set.of("GST", "Invoicing", "Compliance"))
                .build();

        mockMvc.perform(post("/api/v1/admin/content")
                        .header("Authorization", "Bearer " + contentAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(articleReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.contentType").value("ARTICLE"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.slug").value("complete-guide-gst-invoicing-rules"))
                .andExpect(jsonPath("$.data.categoryName").value("Goods and Services Tax"))
                .andExpect(jsonPath("$.data.taxServiceName").value("GST Return Filing"))
                .andExpect(jsonPath("$.data.publicReady").value(false));

        // 2. Video
        CreateContentRequest videoReq = CreateContentRequest.builder()
                .contentType(ContentType.VIDEO)
                .title("How to Reconcile GSTR-2B with Purchase Register")
                .slug("how-to-reconcile-gstr-2b")
                .body("Video walkthrough of ITC matching algorithms.")
                .youtubeUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
                .thumbnailUrl("https://assets.taxoryn.com/thumbnails/gstr2b-guide.jpg")
                .tags(Set.of("GSTR-2B", "ITC"))
                .build();

        mockMvc.perform(post("/api/v1/admin/content")
                        .header("Authorization", "Bearer " + contentAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(videoReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.contentType").value("VIDEO"));

        // 3. Guide
        CreateContentRequest guideReq = CreateContentRequest.builder()
                .contentType(ContentType.GUIDE)
                .title("Annual GST Audit & GSTR-9 Step-by-Step Guide")
                .body("Detailed checklist for preparing annual return reconciliations.")
                .build();

        mockMvc.perform(post("/api/v1/admin/content")
                        .header("Authorization", "Bearer " + contentAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guideReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.contentType").value("GUIDE"));

        // 4. FAQ
        CreateContentRequest faqReq = CreateContentRequest.builder()
                .contentType(ContentType.FAQ)
                .title("What happens if GSTR-3B is filed after the 20th?")
                .body("Late fees under Section 47 apply at Rs. 50 per day along with 18% per annum interest under Section 50.")
                .build();

        mockMvc.perform(post("/api/v1/admin/content")
                        .header("Authorization", "Bearer " + contentAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(faqReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.contentType").value("FAQ"));

        // 5. Tax Update
        CreateContentRequest updateReq = CreateContentRequest.builder()
                .contentType(ContentType.TAX_UPDATE)
                .title("CBIC Notification 12/2026: Extension of Due Date for GSTR-1")
                .body("The central tax board has extended the due date for GSTR-1 filing to the 13th.")
                .build();

        mockMvc.perform(post("/api/v1/admin/content")
                        .header("Authorization", "Bearer " + contentAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.contentType").value("TAX_UPDATE"));
    }

    // 6. Duplicate slug rejected
    @Test
    @DisplayName("6. Duplicate slug is rejected with 409 Conflict")
    void testDuplicateSlugRejected() throws Exception {
        CreateContentRequest req1 = CreateContentRequest.builder()
                .contentType(ContentType.ARTICLE)
                .title("Tax Deductions under Section 80C")
                .slug("section-80c-deductions")
                .body("Comprehensive review of PF, PPF, ELSS, and insurance premiums.")
                .build();

        mockMvc.perform(post("/api/v1/admin/content")
                        .header("Authorization", "Bearer " + contentAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        CreateContentRequest req2 = CreateContentRequest.builder()
                .contentType(ContentType.ARTICLE)
                .title("Another 80C Overview Article")
                .slug("section-80c-deductions") // duplicate slug
                .body("Alternative discussion of 80C limits.")
                .build();

        mockMvc.perform(post("/api/v1/admin/content")
                        .header("Authorization", "Bearer " + contentAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict());
    }

    // 7. Invalid content type rejected
    @Test
    @DisplayName("7. Invalid content type is rejected with 400 Bad Request")
    void testInvalidContentTypeRejected() throws Exception {
        String invalidPayload = """
                {
                    "contentType": "UNKNOWN_CUSTOM_TYPE",
                    "title": "Invalid Type Test",
                    "body": "Body content"
                }
                """;

        mockMvc.perform(post("/api/v1/admin/content")
                        .header("Authorization", "Bearer " + contentAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    // 8-11. Lifecycle Transitions (Draft -> Under Review -> Approved -> Published -> Archived)
    @Test
    @DisplayName("8-11. Full content lifecycle transitions: Draft -> Under Review -> Approved -> Published -> Archived")
    void testContentLifecycleProgression() throws Exception {
        // Step 1: Create Draft
        CreateContentRequest req = CreateContentRequest.builder()
                .contentType(ContentType.GUIDE)
                .title("Capital Gains Tax on Real Estate in India")
                .slug("capital-gains-tax-real-estate")
                .body("Guide explaining LTCG vs STCG, indexation rules, and Section 54 exemptions.")
                .build();

        String createRes = mockMvc.perform(post("/api/v1/admin/content")
                        .header("Authorization", "Bearer " + contentAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        UUID contentId = UUID.fromString(objectMapper.readTree(createRes).get("data").get("id").asText());

        // 8. Submit for Review
        mockMvc.perform(post("/api/v1/admin/content/" + contentId + "/submit-review")
                        .header("Authorization", "Bearer " + contentAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(is(oneOf("SUBMITTED", "UNDER_REVIEW"))))
                .andExpect(jsonPath("$.data.publicReady").value(false));

        // 9. Approve
        mockMvc.perform(post("/api/v1/admin/content/" + contentId + "/approve")
                        .header("Authorization", "Bearer " + contentAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.reviewerId").value(contentAdminUser.getId().toString()))
                .andExpect(jsonPath("$.data.publicReady").value(false));

        // 10. Publish
        mockMvc.perform(post("/api/v1/admin/content/" + contentId + "/publish")
                        .header("Authorization", "Bearer " + contentAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.publishedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.publicReady").value(true));

        // 11. Archive
        mockMvc.perform(post("/api/v1/admin/content/" + contentId + "/archive")
                        .header("Authorization", "Bearer " + contentAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.data.publicReady").value(false));
    }

    // 12. Invalid status transition rejected
    @Test
    @DisplayName("12. Invalid status transition (e.g. Draft directly to Published) is rejected with 400 Bad Request")
    void testInvalidStatusTransitionRejected() throws Exception {
        CreateContentRequest req = CreateContentRequest.builder()
                .contentType(ContentType.ARTICLE)
                .title("New Tax Regime vs Old Tax Regime Calculator")
                .body("Comparison of slab rates under Section 115BAC.")
                .build();

        String createRes = mockMvc.perform(post("/api/v1/admin/content")
                        .header("Authorization", "Bearer " + contentAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID contentId = UUID.fromString(objectMapper.readTree(createRes).get("data").get("id").asText());

        // Attempting to publish directly from DRAFT without approval
        mockMvc.perform(post("/api/v1/admin/content/" + contentId + "/publish")
                        .header("Authorization", "Bearer " + contentAdminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsStringIgnoringCase("DRAFT to PUBLISHED")));
    }

    // 13. Unauthorized user cannot publish
    @Test
    @DisplayName("13. Unauthorized regular client user cannot publish or modify platform content (403 Forbidden)")
    void testUnauthorizedUserDeniedContentModification() throws Exception {
        CreateContentRequest req = CreateContentRequest.builder()
                .contentType(ContentType.ARTICLE)
                .title("Unauthorized Attempt by Client")
                .body("This should be rejected by Spring Security.")
                .build();

        mockMvc.perform(post("/api/v1/admin/content")
                        .header("Authorization", "Bearer " + regularUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // 14. Audit event created
    @Test
    @DisplayName("14. Content operations generate authoritative platform audit log records")
    void testContentAuditTrailGenerated() throws Exception {
        CreateContentRequest req = CreateContentRequest.builder()
                .contentType(ContentType.ARTICLE)
                .title("Audited Article on TDS Rates")
                .body("Section 194C contractor withholding rules.")
                .build();

        String createRes = mockMvc.perform(post("/api/v1/admin/content")
                        .header("Authorization", "Bearer " + contentAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID contentId = UUID.fromString(objectMapper.readTree(createRes).get("data").get("id").asText());

        mockMvc.perform(post("/api/v1/admin/content/" + contentId + "/submit-review")
                        .header("Authorization", "Bearer " + contentAdminToken))
                .andExpect(status().isOk());

        assertThat(auditLogRepository.findAll()).anyMatch(log ->
                "CONTENT_CREATED".equals(log.getAction()) && contentId.toString().equals(log.getEntityId()));
        assertThat(auditLogRepository.findAll()).anyMatch(log ->
                log.getAction() != null && log.getAction().startsWith("CONTENT_SUBMITTED") && contentId.toString().equals(log.getEntityId()));
    }

    // 15-20. Filtering, Pagination, Tax Service relationship, and Public-Ready validation
    @Test
    @DisplayName("15-20. Pagination, Tax Service association, status and type filtering")
    void testContentQueryFilteringAndPagination() throws Exception {
        // Seed 3 articles in GST category
        for (int i = 1; i <= 3; i++) {
            CreateContentRequest req = CreateContentRequest.builder()
                    .contentType(ContentType.ARTICLE)
                    .title("GST Guide Chapter " + i)
                    .slug("gst-guide-chapter-" + i)
                    .body("Chapter " + i + " content details.")
                    .categoryId(gstCategory.getId())
                    .taxServiceId(gstFilingService.getId())
                    .build();

            mockMvc.perform(post("/api/v1/admin/content")
                            .header("Authorization", "Bearer " + contentAdminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }

        // Seed 2 Video Guides
        for (int i = 1; i <= 2; i++) {
            CreateContentRequest req = CreateContentRequest.builder()
                    .contentType(ContentType.VIDEO)
                    .title("Income Tax Tutorial " + i)
                    .slug("it-tutorial-" + i)
                    .body("Video tutorial " + i + " content.")
                    .youtubeUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
                    .build();

            mockMvc.perform(post("/api/v1/admin/content")
                            .header("Authorization", "Bearer " + contentAdminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }

        // 16. Pagination
        mockMvc.perform(get("/api/v1/admin/content?page=0&size=2")
                        .header("Authorization", "Bearer " + contentAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(3));

        // 17. Filter by Content Type (VIDEO)
        mockMvc.perform(get("/api/v1/admin/content?contentType=VIDEO")
                        .header("Authorization", "Bearer " + contentAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].contentType").value("VIDEO"));

        // 18. Filter by Category (GST)
        mockMvc.perform(get("/api/v1/admin/content?categoryId=" + gstCategory.getId())
                        .header("Authorization", "Bearer " + contentAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(3)))
                .andExpect(jsonPath("$.data.content[0].categoryCode").value("GST"));

        // 19. Filter by Tax Service (GST_RETURN_FILING)
        mockMvc.perform(get("/api/v1/admin/content?taxServiceId=" + gstFilingService.getId())
                        .header("Authorization", "Bearer " + contentAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(3)))
                .andExpect(jsonPath("$.data.content[0].taxServiceName").value("GST Return Filing"));

        // 20. Update content details
        ContentEntity first = contentRepository.findBySlug("gst-guide-chapter-1").orElseThrow();
        UpdateContentRequest updateReq = UpdateContentRequest.builder()
                .title("GST Guide Chapter 1 (2026 Edition)")
                .summary("Updated overview of GST compliance")
                .build();

        mockMvc.perform(put("/api/v1/admin/content/" + first.getId())
                        .header("Authorization", "Bearer " + contentAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("GST Guide Chapter 1 (2026 Edition)"))
                .andExpect(jsonPath("$.data.summary").value("Updated overview of GST compliance"));
    }
}
