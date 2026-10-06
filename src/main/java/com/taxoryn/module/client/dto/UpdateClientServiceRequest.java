package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientServiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update an existing client service engagement")
public class UpdateClientServiceRequest {

    @Schema(description = "Lifecycle status: ACTIVE, INACTIVE, SUSPENDED, ENDED, PENDING, COMPLETED")
    private ClientServiceStatus status;

    @Schema(description = "Agreed client-specific price or retainer fee")
    private BigDecimal agreedPrice;

    @Schema(description = "Service commencement date")
    private LocalDate startDate;

    @Schema(description = "Service completion or termination date")
    private LocalDate endDate;

    @Schema(description = "Assigned lead practitioner employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Responsible user ID within practice")
    private UUID responsibleUserId;

    @Schema(description = "Associated location ID")
    private UUID locationId;

    @Schema(description = "Billing frequency")
    private String billingFrequency;

    @Schema(description = "Operational engagement notes")
    private String notes;

    @Size(max = 500, message = "Status change reason cannot exceed 500 characters")
    @Schema(description = "Business reason for the status transition or modification")
    private String reason;
}
