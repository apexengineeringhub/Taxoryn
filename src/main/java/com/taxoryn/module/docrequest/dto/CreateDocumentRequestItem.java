package com.taxoryn.module.docrequest.dto;

import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Document Item to Request from Client")
public class CreateDocumentRequestItem {

    @Schema(description = "Document type enum", example = "FORM_16")
    @Builder.Default
    private DocumentType documentType = DocumentType.OTHER;

    @NotBlank(message = "Document title is required")
    @Schema(description = "User-facing title for requested document", example = "Form 16 Part A & B")
    private String title;

    @Schema(description = "Guidance or instructions for client", example = "Signed Form 16 issued by employer")
    private String description;

    @Builder.Default
    private boolean required = true;
}