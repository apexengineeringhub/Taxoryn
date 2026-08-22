package com.taxoryn.module.tds.dto;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFilingStatus;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFormType;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsQuarter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Filter parameters for searching TDS return filings")
public class TdsReturnFilterRequest extends PageRequestDto {

    @Schema(description = "Filter by Client ID")
    private UUID clientId;

    @Schema(description = "Filter by TDS Profile ID")
    private UUID tdsProfileId;

    @Schema(description = "Filter by Form Type (FORM_24Q, FORM_26Q, etc.)")
    private TdsFormType formType;

    @Schema(description = "Filter by Quarter (Q1, Q2, Q3, Q4)")
    private TdsQuarter quarter;

    @Schema(description = "Filter by Financial Year (e.g., 2026-27)")
    private String financialYear;

    @Schema(description = "Filter by Filing Status (PENDING, FILED, OVERDUE, etc.)")
    private TdsFilingStatus filingStatus;

    @Schema(description = "Filter by Assigned Employee ID")
    private UUID assignedEmployeeId;
}
