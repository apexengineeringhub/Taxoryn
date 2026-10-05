package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientRelationshipType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to establish a client relationship or group link")
public class CreateClientRelationshipRequest {

    @NotNull(message = "Target client ID is required")
    @Schema(description = "Target / Related client ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID targetClientId;

    @NotNull(message = "Relationship type is required")
    @Schema(description = "Relationship type", example = "SUBSIDIARY")
    @Builder.Default
    private ClientRelationshipType relationshipType = ClientRelationshipType.RELATED_ENTITY;

    @Schema(description = "Additional notes", example = "Wholly owned subsidiary incorporated FY24-25")
    private String notes;
}
