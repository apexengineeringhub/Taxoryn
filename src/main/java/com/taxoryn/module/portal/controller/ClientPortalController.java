package com.taxoryn.module.portal.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.document.dto.DocumentDownloadDto;
import com.taxoryn.module.document.dto.DocumentDto;
import com.taxoryn.module.document.dto.PresignedUrlResponse;
import com.taxoryn.module.document.dto.UploadDocumentRequest;
import com.taxoryn.module.portal.dto.ClientDocumentRequestDto;
import com.taxoryn.module.portal.dto.ClientGstStatusDto;
import com.taxoryn.module.portal.dto.ClientItrStatusDto;
import com.taxoryn.module.portal.dto.ClientNotificationDto;
import com.taxoryn.module.portal.dto.ClientPortalDashboardDto;
import com.taxoryn.module.portal.dto.ClientPortalProfileDto;
import com.taxoryn.module.portal.dto.ClientPortalUserDto;
import com.taxoryn.module.portal.dto.ClientTaskDto;
import com.taxoryn.module.portal.dto.CreateClientDocumentRequest;
import com.taxoryn.module.portal.dto.RegisterClientPortalUserRequest;
import com.taxoryn.module.portal.dto.UpdateClientPortalProfileRequest;
import com.taxoryn.module.portal.service.ClientPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/portal", "/api/portal"})
@RequiredArgsConstructor
@Tag(name = "Client Portal", description = "Client-facing portal for business clients: Secure login, compliance dashboard, pending documents, GST & ITR tracking, and notifications")
@SecurityRequirement(name = "BearerAuth")
public class ClientPortalController {

    private final ClientPortalService clientPortalService;
    private final com.taxoryn.module.docrequest.service.DocumentRequestService documentRequestService;
    private final com.taxoryn.module.notice.service.TaxNoticeService taxNoticeService;

    // =========================================================================
    // 1. User Management & Onboarding
    // =========================================================================

    @PostMapping("/users")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('CLIENT_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER')")
    @Operation(summary = "Register client portal user", description = "Provisions a new client portal user (CLIENT_ADMIN or CLIENT_USER) linked to a specific client record.")
    public ResponseEntity<ApiResponse<ClientPortalUserDto>> registerClientPortalUser(
            @Valid @RequestBody RegisterClientPortalUserRequest request) {
        ClientPortalUserDto user = clientPortalService.registerClientPortalUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Client portal user created successfully", user));
    }

    @GetMapping("/clients/{clientId}/users")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('CLIENT_ADMIN')")
    @Operation(summary = "List client portal users", description = "Retrieves all portal logins provisioned for a specific client.")
    public ResponseEntity<ApiResponse<List<ClientPortalUserDto>>> getClientPortalUsers(@PathVariable UUID clientId) {
        List<ClientPortalUserDto> users = clientPortalService.getClientPortalUsers(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client portal users retrieved successfully", users));
    }

    // =========================================================================
    // 2. Dashboard & Profile
    // =========================================================================

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Client portal dashboard", description = "Retrieves real-time dashboard with compliance progress, pending document requests, recent filings, and practitioner contacts.")
    public ResponseEntity<ApiResponse<ClientPortalDashboardDto>> getDashboard() {
        ClientPortalDashboardDto dashboard = clientPortalService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success("Dashboard retrieved successfully", dashboard));
    }

    @GetMapping("/preview/{clientId}")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PARTNER') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT')")
    @Operation(summary = "Practice preview of client portal dashboard", description = "Allows practice professionals to view the exact customer portal experience for a specific client.")
    public ResponseEntity<ApiResponse<ClientPortalDashboardDto>> getDashboardPreview(@PathVariable UUID clientId) {
        ClientPortalDashboardDto dashboard = clientPortalService.getDashboardForClient(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client portal preview retrieved successfully", dashboard));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_PROFILE_VIEW') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Client profile details", description = "Retrieves the authenticated client's legal information, addresses, and statutory tax numbers.")
    public ResponseEntity<ApiResponse<ClientPortalProfileDto>> getProfile() {
        ClientPortalProfileDto profile = clientPortalService.getProfile();
        return ResponseEntity.ok(ApiResponse.success("Client profile retrieved successfully", profile));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_PROFILE_UPDATE') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Update client contact information", description = "Updates address, email, contact phone, or avatar for the authenticated client.")
    public ResponseEntity<ApiResponse<ClientPortalProfileDto>> updateProfile(
            @Valid @RequestBody UpdateClientPortalProfileRequest request) {
        ClientPortalProfileDto profile = clientPortalService.updateProfile(request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", profile));
    }

    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_PROFILE_UPDATE') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Upload client profile avatar", description = "Uploads and scans a profile photo / avatar for the authenticated client.")
    public ResponseEntity<ApiResponse<ClientPortalProfileDto>> uploadProfileAvatar(
            @RequestParam("file") MultipartFile file) {
        ClientPortalProfileDto profile = clientPortalService.uploadProfileAvatar(file);
        return ResponseEntity.ok(ApiResponse.success("Avatar uploaded successfully", profile));
    }

    @DeleteMapping("/profile/avatar")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_PROFILE_UPDATE') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Delete client profile avatar", description = "Removes the avatar photo for the authenticated client.")
    public ResponseEntity<ApiResponse<Void>> deleteProfileAvatar() {
        clientPortalService.deleteProfileAvatar();
        return ResponseEntity.ok(ApiResponse.success("Avatar deleted successfully", null));
    }

    @GetMapping("/profile/avatar")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_PROFILE_VIEW') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Stream client avatar image", description = "Streams the avatar binary image for the authenticated client.")
    public ResponseEntity<byte[]> streamProfileAvatar() {
        com.taxoryn.module.user.service.ProfileImageService.AvatarContent avatar = clientPortalService.getProfileAvatar();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avatar.getContentType()))
                .contentLength(avatar.getFileSize())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .header("X-Content-Type-Options", "nosniff")
                .body(avatar.getData());
    }

    // =========================================================================
    // 3. Compliance Status Tracking (GST & ITR)
    // =========================================================================

    @GetMapping({"/gst-status", "/status/gst"})
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_STATUS_VIEW') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Client GST return filing status", description = "Retrieves all GST filings (GSTR-1, GSTR-3B, CMP-08) for the authenticated client.")
    public ResponseEntity<ApiResponse<List<ClientGstStatusDto>>> getGstStatus() {
        List<ClientGstStatusDto> list = clientPortalService.getGstStatus();
        return ResponseEntity.ok(ApiResponse.success("GST status retrieved successfully", list));
    }

    @GetMapping({"/itr-status", "/status/itr"})
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_STATUS_VIEW') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Client ITR filing status", description = "Retrieves ITR filing records, assessment years, and e-filing acknowledgement numbers.")
    public ResponseEntity<ApiResponse<List<ClientItrStatusDto>>> getItrStatus() {
        List<ClientItrStatusDto> list = clientPortalService.getItrStatus();
        return ResponseEntity.ok(ApiResponse.success("ITR status retrieved successfully", list));
    }

    // =========================================================================
    // 4. Document Vault & Pending Checklist
    // =========================================================================

    @GetMapping("/documents")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_DOCUMENT_VIEW') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Client document vault", description = "Lists all uploaded documents stored in the client's private vault.")
    public ResponseEntity<ApiResponse<List<DocumentDto>>> getClientDocuments() {
        List<DocumentDto> documents = clientPortalService.getClientDocuments();
        return ResponseEntity.ok(ApiResponse.success("Client documents retrieved successfully", documents));
    }

    @GetMapping("/pending-documents")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_DOCUMENT_VIEW') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Pending document checklist", description = "Lists all compliance documents requested by the consulting firm awaiting upload.")
    public ResponseEntity<ApiResponse<List<ClientDocumentRequestDto>>> getPendingDocuments() {
        List<ClientDocumentRequestDto> pending = clientPortalService.getPendingDocuments();
        return ResponseEntity.ok(ApiResponse.success("Pending document requests retrieved successfully", pending));
    }

    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_DOCUMENT_UPLOAD') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Upload document via client portal", description = "Uploads a compliance document to the client vault and optionally marks a document request as submitted.")
    public ResponseEntity<ApiResponse<DocumentDto>> uploadClientDocument(
            @Parameter(description = "Binary file payload", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = @Schema(type = "string", format = "binary")))
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("metadata") UploadDocumentRequest request,
            @RequestParam(value = "documentRequestId", required = false) UUID documentRequestId) {
        DocumentDto document = clientPortalService.uploadClientDocument(file, request, documentRequestId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Document uploaded successfully", document));
    }

    @GetMapping("/documents/{id}/download")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_DOCUMENT_VIEW') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Download client document", description = "Downloads a document belonging strictly to the authenticated client.")
    public ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody> downloadClientDocument(@PathVariable UUID id) {
        DocumentDownloadDto download = clientPortalService.downloadClientDocument(id);
        String safeDispositionName = sanitizeHeaderFilename(download.getFileName());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeDispositionName + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(download.getFileSize()))
                .body(download.getStream());
    }

    @GetMapping("/documents/{id}/preview")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_DOCUMENT_VIEW') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Preview client document", description = "Previews a document belonging strictly to the authenticated client inline.")
    public ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody> previewClientDocument(@PathVariable UUID id) {
        DocumentDownloadDto download = clientPortalService.previewClientDocument(id);
        String safeDispositionName = sanitizeHeaderFilename(download.getFileName());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeDispositionName + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(download.getFileSize()))
                .body(download.getStream());
    }

    @GetMapping("/documents/{id}/download-url")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_DOCUMENT_VIEW') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Get client document download URL", description = "Generates a secure short-lived presigned download URL for S3/R2 storage or an authenticated streaming URL for local storage.")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getClientDocumentDownloadUrl(@PathVariable UUID id) {
        PresignedUrlResponse response = clientPortalService.getClientDocumentDownloadUrl(id);
        return ResponseEntity.ok(ApiResponse.success("Document download URL generated successfully", response));
    }

    private String sanitizeHeaderFilename(String filename) {
        if (!org.springframework.util.StringUtils.hasText(filename)) return "document.bin";
        return filename.replaceAll("[\r\n\"\\\\]", "_");
    }

    // =========================================================================
    // 5. Tasks & Notifications
    // =========================================================================

    @GetMapping("/tasks")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Client tasks & deliverables", description = "Lists active client-visible tasks and action items.")
    public ResponseEntity<ApiResponse<List<ClientTaskDto>>> getClientTasks() {
        List<ClientTaskDto> tasks = clientPortalService.getClientTasks();
        return ResponseEntity.ok(ApiResponse.success("Tasks retrieved successfully", tasks));
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Client notifications & alerts", description = "Retrieves compliance notifications, filing receipts, and document requests.")
    public ResponseEntity<ApiResponse<List<ClientNotificationDto>>> getClientNotifications() {
        List<ClientNotificationDto> notifications = clientPortalService.getClientNotifications();
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", notifications));
    }

    @PatchMapping("/notifications/{id}/read")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Mark notification as read", description = "Marks the specified notification as read.")
    public ResponseEntity<ApiResponse<Void>> markNotificationRead(@PathVariable UUID id) {
        clientPortalService.markNotificationRead(id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    // =========================================================================
    // 6. Client Billing & Invoices
    // =========================================================================

    @GetMapping("/invoices")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Client invoices", description = "Retrieves all issued invoices and payment receipts for the authenticated client.")
    public ResponseEntity<ApiResponse<List<com.taxoryn.module.billing.dto.InvoiceDto>>> getClientInvoices() {
        List<com.taxoryn.module.billing.dto.InvoiceDto> invoices = clientPortalService.getClientInvoices();
        return ResponseEntity.ok(ApiResponse.success("Invoices retrieved successfully", invoices));
    }

    @GetMapping("/invoices/{id}")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Get client invoice detail", description = "Retrieves invoice details, line items, and payment receipts for the authenticated client.")
    public ResponseEntity<ApiResponse<com.taxoryn.module.billing.dto.InvoiceDto>> getClientInvoiceById(@PathVariable UUID id) {
        com.taxoryn.module.billing.dto.InvoiceDto invoice = clientPortalService.getClientInvoiceById(id);
        return ResponseEntity.ok(ApiResponse.success("Invoice retrieved successfully", invoice));
    }

    // =========================================================================
    // 7. Firm Practitioner Actions for Client
    // =========================================================================

    @PostMapping("/document-requests")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('TASK_CREATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Request document from client", description = "Consultant creates a document request checklist item for the client.")
    public ResponseEntity<ApiResponse<ClientDocumentRequestDto>> requestDocument(
            @Valid @RequestBody CreateClientDocumentRequest request) {
        ClientDocumentRequestDto docRequest = clientPortalService.requestDocumentFromClient(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Document request created successfully", docRequest));
    }

    // =========================================================================
    // 8. Client Portal Multi-Item Document Requests V1
    // =========================================================================

    @GetMapping("/document-requests/v1")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_DOCUMENT_VIEW') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "List client document requests V1", description = "Retrieves all multi-item document requests for the authenticated client.")
    public ResponseEntity<ApiResponse<List<com.taxoryn.module.docrequest.dto.DocumentRequestDto>>> getPortalDocumentRequests() {
        List<com.taxoryn.module.docrequest.dto.DocumentRequestDto> list = documentRequestService.getClientPortalRequests();
        return ResponseEntity.ok(ApiResponse.success("Document requests retrieved successfully", list));
    }

    @GetMapping("/document-requests/v1/{id}")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_DOCUMENT_VIEW') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Get client document request detail V1", description = "Retrieves details and checklist items for a specific document request.")
    public ResponseEntity<ApiResponse<com.taxoryn.module.docrequest.dto.DocumentRequestDto>> getPortalDocumentRequestById(@PathVariable UUID id) {
        com.taxoryn.module.docrequest.dto.DocumentRequestDto req = documentRequestService.getClientPortalRequestById(id);
        return ResponseEntity.ok(ApiResponse.success("Document request retrieved successfully", req));
    }

    @PostMapping(value = "/document-requests/v1/items/{itemId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_DOCUMENT_UPLOAD') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Upload document for request item via Client Portal", description = "Authenticated client uploads a document to fulfill a specific request checklist item.")
    public ResponseEntity<ApiResponse<com.taxoryn.module.docrequest.dto.DocumentRequestDto>> uploadPortalItemDocument(
            @PathVariable UUID itemId,
            @Parameter(description = "Binary file payload", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = @Schema(type = "string", format = "binary")))
            @RequestPart("file") MultipartFile file) {
        com.taxoryn.module.docrequest.dto.DocumentRequestDto result = documentRequestService.uploadClientPortalItemDocument(itemId, file);
        return ResponseEntity.ok(ApiResponse.success("Document uploaded successfully", result));
    }

    @PostMapping("/document-requests/v1/request-acknowledgement")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_DOCUMENT_UPLOAD') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Client requests acknowledgement or document from practitioner", description = "Authenticated client requests an ITR/GST acknowledgement, computation, certificate, or tax document from their practitioner.")
    public ResponseEntity<ApiResponse<com.taxoryn.module.docrequest.dto.DocumentRequestDto>> requestAcknowledgement(
            @Valid @RequestBody com.taxoryn.module.docrequest.dto.CreateClientAcknowledgementRequest request) {
        com.taxoryn.module.docrequest.dto.DocumentRequestDto result = documentRequestService.createClientAcknowledgementRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Acknowledgement request submitted successfully", result));
    }

    @GetMapping("/document-requests/v1/delivered")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_DOCUMENT_VIEW') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "List documents delivered to client", description = "Retrieves all acknowledgements and tax documents delivered by the practitioner.")
    public ResponseEntity<ApiResponse<List<com.taxoryn.module.docrequest.dto.DocumentRequestDto>>> getDeliveredDocuments() {
        List<com.taxoryn.module.docrequest.dto.DocumentRequestDto> list = documentRequestService.getClientPortalDeliveredDocuments();
        return ResponseEntity.ok(ApiResponse.success("Delivered documents retrieved successfully", list));
    }

    @PostMapping("/document-requests/v1/{id}/viewed")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_DOCUMENT_VIEW') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Record delivered document viewed", description = "Transitions status to VIEWED and records audit trail.")
    public ResponseEntity<ApiResponse<Void>> recordDocumentViewed(@PathVariable UUID id) {
        documentRequestService.recordClientDocumentViewed(id);
        return ResponseEntity.ok(ApiResponse.success("Document view recorded", null));
    }

    @PostMapping("/document-requests/v1/{id}/downloaded")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_DOCUMENT_VIEW') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Record delivered document downloaded", description = "Transitions status to DOWNLOADED and records audit trail.")
    public ResponseEntity<ApiResponse<Void>> recordDocumentDownloaded(@PathVariable UUID id) {
        documentRequestService.recordClientDocumentDownloaded(id);
        return ResponseEntity.ok(ApiResponse.success("Document download recorded", null));
    }

    // =========================================================================
    // 9. Client Portal Tax Notices & Scrutiny Cases (Sanitized View)
    // =========================================================================

    @GetMapping("/notices")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Client tax notices", description = "Retrieves sanitized tax notices, hearing dates, and submission statuses for the authenticated client.")
    public ResponseEntity<ApiResponse<com.taxoryn.core.response.PagedResponse<com.taxoryn.module.notice.dto.ClientNoticeDto>>> getClientNotices(
            @ModelAttribute com.taxoryn.core.dto.PageRequestDto pageRequest) {
        UUID clientId = com.taxoryn.core.security.SecurityUtils.requireCurrentClientId();
        com.taxoryn.core.response.PagedResponse<com.taxoryn.module.notice.dto.ClientNoticeDto> notices = taxNoticeService.getClientPortalNotices(clientId, pageRequest);
        return ResponseEntity.ok(ApiResponse.success("Tax notices retrieved successfully", notices));
    }

    // =========================================================================
    // 10. Client Portal Consultation Messaging & Chat
    // =========================================================================

    @GetMapping("/messages")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Client consultation messages", description = "Retrieves chronological message history between the authenticated client and their assigned tax practitioner.")
    public ResponseEntity<ApiResponse<List<com.taxoryn.module.portal.dto.ClientPortalMessageDto>>> getClientMessages() {
        List<com.taxoryn.module.portal.dto.ClientPortalMessageDto> list = clientPortalService.getClientMessages();
        return ResponseEntity.ok(ApiResponse.success("Messages retrieved successfully", list));
    }

    @PostMapping("/messages")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Client sends consultation message", description = "Sends a message to the practitioner and broadcasts via WebSocket.")
    public ResponseEntity<ApiResponse<com.taxoryn.module.portal.dto.ClientPortalMessageDto>> sendClientMessage(
            @Valid @RequestBody com.taxoryn.module.portal.dto.SendClientPortalMessageRequest request) {
        com.taxoryn.module.portal.dto.ClientPortalMessageDto message = clientPortalService.sendClientMessage(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Message sent successfully", message));
    }

    @PostMapping("/messages/read")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Mark messages read by client", description = "Updates read receipts for messages sent to client.")
    public ResponseEntity<ApiResponse<Void>> markMessagesReadByClient() {
        clientPortalService.markMessagesReadByClient();
        return ResponseEntity.ok(ApiResponse.success("Messages marked as read", null));
    }

    @GetMapping("/messages/unread-count")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS') or hasRole('CLIENT_ADMIN') or hasRole('CLIENT_USER')")
    @Operation(summary = "Get unread message count for client", description = "Returns count of unread messages from practitioner.")
    public ResponseEntity<ApiResponse<Long>> getUnreadCountForClient() {
        long count = clientPortalService.getUnreadCountForClient();
        return ResponseEntity.ok(ApiResponse.success("Unread count retrieved", count));
    }

    @GetMapping("/clients/{clientId}/messages")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('STAFF')")
    @Operation(summary = "Practitioner gets client consultation messages", description = "Retrieves message history for a specific client under ABAC scope.")
    public ResponseEntity<ApiResponse<List<com.taxoryn.module.portal.dto.ClientPortalMessageDto>>> getMessagesForClient(
            @PathVariable UUID clientId) {
        List<com.taxoryn.module.portal.dto.ClientPortalMessageDto> list = clientPortalService.getMessagesForClient(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client messages retrieved successfully", list));
    }

    @PostMapping("/clients/{clientId}/messages")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('STAFF')")
    @Operation(summary = "Practitioner sends message to client", description = "Sends a message to client and broadcasts via WebSocket.")
    public ResponseEntity<ApiResponse<com.taxoryn.module.portal.dto.ClientPortalMessageDto>> sendPracticeMessageToClient(
            @PathVariable UUID clientId,
            @Valid @RequestBody com.taxoryn.module.portal.dto.SendClientPortalMessageRequest request) {
        com.taxoryn.module.portal.dto.ClientPortalMessageDto message = clientPortalService.sendPracticeMessageToClient(clientId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Message sent successfully", message));
    }

    @PostMapping("/clients/{clientId}/messages/read")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('STAFF')")
    @Operation(summary = "Mark messages read by practitioner", description = "Updates read receipts for messages sent by client.")
    public ResponseEntity<ApiResponse<Void>> markMessagesReadByPractice(
            @PathVariable UUID clientId) {
        clientPortalService.markMessagesReadByPractice(clientId);
        return ResponseEntity.ok(ApiResponse.success("Messages marked as read", null));
    }

    @GetMapping("/clients/{clientId}/messages/unread-count")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('STAFF')")
    @Operation(summary = "Get unread message count for practitioner", description = "Returns count of unread messages sent by client.")
    public ResponseEntity<ApiResponse<Long>> getUnreadCountForPractice(
            @PathVariable UUID clientId) {
        long count = clientPortalService.getUnreadCountForPractice(clientId);
        return ResponseEntity.ok(ApiResponse.success("Unread count retrieved", count));
    }
}
