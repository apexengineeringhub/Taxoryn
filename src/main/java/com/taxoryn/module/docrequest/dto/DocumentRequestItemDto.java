package com.taxoryn.module.docrequest.dto;

import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
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
@Schema(description = "Requested Document Item Detail")
public class DocumentRequestItemDto {

    private UUID id;
    private UUID requestId;
    private UUID clientId;
    private DocumentType documentType;
    private String title;
    private String description;
    private boolean required;
    private ItemStatus status;
    private UUID uploadedDocumentId;
    private String uploadedDocumentName;
    private Long uploadedDocumentSize;
    private String uploadedDocumentContentType;
    private Instant uploadedAt;
    private UUID reviewedByUserId;
    private String reviewedByName;
    private Instant reviewedAt;
    private String rejectionReason;
}