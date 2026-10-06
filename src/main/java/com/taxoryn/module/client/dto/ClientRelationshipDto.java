package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.entity.ClientRelationshipType;
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
@Schema(description = "Client Relationship / Group Association Details")
public class ClientRelationshipDto {

    @Schema(description = "Relationship ID")
    private UUID id;

    @Schema(description = "Owning organization ID")
    private UUID organizationId;

    @Schema(description = "Source client ID")
    private UUID sourceClientId;

    @Schema(description = "Source client display name")
    private String sourceClientDisplayName;

    @Schema(description = "Target client ID")
    private UUID targetClientId;

    @Schema(description = "Target client display name")
    private String targetClientDisplayName;

    @Schema(description = "Target client legal name")
    private String targetClientLegalName;

    @Schema(description = "Target client code")
    private String targetClientCode;

    @Schema(description = "Target client type / constitution")
    private ClientType targetClientType;

    @Schema(description = "Target client PAN")
    private String targetClientPan;

    @Schema(description = "Type of relationship (PARENT, SUBSIDIARY, GROUP_MEMBER, etc.)")
    private ClientRelationshipType relationshipType;

    @Schema(description = "Active status")
    private boolean active;

    @Schema(description = "Additional notes")
    private String notes;

    @Schema(description = "Created at timestamp")
    private Instant createdAt;

    @Schema(description = "Updated at timestamp")
    private Instant updatedAt;
}
