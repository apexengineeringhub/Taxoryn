package com.taxoryn.module.compliance.period.model;

import com.taxoryn.module.compliance.rule.model.CompliancePeriodType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Normalized semantic representation of a compliance statutory period.
 * Computed deterministically from a canonical period key and period type.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompliancePeriod {

    private CompliancePeriodType periodType;
    private String periodKey;
    private LocalDate startDate;
    private LocalDate endDate;
    private String financialYear;
    private String assessmentYear;
    private String displayLabel;

    /**
     * Checks if this period represents Q4 of a financial year (Jan - Mar).
     */
    public boolean isQ4() {
        return periodType == CompliancePeriodType.QUARTER && periodKey != null && periodKey.endsWith("Q4");
    }

    /**
     * Checks if this period falls in the month of March.
     */
    public boolean isMarch() {
        return (periodType == CompliancePeriodType.MONTH && periodKey != null && periodKey.endsWith("-03"))
                || (endDate != null && endDate.getMonthValue() == 3);
    }
}
