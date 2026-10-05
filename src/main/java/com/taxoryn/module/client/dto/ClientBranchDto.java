package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientBranchType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Client Branch / Business Location Details")
public class ClientBranchDto {

    @Schema(description = "Branch ID")
    private UUID id;

    @Schema(description = "Owning organization ID")
    private UUID organizationId;

    @Schema(description = "Associated client ID")
    private UUID clientId;

    @Schema(description = "Branch / Location name")
    private String branchName;

    @Schema(description = "Branch code")
    private String branchCode;

    @Schema(description = "Branch type")
    private ClientBranchType branchType;

    @Schema(description = "Address line 1")
    private String addressLine1;

    @Schema(description = "Address line 2")
    private String addressLine2;

    @Schema(description = "City")
    private String city;

    @Schema(description = "State")
    private String state;

    @Schema(description = "2-digit state code")
    private String stateCode;

    @Schema(description = "Country")
    private String country;

    @Schema(description = "PIN Code")
    private String pincode;

    @Schema(description = "Branch GSTIN (if applicable)")
    private String gstin;

    @Schema(description = "Branch contact phone")
    private String phone;

    @Schema(description = "Branch contact email")
    private String email;

    @Schema(description = "Whether this is the client's primary / principal place of business")
    private boolean primaryBranch;

    @Schema(description = "Active status")
    private boolean active;

    @Schema(description = "Additional notes")
    private String notes;

    @Schema(description = "Created at timestamp")
    private Instant createdAt;

    @Schema(description = "Updated at timestamp")
    private Instant updatedAt;
}
