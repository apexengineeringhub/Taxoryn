package com.taxoryn.module.tds.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of validation checks performed on a TDS return before preparation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Validation result for TDS return preparation")
public class TdsReturnValidationResultDto {

    @Schema(description = "Whether the return passed all statutory and arithmetic validations", example = "true")
    private boolean valid;

    @Schema(description = "List of critical blocking validation errors that prevent preparation")
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Schema(description = "List of non-blocking compliance warnings or advisory notices")
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    public static TdsReturnValidationResultDto success() {
        return TdsReturnValidationResultDto.builder()
                .valid(true)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();
    }

    public static TdsReturnValidationResultDto success(List<String> warnings) {
        return TdsReturnValidationResultDto.builder()
                .valid(true)
                .errors(new ArrayList<>())
                .warnings(warnings != null ? warnings : new ArrayList<>())
                .build();
    }

    public static TdsReturnValidationResultDto failure(List<String> errors, List<String> warnings) {
        return TdsReturnValidationResultDto.builder()
                .valid(false)
                .errors(errors != null ? errors : new ArrayList<>())
                .warnings(warnings != null ? warnings : new ArrayList<>())
                .build();
    }
}
