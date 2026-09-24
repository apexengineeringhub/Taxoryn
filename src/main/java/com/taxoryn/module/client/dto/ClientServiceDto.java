package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientServiceStatus;
import com.taxoryn.module.client.entity.ClientServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Client Service Engagement details")
public class ClientServiceDto {

    @Schema(description = "Service engagement ID")
    private UUID id;

    @Schema(description = "Organization ID")
    private UUID organizationId;

    @Schema(description = "Client ID")
    private UUID clientId;

    @Schema(description = "Client Display Name")
    private String clientName;

    @Schema(description = "Service engagement type")
    private ClientServiceType serviceType;

    @Schema(description = "Service display name")
    private String serviceName;

    @Schema(description = "Service category")
    private String category;

    @Schema(description = "Lifecycle status: ACTIVE, INACTIVE, SUSPENDED, COMPLETED")
    private ClientServiceStatus status;

    @Schema(description = "Service commencement date")
    private LocalDate startDate;

    @Schema(description = "Service completion or termination date")
    private LocalDate endDate;

    @Schema(description = "Assigned lead practitioner employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Assigned lead practitioner name")
    private String assignedEmployeeName;

    @Schema(description = "Billing frequency (e.g. MONTHLY, QUARTERLY, ANNUAL, ONE_TIME)")
    private String billingFrequency;

    @Schema(description = "Operational engagement notes")
    private String notes;

    @Schema(description = "Service operational route path")
    private String routePath;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
}
