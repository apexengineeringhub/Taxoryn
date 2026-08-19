package com.taxoryn.module.compliance.dto;

import com.taxoryn.module.compliance.entity.ComplianceRuleEntity.ComplianceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Trigger Scheduled or On-Demand Compliance Generation Payload")
public class GenerateComplianceRequest {

    @NotBlank(message = "Period is required")
    @Schema(description = "Target compliance period (e.g. 2026-08, 2026-Q2, 2026-27)", example = "2026-08")
    private String period;

    @Schema(description = "Optional filter for specific compliance types (e.g. GST, TDS, ITR)")
    private List<ComplianceType> complianceTypes;

    @Schema(description = "Optional list of client IDs to generate obligations for (if omitted, generates for all active clients)")
    private List<UUID> clientIds;
}
