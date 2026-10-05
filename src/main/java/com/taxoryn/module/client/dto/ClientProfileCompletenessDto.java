package com.taxoryn.module.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Lightweight, deterministic client profile completeness model.
 * Evaluates population of key business profile categories:
 * IDENTITY, CONTACT, ADDRESS, STATUTORY, BUSINESS.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Deterministic Client Profile Completeness Assessment")
public class ClientProfileCompletenessDto {

    @Schema(description = "Whether all key sections are sufficiently populated")
    private boolean complete;

    @Schema(description = "Overall completion score (0 - 100 percentage)", example = "80")
    private int completionPercentage;

    @Schema(description = "List of fully completed profile sections", example = "[\"IDENTITY\", \"CONTACT\", \"ADDRESS\", \"BUSINESS\"]")
    private List<String> completedSections;

    @Schema(description = "List of missing or incomplete profile sections", example = "[\"STATUTORY\"]")
    private List<String> missingSections;

    @Schema(description = "Detailed breakdown of each profile section's completeness status")
    private Map<String, Boolean> sectionStatus;
}
