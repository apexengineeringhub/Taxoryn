package com.taxoryn.module.portal.dto;

import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import com.taxoryn.module.portal.entity.ClientDocumentRequestEntity.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pending Document Request Checklist Item")
public class ClientDocumentRequestDto {

    private UUID id;
    private UUID clientId;
    private DocumentType documentType;
    private String title;
    private String description;
    private LocalDate dueDate;
    private String financialYear;
    private String assessmentYear;
    private RequestStatus status;
    private UUID uploadedDocumentId;
}
