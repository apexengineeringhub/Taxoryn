package com.taxoryn.module.docrequest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Document Requests Metric Summary")
public class DocumentRequestSummaryDto {

    private long totalRequests;
    private long pendingRequests;
    private long partiallyCompletedRequests;
    private long completedRequests;
    private long overdueRequests;
}