package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientBranchType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update an existing client branch / business location")
public class UpdateClientBranchRequest {

    @Size(max = 150, message = "Branch name must not exceed 150 characters")
    @Schema(description = "Branch name", example = "Mumbai Plant / Corporate Office")
    private String branchName;

    @Size(max = 50, message = "Branch code must not exceed 50 characters")
    @Schema(description = "Branch code", example = "MUM-01")
    private String branchCode;

    @Schema(description = "Branch type", example = "PRINCIPAL_PLACE_OF_BUSINESS")
    private ClientBranchType branchType;

    @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
    @Schema(description = "Address line 1", example = "Plot 45, MIDC Industrial Area")
    private String addressLine1;

    @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
    @Schema(description = "Address line 2", example = "Andheri East")
    private String addressLine2;

    @Size(max = 100, message = "City must not exceed 100 characters")
    @Schema(description = "City", example = "Mumbai")
    private String city;

    @Size(max = 100, message = "State must not exceed 100 characters")
    @Schema(description = "State", example = "Maharashtra")
    private String state;

    @Size(max = 10, message = "State code must not exceed 10 characters")
    @Schema(description = "2-digit state code", example = "27")
    private String stateCode;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    @Schema(description = "Country", example = "India")
    private String country;

    @Pattern(regexp = "^$|^[0-9]{6}$", message = "PIN Code must be a 6-digit number")
    @Size(max = 20, message = "PIN Code must not exceed 20 characters")
    @Schema(description = "PIN Code", example = "400093")
    private String pincode;

    @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", message = "Invalid GSTIN format")
    @Size(max = 15, message = "GSTIN must not exceed 15 characters")
    @Schema(description = "Branch GSTIN", example = "27AAAPL1234C1ZV")
    private String gstin;

    @Pattern(regexp = "^$|^[0-9+()\\- ]{7,20}$", message = "Phone must be a valid phone number format")
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    @Schema(description = "Branch phone", example = "+91 22 28394000")
    private String phone;

    @Email(message = "Valid email address is required")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Schema(description = "Branch email", example = "mumbai.plant@example.com")
    private String email;

    @Schema(description = "Whether this is the primary / principal place of business")
    private Boolean primaryBranch;

    @Schema(description = "Active status")
    private Boolean active;

    @Schema(description = "Additional notes")
    private String notes;
}
