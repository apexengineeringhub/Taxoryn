package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to initiate an onboarding pipeline for an accepted lead/proposal")
public class InitiateOnboardingRequest {

    @NotNull(message = "Lead ID is required")
    private UUID leadId;

    private UUID proposalId;

    @Builder.Default
    private ClientType entityType = ClientType.INDIVIDUAL;

    private UUID assignedEmployeeId;
}
