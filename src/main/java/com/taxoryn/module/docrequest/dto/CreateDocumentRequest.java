package com.taxoryn.module.docrequest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create Document Request Payload")
public class CreateDocumentRequest {

    @NotNull(message = "Client ID is required")
    @Schema(description = "Client UUID for whom documents are requested")
    private UUID clientId;

    @NotBlank(message = "Purpose is required")
    @Schema(description = "Purpose of document request", example = "ITR FY 2026-27 Preparation")
    private String purpose;

    @Schema(description = "Due date for document submission")
    private LocalDate dueDate;

    @Schema(description = "Optional custom message for the client")
    private String message;

    @Schema(description = "Financial Year", example = "2026-27")
    private String financialYear;

    @Schema(description = "Assessment Year", example = "2027-28")
    private String assessmentYear;

    @NotEmpty(message = "At least one document item must be requested")
    @Valid
    @Builder.Default
    private List<CreateDocumentRequestItem> items = new ArrayList<>();
}