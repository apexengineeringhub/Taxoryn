package com.taxoryn.module.portal.dto;

import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Firm Request Document from Client Payload")
public class CreateClientDocumentRequest {

    @NotNull(message = "Client ID is required")
    private UUID clientId;

    @NotNull(message = "Document type is required")
    private DocumentType documentType;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    private LocalDate dueDate;
    private String financialYear;
    private String assessmentYear;
}
