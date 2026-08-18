package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
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
@Schema(description = "Update Client Master Request Payload")
public class UpdateClientRequest {

    @NotNull(message = "Client type is required")
    @Schema(description = "Constitution / Legal Type", example = "PRIVATE_LIMITED")
    private ClientType clientType;

    @NotBlank(message = "Display name is required")
    @Size(min = 2, max = 255, message = "Display name must be between 2 and 255 characters")
    @Schema(description = "Client primary display name", example = "Zenith Infotech Pvt Ltd")
    private String displayName;

    @Size(max = 255, message = "Legal name cannot exceed 255 characters")
    @Schema(description = "Full legal registered name", example = "Zenith Information Technologies Private Limited")
    private String legalName;

    @Size(max = 255, message = "Trade name cannot exceed 255 characters")
    @Schema(description = "Trade name / Brand name", example = "Zenith Software")
    private String tradeName;

    @Pattern(regexp = "^$|^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Invalid PAN format (expected e.g. ABCDE1234F)")
    @Schema(description = "Permanent Account Number (PAN)", example = "AAACZ1234D")
    private String pan;

    @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", message = "Invalid GSTIN format (expected 15-character GSTIN)")
    @Schema(description = "GSTIN", example = "27AAACZ1234D1Z8")
    private String gstin;

    @Pattern(regexp = "^$|^[A-Z]{4}[0-9]{5}[A-Z]{1}$", message = "Invalid TAN format (expected 10-character TAN, e.g. MUMZ12345A)")
    @Schema(description = "TAN Number", example = "MUMZ12345A")
    private String tan;

    @Pattern(regexp = "^$|^[UL]{1}[0-9]{5}[A-Z]{2}[0-9]{4}[A-Z]{3}[0-9]{6}$", message = "Invalid CIN format (expected 21-character Corporate ID)")
    @Schema(description = "Corporate Identification Number (CIN)", example = "U72200MH2018PTC312345")
    private String cin;

    @Schema(description = "Date of Birth or Incorporation", example = "2018-05-20")
    private LocalDate dateOfIncorporation;

    @Email(message = "Invalid email format")
    @Schema(description = "Primary email address", example = "finance@zenithinfo.com")
    private String email;

    @Pattern(regexp = "^$|^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    @Schema(description = "Contact phone number", example = "+919811122233")
    private String phone;

    @Pattern(regexp = "^$|^\\+?[0-9]{10,15}$", message = "Invalid alternate phone number format")
    @Schema(description = "Alternate contact phone", example = "+919811144455")
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

    @Schema(description = "Country", example = "India", defaultValue = "India")
    private String country;

    @Pattern(regexp = "^$|^[0-9]{6}$", message = "Invalid Indian postal PIN code")
    @Schema(description = "Postal pincode", example = "400093")
    private String pincode;

    @Schema(description = "Assigned practitioner / Account manager employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Client status")
    private ClientStatus status;

    @Schema(description = "Internal practitioner notes")
    private String notes;
}
