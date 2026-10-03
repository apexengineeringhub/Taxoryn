package com.taxoryn.module.dsc.dto;

import com.taxoryn.module.dsc.model.DscCertificateType;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Update Digital Signature Certificate (DSC) Request")
public class UpdateDscRequest {

    private UUID clientId;

    @Size(min = 2, max = 255, message = "Holder name must be between 2 and 255 characters")
    private String holderName;

    @Size(max = 255, message = "Certificate identifier cannot exceed 255 characters")
    private String certificateIdentifier;

    private DscCertificateType certificateType;

    @Size(max = 255, message = "Issuer name cannot exceed 255 characters")
    private String issuer;

    private LocalDate issuedDate;

    private LocalDate expiryDate;

    @Size(max = 500, message = "Applicable services string cannot exceed 500 characters")
    private String applicableServices;

    private String notes;
}
