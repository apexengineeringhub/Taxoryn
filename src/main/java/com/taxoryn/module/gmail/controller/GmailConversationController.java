package com.taxoryn.module.gmail.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.module.gmail.dto.GmailConversationDto;
import com.taxoryn.module.gmail.dto.GmailConversationFilterRequest;
import com.taxoryn.module.gmail.dto.GmailConversationUpdateDto;
import com.taxoryn.module.gmail.dto.GmailLinkClientRequest;
import com.taxoryn.module.gmail.dto.GmailMetricsDto;
import com.taxoryn.module.gmail.service.GmailConversationService;
import com.taxoryn.module.gmail.service.GmailMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/gmail/conversations")
@RequiredArgsConstructor
@Tag(name = "Gmail Conversation Management", description = "Endpoints for tracking, filtering, assigning, and linking Gmail conversation metadata")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("isAuthenticated()")
public class GmailConversationController {

    private final GmailConversationService conversationService;
    private final GmailMetricsService metricsService;
    private final PracticeSecurityScopeEvaluator scopeEvaluator;

    @GetMapping
    @Operation(summary = "Search and list tracked Gmail conversations with practice scoping and multi-criteria filters")
    public ResponseEntity<ApiResponse<PagedResponse<GmailConversationDto>>> getConversations(
            @ModelAttribute GmailConversationFilterRequest filterRequest
    ) {
        PracticeSecurityScope scope = scopeEvaluator.evaluateCurrentScope();
        PagedResponse<GmailConversationDto> response = conversationService.getConversations(filterRequest, scope);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/metrics")
    @Operation(summary = "Retrieve Gmail conversation operational metrics, response times, and SLA status")
    public ResponseEntity<ApiResponse<GmailMetricsDto>> getMetrics() {
        PracticeSecurityScope scope = scopeEvaluator.evaluateCurrentScope();
        GmailMetricsDto metrics = metricsService.getMetrics(scope);
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    @GetMapping("/{conversationId}")
    @Operation(summary = "Get tracked conversation metadata and client context by ID")
    public ResponseEntity<ApiResponse<GmailConversationDto>> getConversation(@PathVariable UUID conversationId) {
        PracticeSecurityScope scope = scopeEvaluator.evaluateCurrentScope();
        GmailConversationDto dto = conversationService.getConversation(conversationId, scope);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PatchMapping("/{conversationId}")
    @Operation(summary = "Update conversation status, priority, assignment, or read/star flags")
    public ResponseEntity<ApiResponse<GmailConversationDto>> updateConversation(
            @PathVariable UUID conversationId,
            @RequestBody GmailConversationUpdateDto updateDto
    ) {
        PracticeSecurityScope scope = scopeEvaluator.evaluateCurrentScope();
        GmailConversationDto updated = conversationService.updateConversation(conversationId, updateDto, scope);
        return ResponseEntity.ok(ApiResponse.success("Conversation updated successfully", updated));
    }

    @PostMapping("/{conversationId}/assign")
    @Operation(summary = "Assign conversation to a specific practice practitioner/user")
    public ResponseEntity<ApiResponse<GmailConversationDto>> assignConversation(
            @PathVariable UUID conversationId,
            @RequestParam(value = "userId", required = false) UUID userId
    ) {
        PracticeSecurityScope scope = scopeEvaluator.evaluateCurrentScope();
        GmailConversationDto assigned = conversationService.assignConversation(conversationId, userId, scope);
        return ResponseEntity.ok(ApiResponse.success("Conversation assigned successfully", assigned));
    }

    @PostMapping("/{conversationId}/link-client")
    @Operation(summary = "Manually link conversation to a Taxoryn client record")
    public ResponseEntity<ApiResponse<GmailConversationDto>> linkClient(
            @PathVariable UUID conversationId,
            @Valid @RequestBody GmailLinkClientRequest request
    ) {
        PracticeSecurityScope scope = scopeEvaluator.evaluateCurrentScope();
        GmailConversationDto linked = conversationService.linkClient(conversationId, request.getClientId(), scope);
        return ResponseEntity.ok(ApiResponse.success("Client linked to conversation successfully", linked));
    }

    @DeleteMapping("/{conversationId}/link-client")
    @Operation(summary = "Unlink client from conversation")
    public ResponseEntity<ApiResponse<GmailConversationDto>> unlinkClient(@PathVariable UUID conversationId) {
        PracticeSecurityScope scope = scopeEvaluator.evaluateCurrentScope();
        GmailConversationDto unlinked = conversationService.unlinkClient(conversationId, scope);
        return ResponseEntity.ok(ApiResponse.success("Client unlinked successfully", unlinked));
    }
}
