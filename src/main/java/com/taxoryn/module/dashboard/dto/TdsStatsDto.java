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
@Schema(description = "Organization TDS Compliance Statistics")
public class TdsStatsDto {

    @Schema(description = "Total number of clients with TAN profiles", example = "45")
    private long totalTdsClients;

    @Schema(description = "Number of quarterly TDS returns pending processing/filing", example = "12")
    private long pending;

    @Schema(description = "Number of quarterly TDS returns filed", example = "35")
    private long filed;

    @Schema(description = "Number of quarterly TDS returns overdue", example = "3")
    private long overdue;
}
