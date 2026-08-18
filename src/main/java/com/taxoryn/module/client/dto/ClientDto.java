package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
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
@Schema(description = "Client Master Record Details")
public class ClientDto {

    @Schema(description = "Client unique ID", example = "d1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID id;

    @Schema(description = "Organization ID")
    private UUID organizationId;

    @Schema(description = "Client entity constitution type", example = "PRIVATE_LIMITED")
    private ClientType clientType;

    @Schema(description = "Display name / Operating name", example = "Zenith Infotech Pvt Ltd")
    private String displayName;

    @Schema(description = "Legal registered name", example = "Zenith Information Technologies Private Limited")
    private String legalName;

    @Schema(description = "Trade name / Brand name", example = "Zenith Software")
    private String tradeName;

    @Schema(description = "Permanent Account Number (PAN)", example = "AAACZ1234D")
    private String pan;

    @Schema(description = "GST Identification Number (GSTIN)", example = "27AAACZ1234D1Z8")
    private String gstin;

    @Schema(description = "Tax Deduction and Collection Account Number (TAN)", example = "MUMZ12345A")
    private String tan;

    @Schema(description = "Corporate Identification Number (CIN)", example = "U72200MH2018PTC312345")
    private String cin;

    @Schema(description = "Date of Birth or Incorporation", example = "2018-05-20")
    private LocalDate dateOfIncorporation;

    @Schema(description = "Primary email address", example = "finance@zenithinfo.com")
    private String email;

    @Schema(description = "Primary contact phone", example = "+919811122233")
    private String phone;

    @Schema(description = "Alternate phone number", example = "+919811144455")
    private String altPhone;

    @Schema(description = "Key contact person name", example = "Ramesh Gupta")
    private String contactPersonName;

    @Schema(description = "Contact person designation", example = "Director - Finance")
    private String contactPersonDesignation;

    @Schema(description = "Address line 1", example = "Plot 42, MIDC Industrial Area")
    private String addressLine1;

    @Schema(description = "Address line 2", example = "Andheri East")
    private String addressLine2;

    @Schema(description = "City", example = "Mumbai")
    private String city;

    @Schema(description = "State", example = "Maharashtra")
    private String state;

    @Schema(description = "Country", example = "India")
    private String country;

    @Schema(description = "Postal pincode", example = "400093")
    private String pincode;

    @Schema(description = "Assigned employee / Account manager ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Assigned employee full name", example = "Amit Sharma")
    private String assignedEmployeeName;

    @Schema(description = "Client status", example = "ACTIVE")
    private ClientStatus status;

    @Schema(description = "Internal practitioner notes")
    private String notes;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
}
