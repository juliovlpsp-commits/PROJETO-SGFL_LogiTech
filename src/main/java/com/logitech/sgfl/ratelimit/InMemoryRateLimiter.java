package com.logitech.sgfl.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Local mode for a single instance, including the portable H2 distribution. */
final class InMemoryRateLimiter implements RateLimiter {
    private static final long IDLE_EVICTION_NANOS = TimeUnit.MINUTES.toNanos(10);
    private static final long CLEANUP_INTERVAL_NANOS = TimeUnit.MINUTES.toNanos(1);

    private final Map<String, BucketEntry> buckets = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupNanos = new AtomicLong(System.nanoTime());

    @Override
    public RateLimitDecision tryConsume(String scope, String clientKey, long capacity, long tokensPerMinute) {
        evictIdleBucketsIfNeeded();
        String key = scope + ":" + clientKey;
        BucketEntry entry = buckets.computeIfAbsent(key, ignored -> new BucketEntry(createBucket(capacity, tokensPerMinute)));
        entry.lastAccessNanos = System.nanoTime();
        ConsumptionProbe probe = entry.bucket.tryConsumeAndReturnRemaining(1);
        return new RateLimitDecision(probe.isConsumed(), probe.getRemainingTokens(),
                probe.isConsumed() ? 0 : TimeUnit.NANOSECONDS.toMillis(probe.getNanosToWaitForRefill()));
    }

    private Bucket createBucket(long capacity, long tokensPerMinute) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(Math.max(1, capacity))
                .refillGreedy(Math.max(1, tokensPerMinute), Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private void evictIdleBucketsIfNeeded() {
        long now = System.nanoTime();
        long last = lastCleanupNanos.get();
        if (now - last < CLEANUP_INTERVAL_NANOS || !lastCleanupNanos.compareAndSet(last, now)) return;
        buckets.values().removeIf(entry -> now - entry.lastAccessNanos > IDLE_EVICTION_NANOS);
    }

    private static final class BucketEntry {
        private final Bucket bucket;
        private volatile long lastAccessNanos = System.nanoTime();
        private BucketEntry(Bucket bucket) { this.bucket = bucket; }
    }
}

@Configuration(proxyBeanMethods = false)
class RateLimiterConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "ratelimit", name = "storage", havingValue = "memory", matchIfMissing = true)
    RateLimiter inMemoryRateLimiter() {
        return new InMemoryRateLimiter();
    }
}
