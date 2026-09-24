package com.taxoryn.module.compliance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Compliance Workflow Checklist Item Details")
public class ComplianceWorkflowChecklistItemDto {

    @Schema(description = "Checklist Item ID")
    private UUID id;

    @Schema(description = "Workflow ID")
    private UUID workflowId;

    @Schema(description = "Item key identifier", example = "DOCUMENTS_REQUESTED")
    private String itemKey;

    @Schema(description = "Item title", example = "Request required source tax/accounting documents from client")
    private String title;

    @Schema(description = "Item description")
    private String description;

    @Schema(description = "Display sequence order", example = "1")
    private int sequenceOrder;

    @Schema(description = "Completion status flag", example = "true")
    private boolean isCompleted;

    @Schema(description = "Whether step is required before filing", example = "true")
    private boolean isRequired;

    @Schema(description = "Completion timestamp")
    private Instant completedAt;

    @Schema(description = "Completed by user ID")
    private UUID completedByUserId;

    @Schema(description = "Completed by user name", example = "Rahul Sharma")
    private String completedByName;

    @Schema(description = "Notes or remarks")
    private String notes;
}
