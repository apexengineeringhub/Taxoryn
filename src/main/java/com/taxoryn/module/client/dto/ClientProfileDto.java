package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Rich structured Client Business Profile DTO.
 * Exposes core identity, classification, business information, contacts, structured address,
 * statutory identifiers, and deterministic completeness metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Comprehensive Client Business Profile")
public class ClientProfileDto {

    @Schema(description = "Client unique ID")
    private UUID id;

    @Schema(description = "Organization / practice tenant ID")
    private UUID organizationId;

    // --- IDENTITY ---
    @Schema(description = "Client display / primary name")
    private String displayName;

    @Schema(description = "Legal registered entity name")
    private String legalName;

    @Schema(description = "Trade / business brand name")
    private String tradeName;

    @Schema(description = "Practice client code")
    private String clientCode;

    // --- CLASSIFICATION ---
    @Schema(description = "Entity constitution / legal classification")
    private ClientType clientType;

    @Schema(description = "Current lifecycle status")
    private ClientStatus status;

    @Schema(description = "Timestamp of last status transition")
    private java.time.Instant statusChangedAt;

    @Schema(description = "User ID who performed the last status transition")
    private UUID statusChangedBy;

    @Schema(description = "Business reason for the last status transition")
    private String statusChangeReason;

    // --- BUSINESS INFORMATION ---
    @Schema(description = "Business activity description")
    private String businessActivity;

    @Schema(description = "Industry or business sector")
    private String industry;

    @Schema(description = "Business scale (MICRO, SMALL, MEDIUM, LARGE, INDIVIDUAL)")
    private String businessScale;

    @Schema(description = "Date of incorporation / business commencement")
    private LocalDate dateOfIncorporation;

    // --- CONTACT INFORMATION ---
    @Schema(description = "Primary contact person full name")
    private String contactPersonName;

    @Schema(description = "Primary contact person designation / role")
    private String contactPersonDesignation;

    @Schema(description = "Primary email address")
    private String email;

    @Schema(description = "Primary phone number")
    private String phone;

    @Schema(description = "Alternative phone / landline")
    private String altPhone;

    // --- ADDRESS ---
    @Schema(description = "Address Line 1 (Street / Building)")
    private String addressLine1;

    @Schema(description = "Address Line 2 (Locality / Area)")
    private String addressLine2;

    @Schema(description = "City")
    private String city;

    @Schema(description = "State")
    private String state;

    @Schema(description = "2-digit GST state code")
    private String stateCode;

    @Schema(description = "Country")
    private String country;

    @Schema(description = "Postal code / PIN code")
    private String pincode;

    // --- STATUTORY IDENTIFIERS ---
    @Schema(description = "Permanent Account Number (PAN)")
    private String pan;

    @Schema(description = "Primary Goods and Services Tax Identification Number (GSTIN)")
    private String gstin;

    @Schema(description = "Tax Deduction and Collection Account Number (TAN)")
    private String tan;

    @Schema(description = "Corporate Identification Number (CIN) or LLPIN")
    private String cin;

    // --- ASSIGNMENT & METADATA ---
    @Schema(description = "Primary servicing location ID")
    private UUID locationId;

    @Schema(description = "Primary assigned practitioner / employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Assigned practitioner full name")
    private String assignedEmployeeName;

    @Schema(description = "Internal client notes")
    private String notes;

    @Schema(description = "Client profile completeness assessment")
    private ClientProfileCompletenessDto completeness;
}
