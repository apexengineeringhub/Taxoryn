package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientServiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request to update an existing client service engagement")
public class UpdateClientServiceRequest {

    @Schema(description = "Lifecycle status: ACTIVE, INACTIVE, SUSPENDED, COMPLETED")
    private ClientServiceStatus status;

    @Schema(description = "Service commencement date")
    private LocalDate startDate;

    @Schema(description = "Service completion or termination date")
    private LocalDate endDate;

    @Schema(description = "Assigned lead practitioner employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Billing frequency")
    private String billingFrequency;

    @Schema(description = "Operational engagement notes")
    private String notes;
}
