package com.taxoryn.module.content.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.content.dto.*;
import com.taxoryn.module.content.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/public/content", "/api/public/content"})
@RequiredArgsConstructor
@Tag(name = "Taxoryn Learn Public Knowledge API", description = "Public, customer-facing endpoints for published articles, videos, guides, FAQs, and tax updates.")
public class PublicContentController {

    private final ContentService contentService;

    @GetMapping
    @Operation(summary = "Browse Published Tax Topics", description = "Lists published articles, videos, guides, and tax updates with category and keyword search.")
    public ResponseEntity<ApiResponse<PagedResponse<ContentSummaryResponse>>> listPublicContent(@Valid @ModelAttribute ContentFilterRequest filterRequest) {
        PagedResponse<ContentSummaryResponse> response = contentService.listPublicContent(filterRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get Published Content by Slug", description = "Retrieves full published article, guide, video, or FAQ by unique slug. Returns 404 if draft or archived.")
    public ResponseEntity<ApiResponse<ContentResponse>> getPublicContentBySlug(@PathVariable String slug) {
        ContentResponse response = contentService.getPublicContentBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{slug}/related")
    @Operation(summary = "Get Related Published Content", description = "Retrieves related published tax topics based on category or tax service.")
    public ResponseEntity<ApiResponse<List<ContentSummaryResponse>>> getRelatedPublicContent(
            @PathVariable String slug,
            @RequestParam(defaultValue = "4") int limit) {
        List<ContentSummaryResponse> response = contentService.getRelatedPublicContent(slug, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get Public Tax Learning Categories", description = "Retrieves tax categories with published content counts for visual category cards.")
    public ResponseEntity<ApiResponse<List<PublicContentCategoryDto>>> getPublicCategories() {
        List<PublicContentCategoryDto> response = contentService.getPublicCategories();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
