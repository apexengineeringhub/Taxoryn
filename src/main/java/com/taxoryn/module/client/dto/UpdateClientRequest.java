package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
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

    @Schema(description = "Constitution / Legal Type", example = "PRIVATE_LIMITED")
    private ClientType clientType;

    @Size(max = 50, message = "Client code cannot exceed 50 characters")
    @Schema(description = "Unique client alphanumeric code within practice", example = "CLI-001")
    private String clientCode;

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

    // Normalized and validated in the service so lowercase and surrounding whitespace are accepted.
    @Schema(description = "GSTIN", example = "27AAACZ1234D1Z8")
    private String gstin;

    @Pattern(regexp = "^$|^[A-Z]{4}[0-9]{5}[A-Z]{1}$", message = "Invalid TAN format (expected 10-character TAN, e.g. MUMZ12345A)")
    @Schema(description = "TAN Number", example = "MUMZ12345A")
    private String tan;

    @Size(max = 255, message = "Business activity cannot exceed 255 characters")
    @Schema(description = "Business activity description", example = "Software development and cloud services")
    private String businessActivity;

    @Size(max = 100, message = "Industry cannot exceed 100 characters")
    @Schema(description = "Industry or sector", example = "Information Technology")
    private String industry;

    @Size(max = 50, message = "Business scale cannot exceed 50 characters")
    @Schema(description = "Business scale (MICRO, SMALL, MEDIUM, LARGE, INDIVIDUAL)", example = "SMALL")
    private String businessScale;

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

    @Size(max = 10, message = "State code must not exceed 10 characters")
    @Schema(description = "2-digit GST state code", example = "27")
    private String stateCode;

    @Schema(description = "Country", example = "India", defaultValue = "India")
    private String country;

    @Pattern(regexp = "^$|^[0-9]{6}$", message = "Invalid Indian postal PIN code")
    @Schema(description = "Postal pincode", example = "400093")
    private String pincode;

    @Schema(description = "Practice branch / Location ID")
    private UUID locationId;

    @Schema(description = "Assigned practitioner / Account manager employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Client status")
    private ClientStatus status;

    @Schema(description = "Internal practitioner notes")
    private String notes;

    public String getPrimaryPhone() {
        return phone;
    }

    public void setPrimaryPhone(String primaryPhone) {
        if (primaryPhone != null) {
            this.phone = primaryPhone;
        }
    }

    public String getAlternatePhone() {
        return altPhone;
    }

    public void setAlternatePhone(String alternatePhone) {
        if (alternatePhone != null) {
            this.altPhone = alternatePhone;
        }
    }

    public String getPostalCode() {
        return pincode;
    }

    public void setPostalCode(String postalCode) {
        if (postalCode != null) {
            this.pincode = postalCode;
        }
    }
}
