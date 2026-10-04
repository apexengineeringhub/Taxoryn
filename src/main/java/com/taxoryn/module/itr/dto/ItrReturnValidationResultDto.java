package com.taxoryn.module.itr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Structured validation outcome for an ITR return preparation payload")
public class ItrReturnValidationResultDto {

    @Schema(description = "Indicates whether the return payload passed all structural and arithmetic checks", example = "true")
    private boolean valid;

    @Schema(description = "List of blocking validation error messages")
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Schema(description = "List of non-blocking advisory warnings")
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    @Schema(description = "Timestamp when the validation rules were executed")
    private Instant validatedAt;

    public static ItrReturnValidationResultDto success(List<String> warnings) {
        return ItrReturnValidationResultDto.builder()
                .valid(true)
                .errors(Collections.emptyList())
                .warnings(warnings != null ? warnings : Collections.emptyList())
                .validatedAt(Instant.now())
                .build();
    }

    public static ItrReturnValidationResultDto failure(List<String> errors, List<String> warnings) {
        return ItrReturnValidationResultDto.builder()
                .valid(false)
                .errors(errors != null ? errors : Collections.emptyList())
                .warnings(warnings != null ? warnings : Collections.emptyList())
                .validatedAt(Instant.now())
                .build();
    }
}
