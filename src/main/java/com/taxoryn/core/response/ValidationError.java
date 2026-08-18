package com.taxoryn.core.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Validation Error Detail")
public class ValidationError {

    @Schema(description = "Target field name", example = "email")
    private String field;

    @Schema(description = "Rejected input value", example = "invalid-email")
    private Object rejectedValue;

    @Schema(description = "Validation error message", example = "Email must be a valid email format")
    private String message;
}
