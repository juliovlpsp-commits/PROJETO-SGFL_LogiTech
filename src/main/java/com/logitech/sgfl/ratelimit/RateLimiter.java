package com.logitech.sgfl.ratelimit;

/** Token-bucket abstraction so rate limits can be local or shared across replicas. */
public interface RateLimiter {
    RateLimitDecision tryConsume(String scope, String clientKey, long capacity, long tokensPerMinute);

    record RateLimitDecision(boolean allowed, long remainingTokens, long retryAfterMillis) {}
}
