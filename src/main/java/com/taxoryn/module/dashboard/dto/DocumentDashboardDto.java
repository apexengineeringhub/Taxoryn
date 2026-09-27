package com.taxoryn.module.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Document & Request Operational Metrics")
public class DocumentDashboardDto {

    @Schema(description = "Total documents uploaded/managed", example = "350")
    @Builder.Default
    private long totalDocuments = 0;

    @Schema(description = "Documents linked to active workflows or obligations", example = "120")
    @Builder.Default
    private long activeWorkflowDocuments = 0;

    @Schema(description = "Total document requests created", example = "45")
    @Builder.Default
    private long totalRequests = 0;

    @Schema(description = "Pending document requests awaiting client upload", example = "15")
    @Builder.Default
    private long pendingRequests = 0;

    @Schema(description = "Fulfilled/Completed document requests", example = "28")
    @Builder.Default
    private long fulfilledRequests = 0;

    @Schema(description = "Overdue document requests past due date", example = "2")
    @Builder.Default
    private long overdueRequests = 0;

    @Schema(description = "Timestamp when this summary was generated")
    @Builder.Default
    private Instant generatedAt = Instant.now();
}
