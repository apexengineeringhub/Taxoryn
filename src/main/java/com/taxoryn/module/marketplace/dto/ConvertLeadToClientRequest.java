package com.taxoryn.module.marketplace.dto;

import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to convert an inbound Marketplace Lead into an Active Practice CRM Client")
public class ConvertLeadToClientRequest {

    @Builder.Default
    @Schema(description = "Client entity constitution type", example = "INDIVIDUAL")
    private ClientType clientType = ClientType.INDIVIDUAL;

    @Schema(description = "Assigned employee in the practice firm")
    private UUID assignedEmployeeId;

    @Builder.Default
    @Schema(description = "Auto-generate initial onboarding document collection task", example = "true")
    private Boolean createOnboardingTask = true;

    @Schema(description = "Initial practitioner notes")
    private String notes;
}
