package com.taxoryn.core.health;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Minimal response payload for the lightweight liveness endpoint.
 * <p>
 * Intentionally returned as a bare JSON object (not wrapped in {@link com.taxoryn.core.response.ApiResponse})
 * so that external uptime monitors and Render's health checker can rely on a small,
 * stable, dependency-free response shape.
 *
 * @param status always {@code "UP"} when the Spring Boot process is running and able to serve requests
 */
@Schema(description = "Lightweight liveness payload for /api/health")
public record HealthStatusResponse(
        @Schema(description = "Liveness status of the application", example = "UP") String status
) {

    public static HealthStatusResponse up() {
        return new HealthStatusResponse("UP");
    }
}
