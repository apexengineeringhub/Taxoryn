package com.taxoryn.module.billing.dto;

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
@Schema(description = "Unbilled Time Entry Details for Invoicing")
public class UnbilledTimeEntryDto {

    private UUID id;
    private UUID clientId;
    private String clientName;
    private UUID engagementId;
    private String engagementTitle;
    private UUID workItemId;
    private UUID taskId;
    private UUID userId;
    private String userName;
    private LocalDate entryDate;
    private Integer durationMinutes;
    private BigDecimal durationHours;
    private String description;
    private BigDecimal billingRate;
    private BigDecimal billableAmount;
}
