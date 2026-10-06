package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientServiceStatus;
import com.taxoryn.module.client.entity.ClientServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Client Service Relationship & Engagement details")
public class ClientServiceDto {

    @Schema(description = "Service engagement/relationship ID")
    private UUID id;

    @Schema(description = "Organization ID")
    private UUID organizationId;

    @Schema(description = "Client ID")
    private UUID clientId;

    @Schema(description = "Client Display Name")
    private String clientName;

    @Schema(description = "Service Offering ID from Service Catalog")
    private UUID serviceOfferingId;

    @Schema(description = "Service engagement type enum")
    private ClientServiceType serviceType;

    @Schema(description = "Service code alias", example = "AUDIT_ASSURANCE")
    public String getServiceCode() {
        if (serviceCode != null) {
            return serviceCode;
        }
        return serviceType != null ? serviceType.name() : null;
    }

    @Schema(description = "Service code")
    private String serviceCode;

    @Schema(description = "Service summary notes alias")
    public String getSummary() {
        return notes;
    }

    @Schema(description = "Service display name")
    private String serviceName;

    @Schema(description = "Service category")
    private String category;

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

    @Schema(description = "Assigned lead practitioner name")
    private String assignedEmployeeName;

    @Schema(description = "Responsible user ID")
    private UUID responsibleUserId;

    @Schema(description = "Responsible user full name")
    private String responsibleUserName;

    @Schema(description = "Location ID")
    private UUID locationId;

    @Schema(description = "Location name")
    private String locationName;

    @Schema(description = "Billing frequency (e.g. MONTHLY, QUARTERLY, ANNUAL, ONE_TIME)")
    private String billingFrequency;

    @Schema(description = "Operational engagement notes")
    private String notes;

    @Schema(description = "Service operational route path")
    private String routePath;

    @Schema(description = "Timestamp of last status transition")
    private Instant statusChangedAt;

    @Schema(description = "User ID who executed the last status transition")
    private UUID statusChangedBy;

    @Schema(description = "Business reason for the status transition")
    private String statusChangeReason;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
}
