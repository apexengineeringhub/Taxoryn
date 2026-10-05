package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request payload to update comprehensive Client Business Profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to update Client Business Profile")
public class UpdateClientProfileRequest {

    @Size(min = 2, max = 255, message = "Display name must be between 2 and 255 characters")
    @Schema(description = "Client display name", example = "Acme Solutions Pvt Ltd")
    private String displayName;

    @Size(max = 255, message = "Legal name must not exceed 255 characters")
    @Schema(description = "Legal registered name", example = "Acme Solutions Private Limited")
    private String legalName;

    @Size(max = 255, message = "Trade name must not exceed 255 characters")
    @Schema(description = "Trade name", example = "Acme Solutions")
    private String tradeName;

    @Size(max = 50, message = "Client code must not exceed 50 characters")
    @Schema(description = "Client code", example = "CL-ACM-001")
    private String clientCode;

    @Schema(description = "Client entity classification", example = "PRIVATE_LIMITED")
    private ClientType clientType;

    @Size(max = 255, message = "Business activity must not exceed 255 characters")
    @Schema(description = "Business activity description", example = "Software Development and IT Consulting")
    private String businessActivity;

    @Size(max = 100, message = "Industry must not exceed 100 characters")
    @Schema(description = "Industry or sector", example = "Information Technology")
    private String industry;

    @Size(max = 50, message = "Business scale must not exceed 50 characters")
    @Schema(description = "Business scale (MICRO, SMALL, MEDIUM, LARGE, INDIVIDUAL)", example = "SMALL")
    private String businessScale;

    @Schema(description = "Date of incorporation / business commencement")
    private LocalDate dateOfIncorporation;

    @Size(max = 100, message = "Contact person name must not exceed 100 characters")
    @Schema(description = "Primary contact person name", example = "Rajesh Sharma")
    private String contactPersonName;

    @Size(max = 100, message = "Contact person designation must not exceed 100 characters")
    @Schema(description = "Contact person designation", example = "Managing Director")
    private String contactPersonDesignation;

    @Pattern(regexp = "^$|^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Invalid email format")
    @Schema(description = "Primary email address", example = "finance@acme.com")
    private String email;

    @Pattern(regexp = "^$|^[0-9+\\-\\s()]{7,20}$", message = "Invalid phone number format")
    @Schema(description = "Primary phone number", example = "9876543210")
    private String phone;

    @Size(max = 20, message = "Alt phone must not exceed 20 characters")
    @Schema(description = "Alternate phone number")
    private String altPhone;

    @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
    @Schema(description = "Address line 1", example = "Unit 402, Trade Tower")
    private String addressLine1;

    @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
    @Schema(description = "Address line 2", example = "Bandra Kurla Complex")
    private String addressLine2;

    @Size(max = 100, message = "City must not exceed 100 characters")
    @Schema(description = "City", example = "Mumbai")
    private String city;

    @Size(max = 100, message = "State must not exceed 100 characters")
    @Schema(description = "State", example = "Maharashtra")
    private String state;

    @Size(max = 10, message = "State code must not exceed 10 characters")
    @Schema(description = "2-digit GST state code", example = "27")
    private String stateCode;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    @Schema(description = "Country", example = "India")
    private String country;

    @Pattern(regexp = "^$|^[1-9][0-9]{5}$", message = "PIN code must be a 6-digit Indian postal code")
    @Schema(description = "Postal PIN code", example = "400051")
    private String pincode;

    @Pattern(regexp = "^$|^(?i)[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "PAN must be in standard 10-character alphanumeric format (e.g. ABCDE1234F)")
    @Schema(description = "Permanent Account Number (PAN)", example = "ABCDE1234F")
    private String pan;

    @Pattern(regexp = "^$|^(?i)[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", message = "GSTIN must be in standard 15-character format (e.g. 27ABCDE1234F1Z5)")
    @Schema(description = "Goods and Services Tax Identification Number (GSTIN)", example = "27ABCDE1234F1Z5")
    private String gstin;

    @Pattern(regexp = "^$|^(?i)[A-Z]{4}[0-9]{5}[A-Z]{1}$", message = "TAN must be in standard 10-character alphanumeric format (e.g. MUMA12345B)")
    @Schema(description = "Tax Deduction and Collection Account Number (TAN)", example = "MUMA12345B")
    private String tan;

    @Size(max = 21, message = "CIN must not exceed 21 characters")
    @Schema(description = "Corporate Identification Number (CIN)", example = "U72200MH2020PTC123456")
    private String cin;

    @Schema(description = "Primary location ID")
    private UUID locationId;

    @Schema(description = "Assigned practitioner employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Internal notes")
    private String notes;
}
