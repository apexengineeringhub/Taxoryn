package com.taxoryn.module.dsc.dto;

import com.taxoryn.module.dsc.model.DscCertificateType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Register New Digital Signature Certificate (DSC) Request")
public class CreateDscRequest {

    private UUID clientId;

    @NotBlank(message = "DSC holder name is required")
    @Size(min = 2, max = 255, message = "Holder name must be between 2 and 255 characters")
    private String holderName;

    @Size(max = 255, message = "Certificate identifier cannot exceed 255 characters")
    private String certificateIdentifier;

    @Builder.Default
    private DscCertificateType certificateType = DscCertificateType.CLASS_3;

    @Size(max = 255, message = "Issuer name cannot exceed 255 characters")
    private String issuer;

    @NotNull(message = "Issue date is required")
    private LocalDate issuedDate;

    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;

    @Size(max = 500, message = "Applicable services string cannot exceed 500 characters")
    private String applicableServices;

    private String notes;
}
