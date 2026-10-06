package com.taxoryn.module.client.businesscontext.dto;

import com.taxoryn.module.client.entity.ClientServiceStatus;
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
@Schema(description = "Level 2: Service Context Model (What service offering is engaged?)")
public class ServiceSummaryContextDto {

    @Schema(description = "Client service relationship ID")
    private UUID serviceRelationshipId;

    @Schema(description = "Underlying catalog service offering ID")
    private UUID serviceOfferingId;

    @Schema(description = "Service name")
    private String serviceName;

    @Schema(description = "Service code")
    private String serviceCode;

    @Schema(description = "Service category")
    private String serviceCategory;

    @Schema(description = "Relationship status")
    private ClientServiceStatus status;

    @Schema(description = "Billing frequency")
    private String billingFrequency;

    @Schema(description = "Custom agreed billing rate")
    private BigDecimal rate;

    @Schema(description = "Service commencement date")
    private LocalDate startDate;

    @Schema(description = "Service termination date")
    private LocalDate endDate;

    @Schema(description = "Whether the service relationship is currently ACTIVE")
    private boolean active;
}
