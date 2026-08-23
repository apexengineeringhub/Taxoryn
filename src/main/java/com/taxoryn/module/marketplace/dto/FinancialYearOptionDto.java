package com.taxoryn.module.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialYearOptionDto {

    private String code;         // e.g. "2025-26"
    private String label;        // e.g. "FY 2025-26"
    private boolean isCurrent;   // true if current financial year
}
