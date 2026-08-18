package com.taxoryn.core.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.time.Instant;

/**
 * Standard API Response envelope for all REST endpoints.
 *
 * @param <T> Payload type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Universal API Response Envelope")
public class ApiResponse<T> {

    @Schema(description = "Indicates whether the request was successful", example = "true")
    private boolean success;

    @Schema(description = "HTTP Status code", example = "200")
    private int status;

    @Schema(description = "Human-readable message describing the outcome", example = "Operation completed successfully")
    private String message;

    @Schema(description = "Response payload")
    private T data;

    @Schema(description = "UTC timestamp when the response was generated")
    @Builder.Default
    private Instant timestamp = Instant.now();

    @Schema(description = "Unique trace/correlation identifier for request tracking")
    private String traceId;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(200)
                .message("Success")
                .data(data)
                .timestamp(Instant.now())
                .traceId(MDC.get("traceId"))
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(200)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .traceId(MDC.get("traceId"))
                .build();
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(201)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .traceId(MDC.get("traceId"))
                .build();
    }

    public static <T> ApiResponse<T> error(int status, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .status(status)
                .message(message)
                .timestamp(Instant.now())
                .traceId(MDC.get("traceId"))
                .build();
    }
}
