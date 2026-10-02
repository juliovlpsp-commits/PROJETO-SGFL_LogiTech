package com.logitech.sgfl.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/** Redis-backed token bucket using Redis time and one atomic Lua operation. */
@Component
@ConditionalOnProperty(prefix = "ratelimit", name = "storage", havingValue = "redis")
public class RedisRateLimiter implements RateLimiter {
    private static final DefaultRedisScript<List> TOKEN_BUCKET_SCRIPT = new DefaultRedisScript<>("""
            local capacity = tonumber(ARGV[1])
            local tokensPerMinute = tonumber(ARGV[2])
            local clock = redis.call('TIME')
            local now = tonumber(clock[1]) * 1000 + math.floor(tonumber(clock[2]) / 1000)
            local tokens = tonumber(redis.call('HGET', KEYS[1], 'tokens'))
            local updatedAt = tonumber(redis.call('HGET', KEYS[1], 'updatedAt'))
            local refillPerMs = tokensPerMinute / 60000
            if tokens == nil then
              tokens = capacity
            else
              tokens = math.min(capacity, tokens + math.max(0, now - updatedAt) * refillPerMs)
            end
            local allowed = 0
            if tokens >= 1 then
              tokens = tokens - 1
              allowed = 1
            end
            local retryAfter = 0
            if allowed == 0 then
              retryAfter = math.ceil((1 - tokens) / refillPerMs)
            end
            redis.call('HSET', KEYS[1], 'tokens', string.format('%.9f', tokens), 'updatedAt', now)
            redis.call('PEXPIRE', KEYS[1], math.max(120000, math.ceil(capacity / refillPerMs * 2)))
            return {tostring(allowed), tostring(math.floor(tokens)), tostring(retryAfter)}
            """, List.class);

    private final StringRedisTemplate redis;

    public RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public RateLimitDecision tryConsume(String scope, String clientKey, long capacity, long tokensPerMinute) {
        long safeCapacity = Math.max(1, capacity);
        long safeRate = Math.max(1, tokensPerMinute);
        String key = "sgfl:rate:v1:" + scope + ":" + sha256(clientKey);
        List<?> result = redis.execute(TOKEN_BUCKET_SCRIPT, List.of(key),
                String.valueOf(safeCapacity), String.valueOf(safeRate));
        if (result == null || result.size() != 3) {
            throw new IllegalStateException("O Redis não retornou o resultado do rate limiter.");
        }
        return new RateLimitDecision(number(result.get(0)) == 1, number(result.get(1)), number(result.get(2)));
    }

    private long number(Object value) {
        return value instanceof Number number ? number.longValue() : Long.parseLong(value.toString());
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }
}
