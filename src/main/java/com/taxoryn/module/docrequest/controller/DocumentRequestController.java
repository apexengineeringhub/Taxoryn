package com.taxoryn.module.docrequest.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.docrequest.dto.CreateDocumentRequest;
import com.taxoryn.module.docrequest.dto.DocumentRequestDto;
import com.taxoryn.module.docrequest.dto.DocumentRequestFilterRequest;
import com.taxoryn.module.docrequest.dto.DocumentRequestSummaryDto;
import com.taxoryn.module.docrequest.dto.RejectDocumentItemRequest;
import com.taxoryn.module.docrequest.service.DocumentRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/document-requests", "/api/document-requests"})
@RequiredArgsConstructor
@Tag(name = "Document Requests", description = "Practitioner Document Request Workflow & Document Checklist Verification")
@SecurityRequirement(name = "BearerAuth")
public class DocumentRequestController {

    private final DocumentRequestService documentRequestService;

    @PostMapping
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('DOCUMENT_WRITE') or hasAuthority('CLIENT_CREATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('STAFF')")
    @Operation(summary = "Create & send document request", description = "Creates a structured multi-item document request for a client and dispatches notifications.")
    public ResponseEntity<ApiResponse<DocumentRequestDto>> createAndSendRequest(
            @Valid @RequestBody CreateDocumentRequest request) {
        DocumentRequestDto result = documentRequestService.createAndSendRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Document request created and dispatched successfully", result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('DOCUMENT_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT')")
    @Operation(summary = "Get document request detail", description = "Retrieves document request with all checklist items and status progress.")
    public ResponseEntity<ApiResponse<DocumentRequestDto>> getRequestById(@PathVariable UUID id) {
        DocumentRequestDto result = documentRequestService.getRequestById(id);
        return ResponseEntity.ok(ApiResponse.success("Document request retrieved successfully", result));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('DOCUMENT_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT')")
    @Operation(summary = "List document requests", description = "Queries document requests with pagination and status filters.")
    public ResponseEntity<ApiResponse<PagedResponse<DocumentRequestDto>>> getRequests(
            @ModelAttribute DocumentRequestFilterRequest filter) {
        PagedResponse<DocumentRequestDto> result = documentRequestService.getRequests(filter);
        return ResponseEntity.ok(ApiResponse.success("Document requests retrieved successfully", result));
    }

    @GetMapping("/clients/{clientId}")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('DOCUMENT_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT')")
    @Operation(summary = "List client document requests", description = "Retrieves all document requests for a specific client.")
    public ResponseEntity<ApiResponse<List<DocumentRequestDto>>> getClientRequests(@PathVariable UUID clientId) {
        List<DocumentRequestDto> result = documentRequestService.getClientRequests(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client document requests retrieved successfully", result));
    }

    @GetMapping("/summary/stats")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('DOCUMENT_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT')")
    @Operation(summary = "Get document requests summary metrics", description = "Retrieves counts of pending, partially completed, completed, and overdue requests.")
    public ResponseEntity<ApiResponse<DocumentRequestSummaryDto>> getSummaryStats() {
        DocumentRequestSummaryDto result = documentRequestService.getSummaryStats();
        return ResponseEntity.ok(ApiResponse.success("Document request summary metrics retrieved successfully", result));
    }

    @PostMapping("/items/{itemId}/accept")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('DOCUMENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('STAFF')")
    @Operation(summary = "Accept document item", description = "Practitioner accepts an uploaded document item. If all required items are accepted, marks request COMPLETED.")
    public ResponseEntity<ApiResponse<DocumentRequestDto>> acceptItem(@PathVariable UUID itemId) {
        DocumentRequestDto result = documentRequestService.acceptItem(itemId);
        return ResponseEntity.ok(ApiResponse.success("Document item accepted successfully", result));
    }

    @PostMapping("/items/{itemId}/reject")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('DOCUMENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('STAFF')")
    @Operation(summary = "Reject document item", description = "Practitioner rejects an uploaded document item with a mandatory reason, allowing client re-upload.")
    public ResponseEntity<ApiResponse<DocumentRequestDto>> rejectItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody RejectDocumentItemRequest request) {
        DocumentRequestDto result = documentRequestService.rejectItem(itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Document item rejected with correction notice", result));
    }

    @PostMapping(value = "/items/{itemId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('DOCUMENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('STAFF')")
    @Operation(summary = "Upload document for request item", description = "Practitioner uploads a document on behalf of the client for a specific item.")
    public ResponseEntity<ApiResponse<DocumentRequestDto>> uploadItemDocument(
            @PathVariable UUID itemId,
            @Parameter(description = "Binary file payload", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = @Schema(type = "string", format = "binary")))
            @RequestPart("file") MultipartFile file) {
        DocumentRequestDto result = documentRequestService.uploadItemDocument(itemId, file);
        return ResponseEntity.ok(ApiResponse.success("Document uploaded and associated successfully", result));
    }

    @PostMapping("/{id}/remind")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('DOCUMENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('STAFF')")
    @Operation(summary = "Send document request reminder", description = "Dispatches a friendly reminder notification and email to the client for pending documents.")
    public ResponseEntity<ApiResponse<Void>> sendReminder(@PathVariable UUID id) {
        documentRequestService.sendReminder(id);
        return ResponseEntity.ok(ApiResponse.success("Document request reminder dispatched successfully", null));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('DOCUMENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER')")
    @Operation(summary = "Cancel document request", description = "Cancels a document request.")
    public ResponseEntity<ApiResponse<DocumentRequestDto>> cancelRequest(@PathVariable UUID id) {
        DocumentRequestDto result = documentRequestService.cancelRequest(id);
        return ResponseEntity.ok(ApiResponse.success("Document request cancelled successfully", result));
    }
}