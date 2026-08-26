package com.taxoryn.module.content.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.content.dto.*;

import java.util.List;
import java.util.UUID;

public interface ContentService {

    ContentResponse createContent(CreateContentRequest request);

    ContentResponse getContentById(UUID id);

    ContentResponse getContentBySlug(String slug);

    ContentResponse updateContent(UUID id, UpdateContentRequest request);

    PagedResponse<ContentSummaryResponse> listContent(ContentFilterRequest filterRequest);

    ContentResponse submitForReview(UUID id);

    ContentResponse approveContent(UUID id);

    ContentResponse publishContent(UUID id);

    ContentResponse archiveContent(UUID id);

    ContentResponse previewContent(UUID id);

    // =========================================================================
    // Public / Customer Experience APIs (Strictly PUBLISHED content only)
    // =========================================================================

    PagedResponse<ContentSummaryResponse> listPublicContent(ContentFilterRequest filterRequest);

    ContentResponse getPublicContentBySlug(String slug);

    List<ContentSummaryResponse> getRelatedPublicContent(String slug, int limit);

    List<PublicContentCategoryDto> getPublicCategories();
}
