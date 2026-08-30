package com.taxoryn.core.bootstrap;

import com.taxoryn.module.content.entity.ContentEntity;
import com.taxoryn.module.content.entity.ContentStatus;
import com.taxoryn.module.content.entity.ContentType;
import com.taxoryn.module.content.repository.ContentRepository;
import com.taxoryn.module.content.repository.ContentTagRepository;
import com.taxoryn.module.marketplace.entity.TaxServiceCategoryEntity;
import com.taxoryn.module.marketplace.entity.TaxServiceEntity;
import com.taxoryn.module.marketplace.repository.TaxServiceCategoryRepository;
import com.taxoryn.module.marketplace.repository.TaxServiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(LearnContentDemoDataSeeder.class)
class LearnContentDemoDataSeederIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private ContentTagRepository tagRepository;

    @Autowired
    private TaxServiceCategoryRepository categoryRepository;

    @Autowired
    private TaxServiceRepository taxServiceRepository;

    @Autowired
    private LearnContentDemoDataSeeder learnContentDemoDataSeeder;

    @Test
    @DisplayName("LearnContentDemoDataSeeder seeds dummy content across all 5 types linked to Tax Services")
    void testSeederPopulatesAllContentTypesAndTaxServices() throws Exception {
        // Run seeder explicitly in test
        learnContentDemoDataSeeder.run();

        List<ContentEntity> publishedContents = contentRepository.findAll().stream()
                .filter(c -> c.getStatus() == ContentStatus.PUBLISHED)
                .toList();

        assertThat(publishedContents).isNotEmpty();

        // 1. Verify all 5 public content types are present
        assertThat(publishedContents).anyMatch(c -> c.getContentType() == ContentType.ARTICLE);
        assertThat(publishedContents).anyMatch(c -> c.getContentType() == ContentType.GUIDE);
        assertThat(publishedContents).anyMatch(c -> c.getContentType() == ContentType.FAQ);
        assertThat(publishedContents).anyMatch(c -> c.getContentType() == ContentType.TAX_UPDATE);
        assertThat(publishedContents).anyMatch(c -> c.getContentType() == ContentType.VIDEO);

        // 2. Verify public browse API returns seeded content
        mockMvc.perform(get("/api/v1/public/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", not(empty())))
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(8)));

        // 3. Verify public detail API for ITR-1 article returns active Tax Services & CTA enabled
        mockMvc.perform(get("/api/v1/public/content/complete-guide-itr-1-sahaj-ay-2026-27"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title", containsString("ITR-1 (Sahaj)")))
                .andExpect(jsonPath("$.data.contentType").value("ARTICLE"))
                .andExpect(jsonPath("$.data.marketplaceCtaEnabled").value(true))
                .andExpect(jsonPath("$.data.taxServices", not(empty())))
                .andExpect(jsonPath("$.data.tags", not(empty())));

        // 4. Verify public detail API for Video guide returns youtube video id & duration
        mockMvc.perform(get("/api/v1/public/content/demystifying-tds-salary-form-26as-ais-reconciliation-video"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contentType").value("VIDEO"))
                .andExpect(jsonPath("$.data.youtubeVideoId").value("dQw4w9WgXcQ"))
                .andExpect(jsonPath("$.data.videoDurationSeconds").value(780))
                .andExpect(jsonPath("$.data.marketplaceCtaEnabled").value(true));

        // 5. Verify tags are created
        assertThat(tagRepository.count()).isGreaterThanOrEqualTo(5);
    }
}
