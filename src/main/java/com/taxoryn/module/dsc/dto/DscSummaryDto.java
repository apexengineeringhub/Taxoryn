package com.taxoryn.module.dsc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated count summary of DSC Register lifecycle metrics")
public class DscSummaryDto {

    private long total;
    private long active;
    private long expiringSoon;
    private long expired;
    private long revoked;
    private long inactive;
}
