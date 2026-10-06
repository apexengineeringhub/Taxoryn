package com.taxoryn.module.client.businesscontext.dto;

import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
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
@Schema(description = "Level 1: Client Context Model (Who is the client?)")
public class ClientSummaryContextDto {

    @Schema(description = "Unique client identifier")
    private UUID clientId;

    @Schema(description = "Internal unique practice client code")
    private String clientCode;

    @Schema(description = "Display name")
    private String displayName;

    @Schema(description = "Legal registered name")
    private String legalName;

    @Schema(description = "Constitution / entity classification type")
    private ClientType clientType;

    @Schema(description = "Current lifecycle status")
    private ClientStatus lifecycleStatus;

    @Schema(description = "Whether the client is actively engaged")
    private boolean active;

    @Schema(description = "PAN")
    private String pan;

    @Schema(description = "Primary GSTIN")
    private String gstin;

    @Schema(description = "Primary email")
    private String email;

    @Schema(description = "Primary phone")
    private String phone;

    @Schema(description = "Assigned primary location ID")
    private UUID locationId;

    @Schema(description = "Primary contact summary")
    private ContactSummaryContextDto primaryContact;

    @Schema(description = "Primary branch summary")
    private BranchSummaryContextDto primaryBranch;

    @Schema(description = "Count of active service relationships")
    private Long activeServicesCount;

    @Schema(description = "Total contacts count")
    private Long totalContactsCount;

    @Schema(description = "Total branches count")
    private Long totalBranchesCount;
}
