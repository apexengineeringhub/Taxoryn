package com.taxoryn.module.compliance.calendar.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Compact Client 360 Compliance Deadline Summary")
public class ClientComplianceDeadlineSummaryDto {

    @Schema(description = "Client unique identifier")
    private UUID clientId;

    @Schema(description = "Client display name")
    private String clientDisplayName;

    @Schema(description = "Aggregate counts for client deadlines")
    private ComplianceDeadlineSummaryDto summary;

    @Schema(description = "Next closest statutory compliance deadline for this client")
    private ComplianceDeadlineDto nextDeadline;

    @Schema(description = "Upcoming compliance deadlines for this client (up to 5 items)")
    private List<ComplianceDeadlineDto> upcomingDeadlines;
}
