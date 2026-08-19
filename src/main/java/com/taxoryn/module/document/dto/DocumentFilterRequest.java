package com.taxoryn.module.document.dto;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentStatus;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Document Search & Filter Parameters")
public class DocumentFilterRequest extends PageRequestDto {

    @Schema(description = "Search term across filename, notes, client name")
    private String search;

    @Schema(description = "Filter by Client ID")
    private UUID clientId;

    @Schema(description = "Filter by Document Type")
    private DocumentType documentType;

    @Schema(description = "Filter by Financial Year (e.g. 2025-26)")
    private String financialYear;

    @Schema(description = "Filter by Assessment Year (e.g. 2026-27)")
    private String assessmentYear;

    @Schema(description = "Filter by GST Return Filing ID")
    private UUID gstFilingId;

    @Schema(description = "Filter by ITR Return Filing ID")
    private UUID itrReturnId;

    @Schema(description = "Filter by Task ID")
    private UUID taskId;

    @Schema(description = "Filter by Document Status")
    private DocumentStatus status;
}
