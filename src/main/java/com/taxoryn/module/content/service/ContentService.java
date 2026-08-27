package com.taxoryn.module.content.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.content.dto.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ContentService {

    ContentResponse createContent(CreateContentRequest request);

    ContentResponse getContentById(UUID id);

    ContentResponse getContentBySlug(String slug);

    ContentResponse updateContent(UUID id, UpdateContentRequest request);

    PagedResponse<ContentSummaryResponse> listContent(ContentFilterRequest filterRequest);

    ContentResponse submitForReview(UUID id);

    ContentResponse startReview(UUID id);

    ContentResponse approveContent(UUID id);

    ContentResponse rejectContent(UUID id, String reason);

    ContentResponse scheduleContent(UUID id, Instant scheduledPublishAt);

    int publishScheduledContent();

    ContentResponse publishContent(UUID id);

    ContentResponse archiveContent(UUID id);

    ContentResponse restoreContent(UUID id);

    ContentResponse previewContent(UUID id);

    ContentDashboardStatsDto getDashboardStats();

    PagedResponse<ContentSummaryResponse> getReviewQueue(int page, int size);

    List<ContentVersionDto> getVersionHistory(UUID contentId);

    // =========================================================================
    // Public / Customer Experience APIs (Strictly PUBLISHED content only)
    // =========================================================================

    PagedResponse<ContentSummaryResponse> listPublicContent(ContentFilterRequest filterRequest);

    ContentResponse getPublicContentBySlug(String slug);

    List<ContentSummaryResponse> getRelatedPublicContent(String slug, int limit);

    List<PublicContentCategoryDto> getPublicCategories();

    // =========================================================================
    // SEO, robots.txt, & XML Sitemap
    // =========================================================================

    List<PublicSitemapItemDto> getPublicSitemapItems();

    String generateSitemapXml();

    String getRobotsTxtContent();
}
