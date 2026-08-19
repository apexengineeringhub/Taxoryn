package com.taxoryn.module.compliance.dto;

import com.taxoryn.module.compliance.entity.ComplianceRuleEntity.ComplianceFrequency;
import com.taxoryn.module.compliance.entity.ComplianceRuleEntity.ComplianceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Compliance Rule Configuration Details")
public class ComplianceRuleDto {

    @Schema(description = "Rule ID")
    private UUID id;

    @Schema(description = "Organization ID (null for system rules)")
    private UUID organizationId;

    @Schema(description = "Unique rule code identifier", example = "GST_GSTR1_MONTHLY")
    private String ruleCode;

    @Schema(description = "Human-readable rule name", example = "GSTR-1 Monthly Return")
    private String name;

    @Schema(description = "Compliance domain type", example = "GST")
    private ComplianceType complianceType;

    @Schema(description = "Filing / Compliance frequency", example = "MONTHLY")
    private ComplianceFrequency frequency;

    @Schema(description = "Day of month the compliance is due", example = "11")
    private int dueDay;

    @Schema(description = "Month offset after period end", example = "1")
    private int dueMonthOffset;

    @Schema(description = "Fixed month of year for annual compliance (1-12)", example = "7")
    private Integer fixedDueMonth;

    @Schema(description = "Dynamic description template")
    private String descriptionTemplate;

    @Schema(description = "Applicable client constitution types (comma-separated)")
    private String applicableClientTypes;

    @Schema(description = "Active status flag", example = "true")
    private boolean active;

    @Schema(description = "System default rule flag", example = "true")
    private boolean systemRule;

    @Schema(description = "Created timestamp")
    private Instant createdAt;

    @Schema(description = "Updated timestamp")
    private Instant updatedAt;
}
