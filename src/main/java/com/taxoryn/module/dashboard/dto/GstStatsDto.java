package com.taxoryn.module.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Organization GST Statistics")
public class GstStatsDto {

    @Schema(description = "Total number of clients with GST profiles", example = "85")
    private long totalGstClients;

    @Schema(description = "Number of GST returns currently due", example = "15")
    private long returnsDue;

    @Schema(description = "Number of GST returns overdue", example = "3")
    private long returnsOverdue;

    @Schema(description = "Number of GST returns filed", example = "120")
    private long returnsFiled;
}
