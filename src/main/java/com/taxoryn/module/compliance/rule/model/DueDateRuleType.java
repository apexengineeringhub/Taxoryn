package com.taxoryn.module.compliance.rule.model;

/**
 * Calculation strategy used to determine statutory due dates for a compliance rule.
 */
public enum DueDateRuleType {
    DAY_OF_FOLLOWING_MONTH("Specific day of the month following the period (e.g. 11th / 20th)"),
    DAY_OF_FOLLOWING_QUARTER_END_MONTH("Specific day of the month following the quarter end (e.g. 31st)"),
    FIXED_DATE_IN_YEAR("Fixed statutory calendar date in the year/assessment year (e.g. 31st July, 31st Oct)"),
    CUSTOM_OFFSET_DAYS("Custom offset in days following period end or trigger event"),
    SPECIFIC_DATE("Specific statutory date specified in rule definition");

    private final String description;

    DueDateRuleType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
