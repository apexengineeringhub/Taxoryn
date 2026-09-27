package com.taxoryn.module.gst.dto;

import com.taxoryn.module.gst.model.GstFilingFrequency;
import com.taxoryn.module.gst.model.GstRegistrationStatus;
import com.taxoryn.module.gst.model.GstRegistrationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
@Schema(description = "Request model to create a new GST Registration profile for a client")
public class CreateGstRegistrationRequest {

    @NotNull(message = "Client ID is required")
    @Schema(description = "Client master ID to bind the GST registration with", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID clientId;

    @Schema(description = "Location ID where this GSTIN is serviced")
    private UUID locationId;

    @NotBlank(message = "GSTIN is required")
    @Pattern(
            regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
            message = "Invalid GSTIN format. Expected standard 15-character Indian GSTIN format (e.g. 27ABCDE1234F1Z5)"
    )
    @Schema(description = "15-character Goods and Services Tax Identification Number", example = "27ABCDE1234F1Z5", requiredMode = Schema.RequiredMode.REQUIRED)
    private String gstin;

    @Schema(description = "Legal entity name as recorded on GST registration")
    private String legalName;

    @Schema(description = "Trade or branch operating name")
    private String tradeName;

    @Builder.Default
    @Schema(description = "Taxpayer registration category (REGULAR, COMPOSITION, QRMP, etc.)", defaultValue = "REGULAR")
    private GstRegistrationType registrationType = GstRegistrationType.REGULAR;

    @Builder.Default
    @Schema(description = "Statutory registration status (ACTIVE, SUSPENDED, CANCELLED, etc.)", defaultValue = "ACTIVE")
    private GstRegistrationStatus registrationStatus = GstRegistrationStatus.ACTIVE;

    @Schema(description = "Date of registration grant")
    private LocalDate registrationDate;

    @Schema(description = "2-digit state code (auto-derived from GSTIN prefix if omitted)")
    private String stateCode;

    @Schema(description = "Jurisdictional circle / ward / division")
    private String jurisdiction;

    @Builder.Default
    @Schema(description = "Statutory return filing frequency (MONTHLY, QUARTERLY, ANNUALLY)", defaultValue = "MONTHLY")
    private GstFilingFrequency filingFrequency = GstFilingFrequency.MONTHLY;

    @Schema(description = "Effective start date of registration")
    private LocalDate effectiveFrom;

    @Schema(description = "Effective end date of registration")
    private LocalDate effectiveTo;

    @Builder.Default
    @Schema(description = "Whether the registration is actively tracked", defaultValue = "true")
    private Boolean active = true;
}
