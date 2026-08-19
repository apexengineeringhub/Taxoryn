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
@Schema(description = "Organization ITR Statistics")
public class ItrStatsDto {

    @Schema(description = "Total number of clients with ITR profiles", example = "95")
    private long totalItrClients;

    @Schema(description = "Number of ITR returns pending processing/filing", example = "20")
    private long pending;

    @Schema(description = "Number of ITR returns filed/completed", example = "75")
    private long filed;

    @Schema(description = "Number of ITR returns overdue", example = "5")
    private long overdue;
}
