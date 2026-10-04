package com.taxoryn.module.gov.dto;

import com.taxoryn.module.gov.model.GovConnectionHealthStatus;
import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.model.GovProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Normalized result of a provider adapter handshake or health probe.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovHandshakeResult {

    private GovProviderType providerType;
    private String adapterCode;
    private GovConnectionHealthStatus healthStatus;
    private long latencyMs;
    private GovErrorCode errorCode;
    private String message;
    private Instant timestamp;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    public static GovHandshakeResult healthy(GovProviderType providerType, String adapterCode, long latencyMs, String message) {
        return GovHandshakeResult.builder()
                .providerType(providerType)
                .adapterCode(adapterCode)
                .healthStatus(GovConnectionHealthStatus.HEALTHY)
                .latencyMs(latencyMs)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    public static GovHandshakeResult authRequired(GovProviderType providerType, String adapterCode, String message) {
        return GovHandshakeResult.builder()
                .providerType(providerType)
                .adapterCode(adapterCode)
                .healthStatus(GovConnectionHealthStatus.AUTH_REQUIRED)
                .errorCode(GovErrorCode.AUTH_REQUIRED)
                .latencyMs(0)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    public static GovHandshakeResult unavailable(GovProviderType providerType, String adapterCode, String message) {
        return GovHandshakeResult.builder()
                .providerType(providerType)
                .adapterCode(adapterCode)
                .healthStatus(GovConnectionHealthStatus.UNAVAILABLE)
                .errorCode(GovErrorCode.PROVIDER_UNAVAILABLE)
                .latencyMs(0)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    public static GovHandshakeResult error(GovProviderType providerType, String adapterCode, GovErrorCode errorCode, String message) {
        return GovHandshakeResult.builder()
                .providerType(providerType)
                .adapterCode(adapterCode)
                .healthStatus(GovConnectionHealthStatus.ERROR)
                .errorCode(errorCode != null ? errorCode : GovErrorCode.UNKNOWN)
                .latencyMs(0)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
}
