package com.taxoryn.module.udin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "UDIN Register Summary Statistics")
public class UdinSummaryDto {

    private long totalCount;
    private long activeCount;
    private long verifiedCount;
    private long unverifiedCount;
    private long failedVerificationCount;
    private long cancelledCount;
}
