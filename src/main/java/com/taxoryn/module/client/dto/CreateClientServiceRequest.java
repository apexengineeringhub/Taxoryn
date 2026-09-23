package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new client service engagement")
public class CreateClientServiceRequest {

    @NotNull(message = "Service type is required")
    @Schema(description = "Service engagement type enum", requiredMode = Schema.RequiredMode.REQUIRED)
    private ClientServiceType serviceType;

    @Schema(description = "Service commencement date")
    private LocalDate startDate;

    @Schema(description = "Service planned end date")
    private LocalDate endDate;

    @Schema(description = "Assigned lead practitioner employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Billing frequency (e.g. MONTHLY, QUARTERLY, ANNUAL, ONE_TIME)", defaultValue = "MONTHLY")
    @Builder.Default
    private String billingFrequency = "MONTHLY";

    @Schema(description = "Operational engagement notes")
    private String notes;
}
