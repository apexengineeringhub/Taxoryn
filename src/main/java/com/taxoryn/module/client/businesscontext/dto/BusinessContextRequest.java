package com.taxoryn.module.client.businesscontext.dto;

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
@Schema(description = "Request to resolve composite Business Context")
public class BusinessContextRequest {

    @Schema(description = "Target client ID (optional if resolvable via child identifiers)")
    private UUID clientId;

    @Schema(description = "Target client service relationship ID (optional)")
    private UUID serviceRelationshipId;

    @Schema(description = "Target engagement ID (optional)")
    private UUID engagementId;

    @Schema(description = "Target work item / instance ID (optional)")
    private UUID workInstanceId;

    @Schema(description = "Target task ID (optional)")
    private UUID taskId;
}
