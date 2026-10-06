package com.taxoryn.module.gov.observability.util;

import com.taxoryn.module.gov.model.GovProviderType;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Utility for managing and propagating distributed correlation and tracing context across SLF4J MDC
 * for asynchronous outbox workers, scheduled reconciliation, and synchronous requests.
 */
public final class GovCorrelationContext {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String TENANT_ID_MDC_KEY = "tenantId";
    public static final String OPERATION_TYPE_MDC_KEY = "operationType";
    public static final String PROVIDER_TYPE_MDC_KEY = "providerType";

    private GovCorrelationContext() {
        // Utility class
    }

    public static String getOrCreateCorrelationId(String correlationId) {
        if (correlationId != null && !correlationId.trim().isEmpty()) {
            return correlationId.trim();
        }
        String mdcCorrelationId = MDC.get(CORRELATION_ID_MDC_KEY);
        if (mdcCorrelationId != null && !mdcCorrelationId.trim().isEmpty()) {
            return mdcCorrelationId;
        }
        return UUID.randomUUID().toString();
    }

    public static void setContext(String correlationId, UUID tenantId, String operationType, GovProviderType providerType) {
        if (correlationId != null) {
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        }
        if (tenantId != null) {
            MDC.put(TENANT_ID_MDC_KEY, tenantId.toString());
        }
        if (operationType != null) {
            MDC.put(OPERATION_TYPE_MDC_KEY, operationType);
        }
        if (providerType != null) {
            MDC.put(PROVIDER_TYPE_MDC_KEY, providerType.name());
        }
    }

    public static void clearContext() {
        MDC.remove(CORRELATION_ID_MDC_KEY);
        MDC.remove(TENANT_ID_MDC_KEY);
        MDC.remove(OPERATION_TYPE_MDC_KEY);
        MDC.remove(PROVIDER_TYPE_MDC_KEY);
    }

    public static <T> T executeWithContext(String correlationId, UUID tenantId, String operationType, GovProviderType providerType, Supplier<T> supplier) {
        String prevCorrelationId = MDC.get(CORRELATION_ID_MDC_KEY);
        String prevTenantId = MDC.get(TENANT_ID_MDC_KEY);
        String prevOpType = MDC.get(OPERATION_TYPE_MDC_KEY);
        String prevProvType = MDC.get(PROVIDER_TYPE_MDC_KEY);

        try {
            setContext(getOrCreateCorrelationId(correlationId), tenantId, operationType, providerType);
            return supplier.get();
        } finally {
            restoreContext(prevCorrelationId, prevTenantId, prevOpType, prevProvType);
        }
    }

    public static void executeWithContext(String correlationId, UUID tenantId, String operationType, GovProviderType providerType, Runnable runnable) {
        executeWithContext(correlationId, tenantId, operationType, providerType, () -> {
            runnable.run();
            return null;
        });
    }

    private static void restoreContext(String correlationId, String tenantId, String operationType, String providerType) {
        if (correlationId != null) {
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        } else {
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }

        if (tenantId != null) {
            MDC.put(TENANT_ID_MDC_KEY, tenantId);
        } else {
            MDC.remove(TENANT_ID_MDC_KEY);
        }

        if (operationType != null) {
            MDC.put(OPERATION_TYPE_MDC_KEY, operationType);
        } else {
            MDC.remove(OPERATION_TYPE_MDC_KEY);
        }

        if (providerType != null) {
            MDC.put(PROVIDER_TYPE_MDC_KEY, providerType);
        } else {
            MDC.remove(PROVIDER_TYPE_MDC_KEY);
        }
    }
}
