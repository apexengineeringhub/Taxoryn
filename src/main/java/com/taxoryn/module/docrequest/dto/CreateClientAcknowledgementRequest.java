package com.taxoryn.module.docrequest.dto;

import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.DocumentCategory;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Payload for client requesting an acknowledgement or document from practitioner")
public class CreateClientAcknowledgementRequest {

    @NotNull(message = "Document category is required")
    @Schema(description = "Document category", example = "ACKNOWLEDGEMENT")
    private DocumentCategory category;

    @Schema(description = "Specific document type", example = "ITR_ACKNOWLEDGEMENT")
    @Builder.Default
    private DocumentType documentType = DocumentType.OTHER;

    @NotBlank(message = "Purpose/Subject is required")
    @Schema(description = "Subject or Purpose of the request", example = "ITR Acknowledgement AY 2026-27")
    private String purpose;

    @Schema(description = "Financial Year (e.g. 2025-26)", example = "2025-26")
    private String financialYear;

    @Schema(description = "Assessment Year (e.g. 2026-27)", example = "2026-27")
    private String assessmentYear;

    @Schema(description = "Tax Period (e.g. Q1, July 2026)", example = "Q1")
    private String taxPeriod;

    @Schema(description = "Optional custom message for practitioner", example = "Please send me my ITR acknowledgement.")
    private String message;

    @Schema(description = "Optional linked compliance obligation or return ID")
    private UUID complianceId;

    @Schema(description = "Optional linked task ID")
    private UUID taskId;
}
