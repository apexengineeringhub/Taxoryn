package com.taxoryn.module.udin.dto;

import com.taxoryn.module.udin.model.UdinDocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Register New Unique Document Identification Number (UDIN) Request")
public class CreateUdinRequest {

    @NotBlank(message = "UDIN is required")
    @Size(min = 18, max = 18, message = "UDIN must be exactly 18 characters")
    @Pattern(regexp = "^[A-Za-z0-9]{18}$", message = "UDIN must contain only alphanumeric characters")
    private String udin;

    private UUID clientId;

    private UUID engagementId;

    private UUID serviceId;

    private UUID documentId;

    private UUID invoiceId;

    @NotNull(message = "Document type is required")
    private UdinDocumentType documentType;

    @NotBlank(message = "Document title is required")
    @Size(min = 2, max = 255, message = "Document title must be between 2 and 255 characters")
    private String documentTitle;

    private String documentDescription;

    @NotBlank(message = "Signatory name is required")
    @Size(min = 2, max = 255, message = "Signatory name must be between 2 and 255 characters")
    private String signatoryName;

    @Size(max = 50, message = "Signatory membership number cannot exceed 50 characters")
    private String signatoryMembershipNo;

    @NotNull(message = "Generation date is required")
    private LocalDate generationDate;

    private String financialFiguresJson;

    private String notes;
}
