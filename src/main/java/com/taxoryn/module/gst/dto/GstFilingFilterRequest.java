package com.taxoryn.module.gst.dto;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstReturnType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "GST Return Filing Filter Parameters")
public class GstFilingFilterRequest extends PageRequestDto {

    @Schema(description = "Filter by GST Profile ID")
    private UUID gstProfileId;

    @Schema(description = "Filter by Client ID")
    private UUID clientId;

    @Schema(description = "Filter by Return Type (GSTR1, GSTR3B, GSTR9, CMP08, etc.)")
    private GstReturnType returnType;

    @Schema(description = "Filter by Return Period (e.g. 2026-08)")
    private String returnPeriod;

    @Schema(description = "Filter by Financial Year (e.g. 2026-27)")
    private String financialYear;

    @Schema(description = "Filter by Filing Status (PENDING, PREPARED, FILED, OVERDUE)")
    private GstFilingStatus filingStatus;

    @Schema(description = "Filter by assigned practitioner employee ID")
    private UUID assignedEmployeeId;
}
