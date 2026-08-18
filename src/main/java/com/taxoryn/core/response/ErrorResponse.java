package com.taxoryn.core.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Universal Error Response Body")
public class ErrorResponse {

    @Schema(description = "Success flag - always false for error responses", example = "false")
    @Builder.Default
    private boolean success = false;

    @Schema(description = "HTTP Status code", example = "400")
    private int status;

    @Schema(description = "Machine-readable domain error code", example = "VALIDATION_FAILED")
    private String errorCode;

    @Schema(description = "Human-readable summary error message", example = "Invalid input payload")
    private String message;

    @Schema(description = "Optional list of specific field validation errors")
    private List<ValidationError> validationErrors;

    @Schema(description = "Requested endpoint path", example = "/api/v1/auth/login")
    private String path;

    @Schema(description = "UTC timestamp when error occurred")
    @Builder.Default
    private Instant timestamp = Instant.now();

    @Schema(description = "Correlation / Trace ID for diagnosing server logs")
    private String traceId;
}
