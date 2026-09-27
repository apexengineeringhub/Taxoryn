package com.taxoryn.module.gst.dto;

import com.taxoryn.module.gst.model.GstFilingFrequency;
import com.taxoryn.module.gst.model.GstRegistrationStatus;
import com.taxoryn.module.gst.model.GstRegistrationType;
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
@Schema(description = "GST Registration response model representing a client's statutory GSTIN profile")
public class GstRegistrationDto {

    @Schema(description = "Unique GST Registration ID")
    private UUID id;

    @Schema(description = "Tenant organization ID")
    private UUID organizationId;

    @Schema(description = "Client master ID")
    private UUID clientId;

    @Schema(description = "Client display name")
    private String clientName;

    @Schema(description = "Servicing location ID")
    private UUID locationId;

    @Schema(description = "Servicing location name")
    private String locationName;

    @Schema(description = "15-character Goods and Services Tax Identification Number")
    private String gstin;

    @Schema(description = "Legal business name as per GST certificate")
    private String legalName;

    @Schema(description = "Trade or brand name")
    private String tradeName;

    @Schema(description = "Taxpayer registration category")
    private GstRegistrationType registrationType;

    @Schema(description = "Statutory registration status")
    private GstRegistrationStatus registrationStatus;

    @Schema(description = "Date of GST registration grant")
    private LocalDate registrationDate;

    @Schema(description = "2-digit state code prefix")
    private String stateCode;

    @Schema(description = "Tax jurisdiction / circle / ward")
    private String jurisdiction;

    @Schema(description = "Statutory return filing frequency")
    private GstFilingFrequency filingFrequency;

    @Schema(description = "Effective start date of registration")
    private LocalDate effectiveFrom;

    @Schema(description = "Effective end/cancellation date of registration")
    private LocalDate effectiveTo;

    @Schema(description = "Active flag")
    private boolean active;

    @Schema(description = "Creation audit timestamp")
    private Instant createdAt;

    @Schema(description = "Last update audit timestamp")
    private Instant updatedAt;

    @Schema(description = "Creator username/id")
    private String createdBy;

    @Schema(description = "Modifier username/id")
    private String updatedBy;
}
