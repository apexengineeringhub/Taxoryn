package com.taxoryn.module.notice.controller;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.notice.dto.CloseNoticeRequest;
import com.taxoryn.module.notice.dto.CreateNoticeResponseRequest;
import com.taxoryn.module.notice.dto.CreateTaxNoticeRequest;
import com.taxoryn.module.notice.dto.NoticeActivityDto;
import com.taxoryn.module.notice.dto.NoticeDashboardStatsDto;
import com.taxoryn.module.notice.dto.NoticeHearingDto;
import com.taxoryn.module.notice.dto.NoticeResponseDto;
import com.taxoryn.module.notice.dto.RecordHearingOutcomeRequest;
import com.taxoryn.module.notice.dto.ReviewNoticeResponseRequest;
import com.taxoryn.module.notice.dto.ScheduleHearingRequest;
import com.taxoryn.module.notice.dto.SubmitNoticeRequest;
import com.taxoryn.module.notice.dto.TaxNoticeDto;
import com.taxoryn.module.notice.dto.TaxNoticeFilterRequest;
import com.taxoryn.module.notice.dto.UpdateTaxNoticeRequest;
import com.taxoryn.module.notice.service.TaxNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
@Tag(name = "Tax Notice Management", description = "Endpoints for managing tax notices, maker-checker responses, hearings, and resolutions")
@SecurityRequirement(name = "BearerAuth")
public class TaxNoticeController {

    private final TaxNoticeService noticeService;

    @GetMapping
    @PreAuthorize("hasAuthority('NOTICE_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF')")
    @Operation(summary = "List tax notices with filters and pagination")
    public ResponseEntity<ApiResponse<PagedResponse<TaxNoticeDto>>> getNotices(
            @Valid @ModelAttribute TaxNoticeFilterRequest filterRequest,
            @Valid @ModelAttribute PageRequestDto pageRequest) {
        PagedResponse<TaxNoticeDto> response = noticeService.getNotices(filterRequest, pageRequest);
        return ResponseEntity.ok(ApiResponse.success("Tax notices retrieved successfully", response));
    }

    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasAuthority('NOTICE_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF')")
    @Operation(summary = "Get notice dashboard KPI statistics and breakdown")
    public ResponseEntity<ApiResponse<NoticeDashboardStatsDto>> getDashboardStats() {
        NoticeDashboardStatsDto stats = noticeService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats retrieved successfully", stats));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('NOTICE_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF')")
    @Operation(summary = "Get notice case details by ID")
    public ResponseEntity<ApiResponse<TaxNoticeDto>> getNoticeById(@PathVariable UUID id) {
        TaxNoticeDto notice = noticeService.getNoticeById(id);
        return ResponseEntity.ok(ApiResponse.success("Tax notice retrieved successfully", notice));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('NOTICE_CREATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF')")
    @Operation(summary = "Log a new tax notice case")
    public ResponseEntity<ApiResponse<TaxNoticeDto>> createNotice(@Valid @RequestBody CreateTaxNoticeRequest request) {
        TaxNoticeDto created = noticeService.createNotice(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tax notice logged successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('NOTICE_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('TAX_PROFESSIONAL')")
    @Operation(summary = "Update tax notice details")
    public ResponseEntity<ApiResponse<TaxNoticeDto>> updateNotice(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaxNoticeRequest request) {
        TaxNoticeDto updated = noticeService.updateNotice(id, request);
        return ResponseEntity.ok(ApiResponse.success("Tax notice updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('NOTICE_DELETE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER')")
    @Operation(summary = "Delete or archive a tax notice case")
    public ResponseEntity<ApiResponse<Void>> deleteNotice(@PathVariable UUID id) {
        noticeService.deleteNotice(id);
        return ResponseEntity.ok(ApiResponse.success("Tax notice deleted successfully", null));
    }

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAuthority('NOTICE_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF')")
    @Operation(summary = "List all notices for a specific client")
    public ResponseEntity<ApiResponse<List<TaxNoticeDto>>> getNoticesByClient(@PathVariable UUID clientId) {
        List<TaxNoticeDto> notices = noticeService.getNoticesByClient(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client tax notices retrieved successfully", notices));
    }

    // ==========================================
    // Response Drafting & Review
    // ==========================================

    @GetMapping("/{id}/responses")
    @PreAuthorize("hasAuthority('NOTICE_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF')")
    @Operation(summary = "List response drafts for notice")
    public ResponseEntity<ApiResponse<List<NoticeResponseDto>>> getResponses(@PathVariable UUID id) {
        List<NoticeResponseDto> responses = noticeService.getResponses(id);
        return ResponseEntity.ok(ApiResponse.success("Notice responses retrieved successfully", responses));
    }

    @PostMapping("/{id}/responses")
    @PreAuthorize("hasAuthority('NOTICE_RESPONSE_CREATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF')")
    @Operation(summary = "Draft a new response version for notice")
    public ResponseEntity<ApiResponse<NoticeResponseDto>> createResponse(
            @PathVariable UUID id,
            @Valid @RequestBody CreateNoticeResponseRequest request) {
        NoticeResponseDto response = noticeService.createResponse(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Response draft created successfully", response));
    }

    @PostMapping("/{id}/responses/{responseId}/review")
    @PreAuthorize("hasAuthority('NOTICE_RESPONSE_REVIEW') or hasAuthority('NOTICE_APPROVE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('TAX_PROFESSIONAL')")
    @Operation(summary = "Review response draft (maker-checker review / partner sign-off)")
    public ResponseEntity<ApiResponse<NoticeResponseDto>> reviewResponse(
            @PathVariable UUID id,
            @PathVariable UUID responseId,
            @Valid @RequestBody ReviewNoticeResponseRequest request) {
        NoticeResponseDto response = noticeService.reviewResponse(id, responseId, request);
        return ResponseEntity.ok(ApiResponse.success("Response review recorded successfully", response));
    }

    // ==========================================
    // Hearings & Proceedings
    // ==========================================

    @GetMapping("/{id}/hearings")
    @PreAuthorize("hasAuthority('NOTICE_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF')")
    @Operation(summary = "List hearings and proceedings for notice")
    public ResponseEntity<ApiResponse<List<NoticeHearingDto>>> getHearings(@PathVariable UUID id) {
        List<NoticeHearingDto> hearings = noticeService.getHearings(id);
        return ResponseEntity.ok(ApiResponse.success("Hearings retrieved successfully", hearings));
    }

    @PostMapping("/{id}/hearings")
    @PreAuthorize("hasAuthority('NOTICE_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('TAX_PROFESSIONAL')")
    @Operation(summary = "Schedule a hearing for notice")
    public ResponseEntity<ApiResponse<NoticeHearingDto>> scheduleHearing(
            @PathVariable UUID id,
            @Valid @RequestBody ScheduleHearingRequest request) {
        NoticeHearingDto hearing = noticeService.scheduleHearing(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Hearing scheduled successfully", hearing));
    }

    @PutMapping("/{id}/hearings/{hearingId}/outcome")
    @PreAuthorize("hasAuthority('NOTICE_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('TAX_PROFESSIONAL')")
    @Operation(summary = "Record hearing proceedings and outcome")
    public ResponseEntity<ApiResponse<NoticeHearingDto>> recordHearingOutcome(
            @PathVariable UUID id,
            @PathVariable UUID hearingId,
            @Valid @RequestBody RecordHearingOutcomeRequest request) {
        NoticeHearingDto hearing = noticeService.recordHearingOutcome(id, hearingId, request);
        return ResponseEntity.ok(ApiResponse.success("Hearing outcome recorded successfully", hearing));
    }

    // ==========================================
    // Portal Filing & Closure
    // ==========================================

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('NOTICE_SUBMIT') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('TAX_PROFESSIONAL')")
    @Operation(summary = "Record portal submission and filing acknowledgement")
    public ResponseEntity<ApiResponse<TaxNoticeDto>> submitNotice(
            @PathVariable UUID id,
            @Valid @RequestBody SubmitNoticeRequest request) {
        TaxNoticeDto notice = noticeService.submitNotice(id, request);
        return ResponseEntity.ok(ApiResponse.success("Notice filing recorded successfully", notice));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('NOTICE_CLOSE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER')")
    @Operation(summary = "Resolve or close notice case")
    public ResponseEntity<ApiResponse<TaxNoticeDto>> closeNotice(
            @PathVariable UUID id,
            @Valid @RequestBody CloseNoticeRequest request) {
        TaxNoticeDto notice = noticeService.closeNotice(id, request);
        return ResponseEntity.ok(ApiResponse.success("Notice case closed successfully", notice));
    }

    // ==========================================
    // Activities & Notes
    // ==========================================

    @GetMapping("/{id}/activities")
    @PreAuthorize("hasAuthority('NOTICE_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF')")
    @Operation(summary = "Get complete activity timeline for notice")
    public ResponseEntity<ApiResponse<List<NoticeActivityDto>>> getActivities(@PathVariable UUID id) {
        List<NoticeActivityDto> activities = noticeService.getActivities(id);
        return ResponseEntity.ok(ApiResponse.success("Activities retrieved successfully", activities));
    }

    @PostMapping("/{id}/notes")
    @PreAuthorize("hasAuthority('NOTICE_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF')")
    @Operation(summary = "Add an internal note to notice case")
    public ResponseEntity<ApiResponse<Void>> addNote(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String note = body.get("note");
        noticeService.addInternalNote(id, note);
        return ResponseEntity.ok(ApiResponse.success("Internal note added successfully", null));
    }
}
