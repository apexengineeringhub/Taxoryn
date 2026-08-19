package com.taxoryn.module.gst.dto;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.module.gst.entity.GstProfileEntity.FilingFrequency;
import com.taxoryn.module.gst.entity.GstProfileEntity.GstProfileStatus;
import com.taxoryn.module.gst.entity.GstProfileEntity.GstType;
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
@Schema(description = "GST Profile Search & Filter Parameters")
public class GstProfileFilterRequest extends PageRequestDto {

    @Schema(description = "Keyword search (GSTIN, legal name, trade name, state code)")
    private String search;

    @Schema(description = "Filter by Client ID")
    private UUID clientId;

    @Schema(description = "Filter by GST Type")
    private GstType gstType;

    @Schema(description = "Filter by Filing Frequency")
    private FilingFrequency filingFrequency;

    @Schema(description = "Filter by assigned practitioner employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Filter by status")
    private GstProfileStatus status;
}
