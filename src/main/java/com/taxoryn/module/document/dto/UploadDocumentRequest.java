package com.taxoryn.module.document.dto;

import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Upload Document Metadata Parameters")
public class UploadDocumentRequest {

    @Schema(description = "Associated Client ID (optional if linked to general task)")
    private UUID clientId;

    @NotNull(message = "Document type is required")
    @Schema(description = "Document category type", example = "FORM_16")
    private DocumentType documentType;

    @Schema(description = "Financial Year (e.g. 2025-26)", example = "2025-26")
    private String financialYear;

    @Schema(description = "Assessment Year (e.g. 2026-27)", example = "2026-27")
    private String assessmentYear;

    @Schema(description = "Associated GST Return Filing ID (optional)")
    private UUID gstFilingId;

    @Schema(description = "Associated ITR Return Filing ID (optional)")
    private UUID itrReturnId;

    @Schema(description = "Associated TDS Return Filing ID (optional)")
    private UUID tdsReturnId;

    @Schema(description = "Associated Task ID (optional)")
    private UUID taskId;

    @Schema(description = "Document description or notes")
    private String notes;
}
