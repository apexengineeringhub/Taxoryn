package com.taxoryn.module.document.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.document.dto.DocumentDownloadDto;
import com.taxoryn.module.document.dto.DocumentDto;
import com.taxoryn.module.document.dto.DocumentFilterRequest;
import com.taxoryn.module.document.dto.UpdateDocumentRequest;
import com.taxoryn.module.document.dto.UploadDocumentRequest;
import com.taxoryn.module.document.service.DocumentService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/documents", "/api/documents"})
@RequiredArgsConstructor
@Tag(name = "Document Management", description = "Secure multi-tenant document vault: Upload, download, metadata management, and linking to Clients, GST returns, ITR returns, and Tasks")
@SecurityRequirement(name = "BearerAuth")
public class DocumentController {

    private final DocumentService documentService;

    // =========================================================================
    // 1. Upload & Download
    // =========================================================================

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('DOCUMENT_UPLOAD') or hasAuthority('DOCUMENT_CREATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Upload document file", description = "Uploads a document binary and associates it with metadata (Client, GST Filing, ITR Return, or Task).")
    public ResponseEntity<ApiResponse<DocumentDto>> uploadDocument(
            @Parameter(description = "Binary file payload", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = @Schema(type = "string", format = "binary")))
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("metadata") UploadDocumentRequest request) {
        DocumentDto document = documentService.uploadDocument(file, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Document uploaded successfully", document));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW') or hasAuthority('DOCUMENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Download document content", description = "Streams the binary content of the requested document with attachment disposition and strict cache-control.")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable UUID id) {
        DocumentDownloadDto download = documentService.downloadDocument(id);
        String safeDispositionName = sanitizeHeaderFilename(download.getFileName());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeDispositionName + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(download.getFileSize()))
                .body(download.getData());
    }

    @GetMapping("/{id}/preview")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW') or hasAuthority('DOCUMENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Preview document inline", description = "Streams the binary content of the requested document with inline disposition and strict cache-control.")
    public ResponseEntity<byte[]> previewDocument(@PathVariable UUID id) {
        DocumentDownloadDto download = documentService.previewDocument(id);
        String safeDispositionName = sanitizeHeaderFilename(download.getFileName());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeDispositionName + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(download.getFileSize()))
                .body(download.getData());
    }

    private String sanitizeHeaderFilename(String filename) {
        if (!org.springframework.util.StringUtils.hasText(filename)) return "document.bin";
        return filename.replaceAll("[\r\n\"\\\\]", "_");
    }

    // =========================================================================
    // 2. Metadata & Search
    // =========================================================================

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW') or hasAuthority('DOCUMENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get document metadata by ID", description = "Retrieves document properties, file size, checksum, and associations.")
    public ResponseEntity<ApiResponse<DocumentDto>> getDocumentById(@PathVariable UUID id) {
        DocumentDto document = documentService.getDocumentById(id);
        return ResponseEntity.ok(ApiResponse.success("Document metadata retrieved successfully", document));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW') or hasAuthority('DOCUMENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List & filter documents", description = "Retrieves paginated document metadata filtered by client, document type, AY, FY, or filing associations.")
    public ResponseEntity<ApiResponse<PagedResponse<DocumentDto>>> getDocuments(
            @Valid @ModelAttribute DocumentFilterRequest filterRequest) {
        PagedResponse<DocumentDto> response = documentService.getDocuments(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("Documents retrieved successfully", response));
    }

    @GetMapping("/clients/{clientId}")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW') or hasAuthority('DOCUMENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get client document vault", description = "Retrieves all active documents stored in a client's vault.")
    public ResponseEntity<ApiResponse<List<DocumentDto>>> getClientDocuments(@PathVariable UUID clientId) {
        List<DocumentDto> documents = documentService.getClientDocuments(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client documents retrieved successfully", documents));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENT_UPLOAD') or hasAuthority('DOCUMENT_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update document metadata", description = "Updates document category, tags, notes, or assessment/financial year.")
    public ResponseEntity<ApiResponse<DocumentDto>> updateDocument(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDocumentRequest request) {
        DocumentDto document = documentService.updateDocumentMetadata(id, request);
        return ResponseEntity.ok(ApiResponse.success("Document metadata updated successfully", document));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENT_DELETE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete document", description = "Removes file from storage backend and updates document status to DELETED.")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable UUID id) {
        documentService.deleteDocument(id);
        return ResponseEntity.ok(ApiResponse.success("Document deleted successfully", null));
    }
}
