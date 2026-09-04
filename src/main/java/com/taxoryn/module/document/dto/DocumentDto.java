package com.taxoryn.module.document.dto;

import com.taxoryn.module.document.entity.DocumentEntity.DocumentStatus;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import com.taxoryn.module.document.entity.DocumentEntity.StorageProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Document Metadata Details")
public class DocumentDto {

    @Schema(description = "Document ID")
    private UUID id;

    @Schema(description = "Organization ID")
    private UUID organizationId;

    @Schema(description = "Associated Client ID")
    private UUID clientId;

    @Schema(description = "Client display name", example = "ABC Traders")
    private String clientName;

    @Schema(description = "Associated GST Return Filing ID")
    private UUID gstFilingId;

    @Schema(description = "Associated ITR Return Filing ID")
    private UUID itrReturnId;

    @Schema(description = "Associated Task ID")
    private UUID taskId;

    @Schema(description = "Document category type", example = "FORM_16")
    private DocumentType documentType;

    @Schema(description = "Original file name", example = "Form16_FY2025-26.pdf")
    private String fileName;

    @Schema(description = "MIME content type", example = "application/pdf")
    private String contentType;

    @Schema(description = "File size in bytes", example = "1048576")
    private long fileSize;

    @Schema(description = "Human-readable file size", example = "1.00 MB")
    private String fileSizeFormatted;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Schema(hidden = true)
    private String storageKey;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Schema(hidden = true)
    private StorageProvider storageProvider;

    @Schema(description = "Financial Year (e.g. 2025-26)", example = "2025-26")
    private String financialYear;

    @Schema(description = "Assessment Year (e.g. 2026-27)", example = "2026-27")
    private String assessmentYear;

    @Schema(description = "Document status", example = "ACTIVE")
    private DocumentStatus status;

    @Schema(description = "SHA-256 Checksum hash")
    private String checksum;

    @Schema(description = "User notes and tags")
    private String notes;

    @Schema(description = "Uploader user email")
    private String uploadedBy;

    @Schema(description = "Upload timestamp")
    private Instant uploadedAt;
}
