package com.taxoryn.module.compliance.dto;

import com.taxoryn.module.compliance.entity.ComplianceRuleEntity.ComplianceFrequency;
import com.taxoryn.module.compliance.entity.ComplianceRuleEntity.ComplianceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create Custom Compliance Rule Request Payload")
public class CreateComplianceRuleRequest {

    @NotBlank(message = "Rule code is required")
    @Schema(description = "Unique rule code identifier", example = "ROC_ANNUAL_RETURN")
    private String ruleCode;

    @NotBlank(message = "Rule name is required")
    @Schema(description = "Human-readable rule name", example = "ROC Form MGT-7 Filing")
    private String name;

    @NotNull(message = "Compliance type is required")
    @Schema(description = "Compliance domain type", example = "ROC")
    private ComplianceType complianceType;

    @NotNull(message = "Frequency is required")
    @Schema(description = "Filing / Compliance frequency", example = "ANNUALLY")
    private ComplianceFrequency frequency;

    @Min(value = 1, message = "Due day must be between 1 and 31")
    @Max(value = 31, message = "Due day must be between 1 and 31")
    @Schema(description = "Day of the month the compliance is due", example = "29")
    private int dueDay;

    @Schema(description = "Month offset after period end", defaultValue = "1")
    private int dueMonthOffset;

    @Min(value = 1, message = "Fixed due month must be between 1 and 12")
    @Max(value = 12, message = "Fixed due month must be between 1 and 12")
    @Schema(description = "Fixed month of year for annual compliance (1-12)", example = "11")
    private Integer fixedDueMonth;

    @Schema(description = "Dynamic description template", example = "Filing of ROC Form MGT-7 for Financial Year {period}")
    private String descriptionTemplate;

    @Schema(description = "Applicable client constitution types (comma-separated, e.g. PRIVATE_LIMITED,PUBLIC_LIMITED)")
    private String applicableClientTypes;
}
