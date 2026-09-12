package com.taxoryn.core.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Core Rate Limiting Service for Taxoryn API Protection.
 * <p>
 * <b>Architecture Note: Instance-Local Token Bucket (Pilot Deployment)</b>:
 * <ul>
 *   <li>The current implementation maintains an in-memory {@link ConcurrentHashMap} of {@link TokenBucket}s.</li>
 *   <li>For a single Render instance (pilot), this provides microsecond-level rate limiting with zero external
 *       infrastructure dependencies, zero network latency, and low memory footprint (~128 bytes per active IP).</li>
 *   <li>Buckets idle for longer than 5 minutes are evicted automatically via {@link #cleanupExpiredBuckets()}
 *       to prevent unbounded memory growth.</li>
 * </ul>
 * <p>
 * <b>Future Distributed Scaling Enhancement (Redis Roadmap)</b>:
 * <ul>
 *   <li>When horizontal scaling across multiple Render instances or Kubernetes replica pods is deployed,
 *       rate limiting should be migrated to a distributed backend (e.g. Redis via Bucket4j-redis or Lua token bucket scripts).</li>
 *   <li>This will ensure shared rate limit counters across all running cluster nodes.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitingService {

    @Value("${taxoryn.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${taxoryn.rate-limit.auth-limit:15}")
    private int authLimitPerMinute;

    @Value("${taxoryn.rate-limit.api-limit:300}")
    private int apiLimitPerMinute;

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitResult checkRateLimit(String clientIp, boolean isAuthEndpoint) {
        if (!enabled) {
            return new RateLimitResult(true, isAuthEndpoint ? authLimitPerMinute : apiLimitPerMinute, isAuthEndpoint ? authLimitPerMinute : apiLimitPerMinute, 0);
        }

        int limit = isAuthEndpoint ? authLimitPerMinute : apiLimitPerMinute;
        long windowMillis = 60_000L; // 1 minute window
        String bucketKey = (isAuthEndpoint ? "AUTH:" : "API:") + clientIp;

        if (buckets.size() > 50_000) {
            cleanupExpiredBuckets();
        }

        TokenBucket bucket = buckets.computeIfAbsent(bucketKey, k -> new TokenBucket(limit, windowMillis));
        return bucket.tryConsume(limit, windowMillis);
    }

    @Scheduled(fixedRate = 300_000) // Cleanup every 5 minutes
    public void cleanupExpiredBuckets() {
        long now = System.currentTimeMillis();
        int initialSize = buckets.size();
        buckets.entrySet().removeIf(entry -> (now - entry.getValue().getLastRefillTime()) > 300_000);
        int removed = initialSize - buckets.size();
        if (removed > 0) {
            log.debug("Cleaned up {} expired rate limiting buckets. Active buckets: {}", removed, buckets.size());
        }
    }

    public void reset() {
        buckets.clear();
    }

    @Getter
    public static class RateLimitResult {
        private final boolean allowed;
        private final int limit;
        private final int remaining;
        private final long retryAfterSeconds;

        public RateLimitResult(boolean allowed, int limit, int remaining, long retryAfterSeconds) {
            this.allowed = allowed;
            this.limit = limit;
            this.remaining = Math.max(0, remaining);
            this.retryAfterSeconds = retryAfterSeconds;
        }
    }

    private static class TokenBucket {
        private final AtomicInteger tokens;
        private final AtomicLong lastRefillTime;

        public TokenBucket(int capacity, long windowMillis) {
            this.tokens = new AtomicInteger(capacity);
            this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
        }

        public synchronized RateLimitResult tryConsume(int capacity, long windowMillis) {
            refill(capacity, windowMillis);
            int currentTokens = tokens.get();
            if (currentTokens > 0) {
                int remaining = tokens.decrementAndGet();
                return new RateLimitResult(true, capacity, remaining, 0);
            } else {
                long now = System.currentTimeMillis();
                long elapsedTime = now - lastRefillTime.get();
                long timeUntilRefill = Math.max(1, (windowMillis - elapsedTime) / 1000);
                return new RateLimitResult(false, capacity, 0, timeUntilRefill);
            }
        }

        private void refill(int capacity, long windowMillis) {
            long now = System.currentTimeMillis();
            long lastRefill = lastRefillTime.get();
            long elapsedTime = now - lastRefill;

            if (elapsedTime >= windowMillis) {
                tokens.set(capacity);
                lastRefillTime.set(now);
            }
        }

        public long getLastRefillTime() {
            return lastRefillTime.get();
        }
    }
}
