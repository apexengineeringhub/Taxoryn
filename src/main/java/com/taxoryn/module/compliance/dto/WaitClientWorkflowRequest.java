package com.taxoryn.module.compliance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to move Compliance Workflow to WAITING_FOR_CLIENT state")
public class WaitClientWorkflowRequest {

    @NotBlank(message = "Waiting reason is required")
    @Schema(description = "Explanation of missing information or documentation required from client", example = "Pending GSTR-2B purchase invoices for July 2026")
    private String reason;

    @Schema(description = "Expected date by which client is requested to provide info", example = "2026-08-15")
    private LocalDate expectedResponseDate;

    @Schema(description = "Optional additional notes")
    private String notes;
}
