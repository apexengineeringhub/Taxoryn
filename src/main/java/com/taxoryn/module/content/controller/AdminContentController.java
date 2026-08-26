package com.taxoryn.module.content.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.content.dto.*;
import com.taxoryn.module.content.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/admin/content", "/api/admin/content"})
@RequiredArgsConstructor
@Tag(name = "Taxoryn Learn Content Management", description = "Authoritative APIs for Platform Knowledge Base, Articles, Guides, Videos, and Tax Updates")
public class AdminContentController {

    private final ContentService contentService;

    @PostMapping
    @PreAuthorize("hasRole('TAXORYN_CONTENT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('CONTENT_CREATE') or hasAuthority('ARTICLE_CREATE') or hasAuthority('VIDEO_CREATE')")
    @Operation(summary = "Create Platform Content", description = "Drafts a new knowledge base article, video, guide, FAQ, or tax update in DRAFT status.")
    public ResponseEntity<ApiResponse<ContentResponse>> createContent(@Valid @RequestBody CreateContentRequest request) {
        ContentResponse response = contentService.createContent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Content created successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TAXORYN_CONTENT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_OPERATIONS_ADMIN') or hasAuthority('CONTENT_VIEW')")
    @Operation(summary = "Get Content by ID", description = "Retrieves a single content record with author, category, tax service, and tags.")
    public ResponseEntity<ApiResponse<ContentResponse>> getContentById(@PathVariable UUID id) {
        ContentResponse response = contentService.getContentById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/slug/{slug}")
    @PreAuthorize("hasRole('TAXORYN_CONTENT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_OPERATIONS_ADMIN') or hasAuthority('CONTENT_VIEW')")
    @Operation(summary = "Get Content by Slug", description = "Retrieves a content record by unique slug.")
    public ResponseEntity<ApiResponse<ContentResponse>> getContentBySlug(@PathVariable String slug) {
        ContentResponse response = contentService.getContentBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TAXORYN_CONTENT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('CONTENT_EDIT') or hasAuthority('ARTICLE_UPDATE') or hasAuthority('VIDEO_UPDATE')")
    @Operation(summary = "Update Content", description = "Updates fields and metadata of an existing content record.")
    public ResponseEntity<ApiResponse<ContentResponse>> updateContent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateContentRequest request) {
        ContentResponse response = contentService.updateContent(id, request);
        return ResponseEntity.ok(ApiResponse.success("Content updated successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('TAXORYN_CONTENT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_OPERATIONS_ADMIN') or hasAuthority('CONTENT_VIEW')")
    @Operation(summary = "List Platform Content", description = "Lists paginated platform content with filtering by contentType, status, category, taxService, and tags.")
    public ResponseEntity<ApiResponse<PagedResponse<ContentSummaryResponse>>> listContent(@Valid @ModelAttribute ContentFilterRequest filterRequest) {
        PagedResponse<ContentSummaryResponse> response = contentService.listContent(filterRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/submit-review")
    @PreAuthorize("hasRole('TAXORYN_CONTENT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('CONTENT_SUBMIT_REVIEW') or hasAuthority('CONTENT_MANAGE')")
    @Operation(summary = "Submit for Review", description = "Transitions drafted content into UNDER_REVIEW status.")
    public ResponseEntity<ApiResponse<ContentResponse>> submitForReview(@PathVariable UUID id) {
        ContentResponse response = contentService.submitForReview(id);
        return ResponseEntity.ok(ApiResponse.success("Content submitted for review", response));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('TAXORYN_CONTENT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('CONTENT_APPROVE') or hasAuthority('CONTENT_MANAGE')")
    @Operation(summary = "Approve Content", description = "Transitions reviewed content into APPROVED status and records reviewer ID.")
    public ResponseEntity<ApiResponse<ContentResponse>> approveContent(@PathVariable UUID id) {
        ContentResponse response = contentService.approveContent(id);
        return ResponseEntity.ok(ApiResponse.success("Content approved successfully", response));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('TAXORYN_CONTENT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('CONTENT_PUBLISH') or hasAuthority('ARTICLE_PUBLISH') or hasAuthority('VIDEO_PUBLISH')")
    @Operation(summary = "Publish Content", description = "Transitions approved content into PUBLISHED status and timestamps publishedAt.")
    public ResponseEntity<ApiResponse<ContentResponse>> publishContent(@PathVariable UUID id) {
        ContentResponse response = contentService.publishContent(id);
        return ResponseEntity.ok(ApiResponse.success("Content published successfully", response));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('TAXORYN_CONTENT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('CONTENT_ARCHIVE') or hasAuthority('ARTICLE_ARCHIVE') or hasAuthority('VIDEO_ARCHIVE')")
    @Operation(summary = "Archive Content", description = "Transitions active or drafted content into ARCHIVED status.")
    public ResponseEntity<ApiResponse<ContentResponse>> archiveContent(@PathVariable UUID id) {
        ContentResponse response = contentService.archiveContent(id);
        return ResponseEntity.ok(ApiResponse.success("Content archived successfully", response));
    }

    @GetMapping("/{id}/preview")
    @PreAuthorize("hasRole('TAXORYN_CONTENT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_OPERATIONS_ADMIN') or hasAuthority('CONTENT_VIEW')")
    @Operation(summary = "Admin Preview Content", description = "Allows administrators to preview drafts and unpublished content.")
    public ResponseEntity<ApiResponse<ContentResponse>> previewContent(@PathVariable UUID id) {
        ContentResponse response = contentService.previewContent(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
