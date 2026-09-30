package com.logitech.sgfl.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Order(-100) // Executa logo após o StructuredLoggingFilter
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final ObjectMapper objectMapper;

    @Value("${ratelimit.enabled:true}")
    private boolean enabled;

    @Value("${ratelimit.auth.capacity:15}")
    private long authCapacity;

    @Value("${ratelimit.auth.tokens-per-minute:15}")
    private long authTokensPerMinute;

    @Value("${ratelimit.api.capacity:120}")
    private long apiCapacity;

    @Value("${ratelimit.api.tokens-per-minute:120}")
    private long apiTokensPerMinute;

    /** Buckets sem uso por este tempo são descartados, para o mapa não crescer indefinidamente. */
    private static final long IDLE_EVICTION_NANOS = TimeUnit.MINUTES.toNanos(10);
    private static final long CLEANUP_INTERVAL_NANOS = TimeUnit.MINUTES.toNanos(1);

    private final Map<String, BucketEntry> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, BucketEntry> apiBuckets = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupNanos = new AtomicLong(System.nanoTime());

    private static final class BucketEntry {
        private final Bucket bucket;
        private volatile long lastAccessNanos;

        private BucketEntry(Bucket bucket) {
            this.bucket = bucket;
            this.lastAccessNanos = System.nanoTime();
        }
    }

    public RateLimitingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        evictIdleBucketsIfNeeded();

        String clientIp = resolveClientIp(request);
        BucketEntry entry;

        if (path.startsWith("/api/auth/")) {
            entry = authBuckets.computeIfAbsent(clientIp, k -> new BucketEntry(createBucket(authCapacity, authTokensPerMinute)));
        } else {
            entry = apiBuckets.computeIfAbsent(clientIp, k -> new BucketEntry(createBucket(apiCapacity, apiTokensPerMinute)));
        }

        entry.lastAccessNanos = System.nanoTime();
        Bucket bucket = entry.bucket;

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long waitForRefillNanos = probe.getNanosToWaitForRefill();
            long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(waitForRefillNanos));

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setHeader("X-Rate-Limit-Remaining", "0");

            log.warn("Rate limit excedido para IP {} na rota {}. Tente novamente em {}s", clientIp, path, retryAfterSeconds);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("timestamp", LocalDateTime.now().toString());
            body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
            body.put("error", "Too Many Requests");
            body.put("message", "Limite de requisições excedido. Tente novamente em " + retryAfterSeconds + " segundo(s).");

            String requestId = MDC.get("requestId");
            if (requestId != null) {
                body.put("requestId", requestId);
            }

            response.getWriter().write(objectMapper.writeValueAsString(body));
        }
    }

    private Bucket createBucket(long capacity, long tokensPerMinute) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(tokensPerMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Remove periodicamente (no máximo 1x por minuto) os buckets ociosos.
     */
    private void evictIdleBucketsIfNeeded() {
        long now = System.nanoTime();
        long last = lastCleanupNanos.get();

        if (now - last < CLEANUP_INTERVAL_NANOS || !lastCleanupNanos.compareAndSet(last, now)) {
            return;
        }

        authBuckets.values().removeIf(e -> now - e.lastAccessNanos > IDLE_EVICTION_NANOS);
        apiBuckets.values().removeIf(e -> now - e.lastAccessNanos > IDLE_EVICTION_NANOS);
    }

    /**
     * Usa SOMENTE o endereço remoto da conexão.
     *
     * Não lemos X-Forwarded-For / X-Real-IP diretamente aqui: esses cabeçalhos são
     * controlados pelo cliente e permitiriam burlar o limite trocando o valor a cada
     * requisição. Atrás de um proxy (nginx/Docker), a aplicação usa
     * server.forward-headers-strategy=native: o Tomcat só aceita X-Forwarded-For quando
     * a requisição vem de um proxy confiável (faixas de IP privadas) e então já
     * entrega o IP real do cliente em request.getRemoteAddr().
     */
    private String resolveClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
