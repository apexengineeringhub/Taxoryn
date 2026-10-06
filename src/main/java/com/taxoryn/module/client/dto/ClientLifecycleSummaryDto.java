package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Authoritative summary of client lifecycle and status governance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Client Lifecycle State & Status Summary")
public class ClientLifecycleSummaryDto {

    @Schema(description = "Client ID")
    private UUID clientId;

    @Schema(description = "Organization / practice tenant ID")
    private UUID organizationId;

    @Schema(description = "Current lifecycle status")
    private ClientStatus currentStatus;

    @Schema(description = "Timestamp when status was last transitioned")
    private Instant statusChangedAt;

    @Schema(description = "User ID who performed the last status transition")
    private UUID statusChangedBy;

    @Schema(description = "Business justification or audit reason for last status transition")
    private String statusChangeReason;

    @Schema(description = "Permitted next lifecycle transitions from current status")
    private Set<ClientStatus> allowedTransitions;

    @Schema(description = "Whether the client is fully active in practice")
    private boolean active;

    @Schema(description = "Whether the client onboarding is in progress")
    private boolean onboarding;

    @Schema(description = "Whether the client is archived")
    private boolean archived;

    @Schema(description = "Whether the client is suspended")
    private boolean suspended;

    @Schema(description = "Whether the client is inactive")
    private boolean inactive;
}
