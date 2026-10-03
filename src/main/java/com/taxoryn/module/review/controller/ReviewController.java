package com.taxoryn.module.review.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.review.dto.ReviewDecisionRequest;
import com.taxoryn.module.review.dto.ReviewRequestDto;
import com.taxoryn.module.review.dto.SubmitReviewRequest;
import com.taxoryn.module.review.service.ReviewService;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.TASKS)
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewRequestDto>> submit(@Valid @RequestBody SubmitReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Review submitted", reviewService.submit(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReviewRequestDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success("Reviews retrieved", reviewService.list()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReviewRequestDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Review retrieved", reviewService.get(id)));
    }

    @GetMapping("/resource/{resourceType}/{resourceId}")
    public ResponseEntity<ApiResponse<ReviewRequestDto>> getForResource(@PathVariable String resourceType,
            @PathVariable UUID resourceId) {
        return ResponseEntity.ok(ApiResponse.success("Review retrieved", reviewService.getForResource(resourceType, resourceId)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ReviewRequestDto>> approve(@PathVariable UUID id,
            @Valid @RequestBody(required = false) ReviewDecisionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Review approved", reviewService.approve(id, request == null ? null : request.getComment())));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<ReviewRequestDto>> reject(@PathVariable UUID id, @Valid @RequestBody ReviewDecisionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Review rejected", reviewService.reject(id, request.getComment())));
    }

    @PostMapping("/{id}/resubmit")
    public ResponseEntity<ApiResponse<ReviewRequestDto>> resubmit(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Review resubmitted", reviewService.resubmit(id)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ReviewRequestDto>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Review cancelled", reviewService.cancel(id)));
    }
}
