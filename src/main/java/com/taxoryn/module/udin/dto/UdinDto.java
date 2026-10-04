package com.taxoryn.module.udin.dto;

import com.taxoryn.module.udin.model.UdinDocumentType;
import com.taxoryn.module.udin.model.UdinStatus;
import com.taxoryn.module.udin.model.UdinVerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "UDIN Details Data Transfer Object")
public class UdinDto {

    private UUID id;
    private UUID organizationId;
    private String udin;

    private UUID clientId;
    private String clientName;
    private String clientPan;

    private UUID engagementId;
    private String engagementName;

    private UUID serviceId;
    private String serviceName;

    private UUID documentId;
    private String documentFileName;

    private UUID invoiceId;
    private String invoiceNumber;

    private UdinDocumentType documentType;
    private String documentTitle;
    private String documentDescription;

    private String signatoryName;
    private String signatoryMembershipNo;
    private LocalDate generationDate;

    private UdinStatus status;
    private UdinVerificationStatus verificationStatus;
    private String verificationSource;
    private String verifiedBy;
    private Instant verifiedAt;
    private String verificationRemarks;

    private String financialFiguresJson;
    private String notes;

    private String createdBy;
    private String updatedBy;
    private Instant createdAt;
    private Instant updatedAt;
}
