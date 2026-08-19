package com.taxoryn.module.document.dto;

import com.taxoryn.module.document.entity.DocumentEntity.DocumentStatus;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update Document Metadata Payload")
public class UpdateDocumentRequest {

    @Schema(description = "Document category type", example = "FORM_16")
    private DocumentType documentType;

    @Schema(description = "Financial Year", example = "2025-26")
    private String financialYear;

    @Schema(description = "Assessment Year", example = "2026-27")
    private String assessmentYear;

    @Schema(description = "Document status")
    private DocumentStatus status;

    @Schema(description = "Notes or remarks")
    private String notes;
}
