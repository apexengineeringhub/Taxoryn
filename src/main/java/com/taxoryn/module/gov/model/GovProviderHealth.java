package com.taxoryn.module.gov.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Health check status of a registered government integration provider adapter.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovProviderHealth {

    public enum Status {
        UP,
        DOWN,
        DEGRADED,
        UNKNOWN
    }

    private GovProviderType providerType;
    private String adapterCode;
    private Status status;
    private long latencyMs;
    private String message;
    private Instant checkedAt;

    public static GovProviderHealth up(GovProviderType providerType, String adapterCode, String message) {
        return GovProviderHealth.builder()
                .providerType(providerType)
                .adapterCode(adapterCode)
                .status(Status.UP)
                .latencyMs(0)
                .message(message)
                .checkedAt(Instant.now())
                .build();
    }

    public static GovProviderHealth down(GovProviderType providerType, String adapterCode, String message) {
        return GovProviderHealth.builder()
                .providerType(providerType)
                .adapterCode(adapterCode)
                .status(Status.DOWN)
                .latencyMs(0)
                .message(message)
                .checkedAt(Instant.now())
                .build();
    }
}
