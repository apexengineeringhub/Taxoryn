package com.taxoryn.module.gov.reliability.policy;

import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.reliability.model.GovFailureClassification;
import lombok.Builder;
import lombok.Getter;

import java.util.Random;

/**
 * Centralized retry policy with exponential backoff and jitter for government operations.
 * Enforces that only transient failures are retried and bounded by max attempts.
 */
@Getter
@Builder
public class GovRetryPolicy {

    @Builder.Default
    private int maxAttempts = 3;

    @Builder.Default
    private long initialBackoffMillis = 1000L;

    @Builder.Default
    private double backoffMultiplier = 2.0;

    @Builder.Default
    private long maxBackoffMillis = 30000L;

    @Builder.Default
    private double jitterFactor = 0.2;

    private static final Random RANDOM = new Random();

    public static GovRetryPolicy defaultPolicy() {
        return GovRetryPolicy.builder()
                .maxAttempts(3)
                .initialBackoffMillis(1000L)
                .backoffMultiplier(2.0)
                .maxBackoffMillis(30000L)
                .jitterFactor(0.2)
                .build();
    }

    public static GovRetryPolicy immediatePolicy() {
        return GovRetryPolicy.builder()
                .maxAttempts(3)
                .initialBackoffMillis(0L)
                .backoffMultiplier(1.0)
                .maxBackoffMillis(0L)
                .jitterFactor(0.0)
                .build();
    }

    public static GovRetryPolicy conservativePolicy() {
        return GovRetryPolicy.builder()
                .maxAttempts(2)
                .initialBackoffMillis(2000L)
                .backoffMultiplier(2.0)
                .maxBackoffMillis(10000L)
                .jitterFactor(0.1)
                .build();
    }

    /**
     * Determines whether the given error code and attempt number qualify for retry.
     */
    public boolean shouldRetry(GovErrorCode errorCode, int currentAttempt) {
        if (currentAttempt >= maxAttempts) {
            return false;
        }
        if (errorCode == null) {
            return false;
        }
        return errorCode.isTransient();
    }

    /**
     * Determines whether an HTTP status code qualifies for retry.
     */
    public boolean shouldRetryHttp(int httpStatusCode, int currentAttempt) {
        if (currentAttempt >= maxAttempts) {
            return false;
        }
        return switch (httpStatusCode) {
            case 408, 429, 502, 503, 504 -> true;
            case 400, 401, 403, 404, 422 -> false;
            default -> httpStatusCode >= 500;
        };
    }

    /**
     * Determines whether a failure classification qualifies for retry.
     */
    public boolean shouldRetryClassification(GovFailureClassification classification, int currentAttempt) {
        if (currentAttempt >= maxAttempts) {
            return false;
        }
        return classification != null && classification.isRetryable();
    }

    /**
     * Computes the exponential backoff duration in milliseconds with jitter for a given attempt.
     */
    public long calculateBackoff(int attempt) {
        if (initialBackoffMillis <= 0 || attempt <= 0) {
            return 0L;
        }

        double rawBackoff = initialBackoffMillis * Math.pow(backoffMultiplier, Math.max(0, attempt - 1));
        long cappedBackoff = (long) Math.min(rawBackoff, maxBackoffMillis);

        if (jitterFactor <= 0.0) {
            return cappedBackoff;
        }

        // Apply symmetric jitter: cappedBackoff * (1 +/- jitterFactor * random)
        double jitterRange = cappedBackoff * jitterFactor;
        double jitterDelta = (RANDOM.nextDouble() * 2.0 - 1.0) * jitterRange;
        return Math.max(0L, (long) (cappedBackoff + jitterDelta));
    }
}
