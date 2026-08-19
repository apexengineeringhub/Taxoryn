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
@Schema(description = "Organization Client Statistics")
public class ClientStatsDto {

    @Schema(description = "Total number of clients", example = "150")
    private long total;

    @Schema(description = "Number of active clients", example = "140")
    private long active;

    @Schema(description = "Number of inactive/prospect/archived clients", example = "10")
    private long inactive;
}
