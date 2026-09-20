package com.taxoryn.module.docrequest.dto;

import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.DocumentCategory;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload for practitioner sending document/acknowledgement to client")
public class SendDocumentToClientRequest {

    @Schema(description = "Client UUID (mandatory if not fulfilling an existing request)")
    private UUID clientId;

    @Schema(description = "Optional Document Request ID if fulfilling an existing client request")
    private UUID requestId;

    @Schema(description = "Optional existing clean Document UUID from client vault (if not uploading a new file)")
    private UUID existingDocumentId;

    @Schema(description = "Document Category", example = "ACKNOWLEDGEMENT")
    @Builder.Default
    private DocumentCategory category = DocumentCategory.ACKNOWLEDGEMENT;

    @Schema(description = "Document Type", example = "ITR_ACKNOWLEDGEMENT")
    @Builder.Default
    private DocumentType documentType = DocumentType.OTHER;

    @Schema(description = "Document Title or Purpose", example = "ITR-V Acknowledgement AY 2026-27")
    private String title;

    @Schema(description = "Financial Year (e.g. 2025-26)", example = "2025-26")
    private String financialYear;

    @Schema(description = "Assessment Year (e.g. 2026-27)", example = "2026-27")
    private String assessmentYear;

    @Schema(description = "Tax Period (e.g. Q1, July 2026)", example = "Q1")
    private String taxPeriod;

    @Schema(description = "Optional message to client", example = "Your ITR acknowledgement is ready.")
    private String message;

    @Schema(description = "Optional linked task ID")
    private UUID taskId;

    @Schema(description = "Optional linked compliance obligation ID")
    private UUID complianceId;

    @Schema(description = "Optional linked GST filing ID")
    private UUID gstFilingId;

    @Schema(description = "Optional linked ITR return ID")
    private UUID itrReturnId;

    @Schema(description = "Optional linked TDS return ID")
    private UUID tdsReturnId;
}
