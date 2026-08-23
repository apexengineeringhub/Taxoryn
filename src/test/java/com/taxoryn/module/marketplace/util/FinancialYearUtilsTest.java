package com.taxoryn.module.marketplace.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinancialYearUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "2025-26",
            "2024-25",
            "2026-27",
            "2025-2026",
            "FY_2025_26",
            "FY 2025-26",
            "FY2025-26",
            "fy_2024_25",
            "fy 2025-26"
    })
    @DisplayName("Should accept valid Indian financial year formats")
    void shouldAcceptValidFinancialYears(String input) {
        assertThat(FinancialYearUtils.isValid(input)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "last year",
            "this year",
            "2025",
            "FY25",
            "2025-27",      // non-consecutive
            "2025-24",      // reverse
            "1990-91",      // too old (< 2015)
            "2050-51",      // too far in future (> 2040)
            "",
            "   ",
            "abc"
    })
    @DisplayName("Should reject invalid or arbitrary financial year strings")
    void shouldRejectInvalidFinancialYears(String input) {
        assertThat(FinancialYearUtils.isValid(input)).isFalse();
    }

    @Test
    @DisplayName("Should normalize diverse FY representations to standard YYYY-YY format")
    void shouldNormalizeFinancialYears() {
        assertThat(FinancialYearUtils.normalize("FY_2025_26")).isEqualTo("2025-26");
        assertThat(FinancialYearUtils.normalize("FY 2025-26")).isEqualTo("2025-26");
        assertThat(FinancialYearUtils.normalize("2025-2026")).isEqualTo("2025-26");
        assertThat(FinancialYearUtils.normalize("2025-26")).isEqualTo("2025-26");
        assertThat(FinancialYearUtils.normalize(null)).isNull();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when normalizing invalid format")
    void shouldThrowOnNormalizingInvalidFormat() {
        assertThatThrownBy(() -> FinancialYearUtils.normalize("invalid-fy"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Financial Year format");
    }

    @Test
    @DisplayName("Should format normalized FY to display string")
    void shouldFormatToDisplayString() {
        assertThat(FinancialYearUtils.toDisplayString("2025-26")).isEqualTo("FY 2025-26");
        assertThat(FinancialYearUtils.toDisplayString("FY 2025-26")).isEqualTo("FY 2025-26");
        assertThat(FinancialYearUtils.toDisplayString(null)).isEqualTo("");
    }

    @Test
    @DisplayName("Should provide current and selectable standard financial years")
    void shouldProvideStandardFinancialYears() {
        String currentFy = FinancialYearUtils.getCurrentFinancialYear();
        assertThat(currentFy).matches("^\\d{4}-\\d{2}$");

        List<String> standardYears = FinancialYearUtils.getStandardFinancialYears();
        assertThat(standardYears).isNotEmpty();
        assertThat(standardYears).contains(currentFy);
    }
}
