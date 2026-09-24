package com.taxoryn.module.compliance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Record Government Filing on Portal")
public class MarkWorkflowFiledRequest {

    @NotNull(message = "Filing date is required")
    @Schema(description = "Date return was submitted/filed on statutory portal", example = "2026-08-19")
    private LocalDate filedDate;

    @Schema(description = "Government acknowledgement / ARN number", example = "AA2708260192837")
    private String acknowledgementNumber;

    @Schema(description = "Optional filing remarks")
    private String filingNotes;
}
