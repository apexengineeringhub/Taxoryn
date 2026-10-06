package com.taxoryn.module.client.businesscontext.dto;

import com.taxoryn.module.client.entity.ClientBranchType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Primary branch summary in business context")
public class BranchSummaryContextDto {

    @Schema(description = "Branch ID")
    private UUID id;

    @Schema(description = "Branch / Location name")
    private String branchName;

    @Schema(description = "Branch type")
    private ClientBranchType branchType;

    @Schema(description = "GSTIN registered for this branch")
    private String gstin;

    @Schema(description = "City")
    private String city;

    @Schema(description = "State")
    private String state;

    @Schema(description = "State Code")
    private String stateCode;
}
