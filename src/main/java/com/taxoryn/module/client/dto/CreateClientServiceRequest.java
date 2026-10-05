package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request to create or assign a new client service engagement")
public class CreateClientServiceRequest {

    @Schema(description = "Service offering ID from master catalog")
    private UUID serviceOfferingId;

    @Schema(description = "Service engagement type enum (legacy/direct code alias)")
    private ClientServiceType serviceType;

    @Schema(description = "Agreed client-specific price or retainer fee")
    private BigDecimal agreedPrice;

    @Schema(description = "Service commencement date")
    private LocalDate startDate;

    @Schema(description = "Service planned end date")
    private LocalDate endDate;

    @Schema(description = "Assigned lead practitioner employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Responsible user ID within practice")
    private UUID responsibleUserId;

    @Schema(description = "Associated location ID")
    private UUID locationId;

    @Schema(description = "Billing frequency (e.g. MONTHLY, QUARTERLY, ANNUAL, ONE_TIME)", defaultValue = "MONTHLY")
    @Builder.Default
    private String billingFrequency = "MONTHLY";

    @Schema(description = "Operational engagement notes")
    private String notes;

    @Schema(description = "Reason for initial service assignment")
    private String reason;
}
