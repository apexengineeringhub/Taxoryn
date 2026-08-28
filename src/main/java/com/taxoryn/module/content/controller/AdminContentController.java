package com.taxoryn.module.content.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.content.dto.*;
import com.taxoryn.module.content.service.ContentService;
import com.taxoryn.module.marketplace.dto.PublicTaxServiceDto;
import com.taxoryn.module.marketplace.service.TaxServiceMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/admin/content", "/api/admin/content"})
@RequiredArgsConstructor
@Tag(name = "Taxoryn Content & Marketing Studio", description = "Authoritative APIs for Platform Knowledge Base, Studio Dashboard, Review Queue, Scheduling, and Media")
public class AdminContentController {

    private final ContentService contentService;
    private final TaxServiceMasterService taxServiceMasterService;

    @GetMapping("/dashboard-stats")
    @PreAuthorize("hasRole('TAXORYN_CONTENT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_OPERATIONS_ADMIN') or hasAuthority('CONTENT_VIEW')")
    @Operation(summary = "Content Studio Dashboard Stats", description = "Operational metrics, needs attention queue, and recent studio activity.")
    public ResponseEntity<ApiResponse<ContentDashboardStatsDto>> getDashboardStats() {
        ContentDashboardStatsDto stats = contentService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/review-queue")
    @PreAuthorize("hasRole('TAXORYN_CONTENT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_OPERATIONS_ADMIN') or hasAuthority('CONTENT_REVIEW') or hasAuthority('CONTENT_VIEW')")
    @Operation(summary = "Content Studio Review Queue", description = "List of items pending peer/compliance review.")
    public ResponseEntity<ApiResponse<PagedResponse<ContentSummaryResponse>>> getReviewQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PagedResponse<ContentSummaryResponse> queue = contentService.getReviewQueue(page, size);
        return ResponseEntity.ok(ApiResponse.success(queue));
    }

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
    @Operation(summary = "Submit for Review", description = "Transitions drafted content into SUBMITTED status.")
    public ResponseEntity<ApiResponse<ContentResponse>> submitForReview(@PathVariable UUID id) {
        ContentResponse response = contentService.submitForReview(id);
        return ResponseEntity.ok(ApiResponse.success("Content submitted for review", response));
    }

    @PostMapping("/{id}/start-review")
    @PreAuthorize("hasRole('TAXORYN_CONTENT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('CONTENT_REVIEW') or hasAuthority('CONTENT_MANAGE')")
    @Operation(summary = "Start Review", description = "Transitions submitted content into IN_REVIEW status.")
    public ResponseEntity<ApiResponse<ContentResponse>> startReview(@PathVariable UUID id) {
        ContentResponse response = contentService.startReview(id);
        return ResponseEntity.ok(ApiResponse.success("Review started", response));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('TAXORYN_CONTENT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('CONTENT_APPROVE') or hasAuthority('CONTENT_MANAGE')")
    @Operation(summary = "Approve Content", description = "Transitions reviewed content into APPROVED status and records reviewer ID.")
    public ResponseEntity<ApiResponse<ContentResponse>> approveContent(@PathVariable UUID id) {
        ContentResponse response = contentService.approveContent(id);
        return ResponseEntity.ok(ApiResponse.success("Content approved successfully", response));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('TAXORYN_CONTENT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('CONTENT_REJECT') or hasAuthority('CONTENT_APPROVE') or hasAuthority('CONTENT_MANAGE')")
    @Operation(summary = "Reject Content with Reason", description = "Rejects submitted/in-review content and records the rejection reason.")
    public ResponseEntity<ApiResponse<ContentResponse>> rejectContent(
            @PathVariable UUID id,
            @Valid @RequestBody RejectContentRequest request
    ) {
        ContentResponse response = contentService.rejectContent(id, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Content rejected with feedback", response));
    }

    @PostMapping("/{id}/schedule")
    @PreAuthorize("hasRole('TAXORYN_CONTENT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('CONTENT_SCHEDULE') or hasAuthority('CONTENT_PUBLISH') or hasAuthority('CONTENT_MANAGE')")
    @Operation(summary = "Schedule Publication", description = "Schedules approved content for automated future publication.")
    public ResponseEntity<ApiResponse<ContentResponse>> scheduleContent(
            @PathVariable UUID id,
            @Valid @RequestBody ScheduleContentRequest request
    ) {
        ContentResponse response = contentService.scheduleContent(id, request.getScheduledPublishAt());
        return ResponseEntity.ok(ApiResponse.success("Content scheduled for publication", response));
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

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('TAXORYN_CONTENT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('CONTENT_RESTORE') or hasAuthority('CONTENT_ARCHIVE')")
    @Operation(summary = "Restore Archived Content", description = "Restores archived content back to DRAFT for revision.")
    public ResponseEntity<ApiResponse<ContentResponse>> restoreContent(@PathVariable UUID id) {
        ContentResponse response = contentService.restoreContent(id);
        return ResponseEntity.ok(ApiResponse.success("Content restored to DRAFT", response));
    }

    @GetMapping("/{id}/preview")
    @PreAuthorize("hasRole('TAXORYN_CONTENT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_OPERATIONS_ADMIN') or hasAuthority('CONTENT_VIEW')")
    @Operation(summary = "Admin Preview Content", description = "Allows administrators to preview drafts and unpublished content.")
    public ResponseEntity<ApiResponse<ContentResponse>> previewContent(@PathVariable UUID id) {
        ContentResponse response = contentService.previewContent(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("hasRole('TAXORYN_CONTENT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('CONTENT_VIEW')")
    @Operation(summary = "Get Content Version History", description = "Retrieves revision and snapshot history of a content item.")
    public ResponseEntity<ApiResponse<List<ContentVersionDto>>> getVersionHistory(@PathVariable UUID id) {
        List<ContentVersionDto> versions = contentService.getVersionHistory(id);
        return ResponseEntity.ok(ApiResponse.success(versions));
    }

    @GetMapping("/tax-services")
    @PreAuthorize("hasRole('TAXORYN_CONTENT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_OPERATIONS_ADMIN') or hasAuthority('CONTENT_VIEW')")
    @Operation(summary = "Read-Only Controlled Tax Service Master Reference", description = "Provides content authors with the active controlled taxonomy master list.")
    public ResponseEntity<ApiResponse<List<PublicTaxServiceDto>>> getControlledTaxServices() {
        List<PublicTaxServiceDto> services = taxServiceMasterService.getPublicActiveServices();
        return ResponseEntity.ok(ApiResponse.success(services));
    }
}
