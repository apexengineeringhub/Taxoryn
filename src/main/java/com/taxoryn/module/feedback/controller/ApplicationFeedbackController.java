package com.taxoryn.module.feedback.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.feedback.dto.ApplicationFeedbackDto;
import com.taxoryn.module.feedback.dto.CreateApplicationFeedbackRequest;
import com.taxoryn.module.feedback.service.ApplicationFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/feedback", "/api/feedback", "/api/v1/customer/feedback", "/api/v1/marketplace/customer/feedback", "/api/marketplace/customer/feedback"})
@RequiredArgsConstructor
@Tag(name = "Application Feedback", description = "Authenticated user feedback about using the Taxoryn platform")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("isAuthenticated()")
public class ApplicationFeedbackController {

    private final ApplicationFeedbackService feedbackService;

    @PostMapping
    @Operation(summary = "Submit application feedback", description = "Records feedback about Taxoryn itself; it does not create a marketplace practice review")
    public ResponseEntity<ApiResponse<ApplicationFeedbackDto>> createFeedback(
            @Valid @RequestBody CreateApplicationFeedbackRequest request,
            @RequestHeader(value = "X-Feedback-Page", required = false) String page,
            @RequestHeader(value = "X-Feedback-Feature", required = false) String feature
    ) {
        ApplicationFeedbackDto created = feedbackService.createFeedback(request, page, feature);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Thank you for your feedback. Your input helps us improve Taxoryn.", created));
    }

    @GetMapping
    @Operation(summary = "List my application feedback", description = "Returns only feedback submitted by the authenticated user in their authorized context")
    public ResponseEntity<ApiResponse<PagedResponse<ApplicationFeedbackDto>>> getMyFeedback(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success("Your feedback retrieved successfully", feedbackService.getMyFeedback(pageable)));
    }
}
