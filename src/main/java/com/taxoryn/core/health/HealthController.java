package com.taxoryn.core.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dedicated liveness endpoint used to keep the Render Free Web Service warm and to let
 * external uptime monitors verify that the Taxoryn backend process is up.
 * <p>
 * This is intentionally NOT backed by Spring Boot Actuator's {@code /actuator/health}:
 * Actuator's default health group aggregates indicators (e.g. the datasource indicator,
 * which runs a validation query against PostgreSQL) and can therefore be slower and
 * carries a small amount of DB load on every call. This endpoint does not touch the
 * database, does not call any external service, and does not require authentication -
 * it only confirms that the embedded servlet container and Spring context are serving
 * requests.
 * <p>
 * See {@code docs/RENDER_KEEPALIVE.md} for the full rationale and Render configuration steps.
 */
@RestController
@Tag(name = "Health", description = "Public, dependency-free liveness check for uptime monitoring and Render keep-alive")
public class HealthController {

    @GetMapping("/api/health")
    @Operation(
            summary = "Liveness check",
            description = "Returns HTTP 200 with {\"status\": \"UP\"} whenever the application process is running. " +
                    "Performs no database queries and no external calls. Public - no authentication required."
    )
    public ResponseEntity<HealthStatusResponse> health() {
        return ResponseEntity.ok(HealthStatusResponse.up());
    }
}
