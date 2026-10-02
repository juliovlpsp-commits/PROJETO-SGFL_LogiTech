package com.logitech.sgfl.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class RedisRateLimiterIntegrationTest {
    @Container
    static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Test
    void compartilhaTokensEntreInstanciasDoRateLimiter() {
        LettuceConnectionFactory connectionFactory =
                new LettuceConnectionFactory(redisContainer.getHost(), redisContainer.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        try {
            StringRedisTemplate redis = new StringRedisTemplate(connectionFactory);
            redis.afterPropertiesSet();
            RedisRateLimiter primeiraInstancia = new RedisRateLimiter(redis);
            RedisRateLimiter segundaInstancia = new RedisRateLimiter(redis);

            assertThat(primeiraInstancia.tryConsume("auth", "192.168.1.10", 2, 2).allowed()).isTrue();
            assertThat(primeiraInstancia.tryConsume("auth", "192.168.1.10", 2, 2).allowed()).isTrue();
            RateLimiter.RateLimitDecision bloqueada =
                    segundaInstancia.tryConsume("auth", "192.168.1.10", 2, 2);

            assertThat(bloqueada.allowed()).isFalse();
            assertThat(bloqueada.remainingTokens()).isZero();
            assertThat(bloqueada.retryAfterMillis()).isPositive();
        } finally {
            connectionFactory.destroy();
        }
    }
}
