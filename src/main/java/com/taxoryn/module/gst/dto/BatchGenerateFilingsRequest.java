package com.taxoryn.module.gst.dto;

import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstReturnType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Batch Generate GST Filings for all Active Practice Clients")
public class BatchGenerateFilingsRequest {

    @NotBlank(message = "Return period is required")
    @Schema(description = "Target period (e.g. 2026-08)", example = "2026-08")
    private String returnPeriod;

    @NotBlank(message = "Financial year is required")
    @Schema(description = "Financial Year (e.g. 2026-27)", example = "2026-27")
    private String financialYear;

    @NotNull(message = "Return types list is required")
    @Schema(description = "Return types to generate (e.g. [GSTR1, GSTR3B])")
    private List<GstReturnType> returnTypes;

    @Schema(description = "GSTR-1 Due Date", example = "2026-09-11")
    private LocalDate gstr1DueDate;

    @Schema(description = "GSTR-3B Due Date", example = "2026-09-20")
    private LocalDate gstr3bDueDate;

    @Schema(description = "CMP-08 Due Date", example = "2026-10-18")
    private LocalDate cmp08DueDate;
}
