package com.taxoryn.module.compliance.dto;

import com.taxoryn.module.client.entity.ClientServiceType;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.compliance.model.ComplianceRecurrenceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Compliance Cycle Template Details")
public class ComplianceCycleTemplateDto {

    private UUID id;
    private UUID organizationId;
    private ClientServiceType serviceType;
    private ComplianceObligationType obligationType;
    private String name;
    private String description;
    private ComplianceRecurrenceType recurrenceType;
    private Integer defaultDueDay;
    private Integer defaultDueMonthOffset;
    private Integer fixedDueMonth;
    private Integer internalTargetOffsetDays;
    private Boolean isActive;
    private Boolean isSystem;
}
