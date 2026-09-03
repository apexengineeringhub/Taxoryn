package com.taxoryn.module.feedback.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.feedback.dto.*;
import com.taxoryn.module.feedback.entity.*;
import com.taxoryn.module.feedback.service.AdminApplicationFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/admin/feedback", "/api/admin/feedback"})
@RequiredArgsConstructor
@Tag(name = "Platform Admin Feedback Management", description = "Taxoryn Operations & Super Admin lifecycle management for product feedback")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('TAXORYN_OPERATIONS_ADMIN') or hasRole('TAXORYN_SUPPORT_ADMIN') or hasAuthority('FEEDBACK_MANAGE')")
public class AdminApplicationFeedbackController {

    private final AdminApplicationFeedbackService adminFeedbackService;

    @GetMapping
    @Operation(summary = "List Admin Application Feedback", description = "Searches and filters feedback across all platform actors and tenants.")
    public ResponseEntity<ApiResponse<PagedResponse<AdminApplicationFeedbackSummaryDto>>> getFeedbackList(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ApplicationFeedbackActorType actorType,
            @RequestParam(required = false) ApplicationFeedbackType type,
            @RequestParam(required = false) ApplicationFeedbackCategory category,
            @RequestParam(required = false) ApplicationFeedbackStatus status,
            @RequestParam(required = false) ApplicationFeedbackPriority priority,
            @RequestParam(required = false) UUID practiceId,
            @RequestParam(required = false) FeedbackTeam assignedTeam,
            @RequestParam(required = false) UUID assignedUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), Sort.by(direction, sortBy));

        PagedResponse<AdminApplicationFeedbackSummaryDto> result = adminFeedbackService.getFeedbackList(
                search, actorType, type, category, status, priority, practiceId, assignedTeam, assignedUserId, fromDate, toDate, pageable
        );
        return ResponseEntity.ok(ApiResponse.success("Feedback inbox retrieved successfully", result));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get Admin Feedback KPIs", description = "Retrieves aggregate counts across status and priority queues.")
    public ResponseEntity<ApiResponse<AdminFeedbackStatsDto>> getStats() {
        return ResponseEntity.ok(ApiResponse.success("Feedback metrics retrieved successfully", adminFeedbackService.getStats()));
    }

    @GetMapping("/assignees")
    @Operation(summary = "List Available Assignees", description = "Retrieves active platform staff and team members eligible for assignment.")
    public ResponseEntity<ApiResponse<List<AdminAssigneeDto>>> getAssignees() {
        return ResponseEntity.ok(ApiResponse.success("Assignees retrieved successfully", adminFeedbackService.getAssignees()));
    }

    @GetMapping("/teams")
    @Operation(summary = "List Internal Routing Teams", description = "Retrieves all standard Taxoryn internal functional teams.")
    public ResponseEntity<ApiResponse<List<FeedbackTeam>>> getTeams() {
        return ResponseEntity.ok(ApiResponse.success("Teams retrieved successfully", adminFeedbackService.getTeams()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Feedback Detail", description = "Retrieves complete feedback record including reporter info, practice affiliation, notes, and activity timeline.")
    public ResponseEntity<ApiResponse<AdminApplicationFeedbackDetailDto>> getFeedbackDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Feedback details retrieved successfully", adminFeedbackService.getFeedbackDetail(id)));
    }

    @PostMapping("/{id}/start-review")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('FEEDBACK_MANAGE') or hasAuthority('FEEDBACK_REVIEW')")
    @Operation(summary = "Start Feedback Review", description = "Transitions feedback status from NEW to UNDER_REVIEW and logs timeline activity.")
    public ResponseEntity<ApiResponse<AdminApplicationFeedbackDetailDto>> startReview(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Feedback review started", adminFeedbackService.startReview(id)));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('FEEDBACK_MANAGE') or hasAuthority('FEEDBACK_ASSIGN')")
    @Operation(summary = "Assign Feedback", description = "Assigns feedback to an internal functional team and optional assignee.")
    public ResponseEntity<ApiResponse<AdminApplicationFeedbackDetailDto>> assignFeedback(
            @PathVariable UUID id,
            @Valid @RequestBody AssignFeedbackRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Feedback assigned successfully", adminFeedbackService.assignFeedback(id, request)));
    }

    @PostMapping("/{id}/notes")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('FEEDBACK_MANAGE') or hasAuthority('FEEDBACK_REVIEW')")
    @Operation(summary = "Add Internal Note", description = "Adds an internal note invisible to external customers and practice users.")
    public ResponseEntity<ApiResponse<FeedbackNoteDto>> addNote(
            @PathVariable UUID id,
            @Valid @RequestBody CreateFeedbackNoteRequest request
    ) {
        FeedbackNoteDto note = adminFeedbackService.addNote(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Internal note added successfully", note));
    }

    @PatchMapping("/{id}/priority")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('FEEDBACK_MANAGE') or hasAuthority('FEEDBACK_REVIEW')")
    @Operation(summary = "Update Triage Priority", description = "Updates priority rating (LOW, MEDIUM, HIGH, CRITICAL).")
    public ResponseEntity<ApiResponse<AdminApplicationFeedbackDetailDto>> updatePriority(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFeedbackPriorityRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Feedback priority updated", adminFeedbackService.updatePriority(id, request)));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('FEEDBACK_MANAGE') or hasAuthority('FEEDBACK_RESOLVE')")
    @Operation(summary = "Resolve Feedback", description = "Resolves feedback with mandatory resolution notes.")
    public ResponseEntity<ApiResponse<AdminApplicationFeedbackDetailDto>> resolveFeedback(
            @PathVariable UUID id,
            @Valid @RequestBody ResolveFeedbackRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Feedback resolved successfully", adminFeedbackService.resolveFeedback(id, request)));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('FEEDBACK_MANAGE') or hasAuthority('FEEDBACK_RESOLVE')")
    @Operation(summary = "Close Feedback", description = "Closes resolved, rejected, or duplicate feedback.")
    public ResponseEntity<ApiResponse<AdminApplicationFeedbackDetailDto>> closeFeedback(
            @PathVariable UUID id,
            @RequestBody(required = false) CloseFeedbackRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Feedback closed successfully", adminFeedbackService.closeFeedback(id, request)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('FEEDBACK_MANAGE') or hasAuthority('FEEDBACK_RESOLVE')")
    @Operation(summary = "Reject Feedback", description = "Classifies feedback as non-actionable or out of scope.")
    public ResponseEntity<ApiResponse<AdminApplicationFeedbackDetailDto>> rejectFeedback(
            @PathVariable UUID id,
            @Valid @RequestBody RejectFeedbackRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Feedback rejected", adminFeedbackService.rejectFeedback(id, request)));
    }

    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('FEEDBACK_MANAGE') or hasAuthority('FEEDBACK_RESOLVE')")
    @Operation(summary = "Mark Duplicate Feedback", description = "Links duplicate feedback to an existing original feedback ID.")
    public ResponseEntity<ApiResponse<AdminApplicationFeedbackDetailDto>> markDuplicate(
            @PathVariable UUID id,
            @Valid @RequestBody MarkDuplicateFeedbackRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Feedback marked as duplicate", adminFeedbackService.markDuplicate(id, request)));
    }

    @PostMapping("/{id}/escalate")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('FEEDBACK_MANAGE') or hasAuthority('FEEDBACK_ESCALATE')")
    @Operation(summary = "Escalate to Engineering", description = "Creates an internal EngineeringIssue and links it to the feedback record.")
    public ResponseEntity<ApiResponse<EngineeringIssueDto>> escalateToEngineering(
            @PathVariable UUID id,
            @Valid @RequestBody EscalateToEngineeringRequest request
    ) {
        EngineeringIssueDto issue = adminFeedbackService.escalateToEngineering(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Feedback escalated to Engineering successfully", issue));
    }
}
