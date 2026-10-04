package com.taxoryn.module.dsc.dto;

import com.taxoryn.module.dsc.model.DscCertificateType;
import com.taxoryn.module.dsc.model.DscStatus;
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
@Schema(description = "Digital Signature Certificate (DSC) Details")
public class DscDto {

    private UUID id;
    private UUID organizationId;
    private UUID clientId;
    private String clientName;
    private String clientPan;
    private String clientGstin;

    private String holderName;
    private String certificateIdentifier;
    private DscCertificateType certificateType;
    private String issuer;

    private LocalDate issuedDate;
    private LocalDate expiryDate;
    private DscStatus status;
    private Long daysUntilExpiry;

    private String applicableServices;
    private String notes;

    private String createdBy;
    private String updatedBy;
    private Instant createdAt;
    private Instant updatedAt;
}
