package com.logitech.sgfl.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

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

    private final RateLimiter rateLimiter;

    public RateLimitingFilter(ObjectMapper objectMapper, RateLimiter rateLimiter) {
        this.objectMapper = objectMapper;
        this.rateLimiter = rateLimiter;
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

        String clientIp = resolveClientIp(request);
        String scope;
        long capacity;
        long tokensPerMinute;
        if (path.startsWith("/api/auth/")) {
            scope = "auth";
            capacity = authCapacity;
            tokensPerMinute = authTokensPerMinute;
        } else {
            scope = "api";
            capacity = apiCapacity;
            tokensPerMinute = apiTokensPerMinute;
        }

        RateLimiter.RateLimitDecision decision;
        try {
            decision = rateLimiter.tryConsume(scope, clientIp, capacity, tokensPerMinute);
        } catch (RuntimeException exception) {
            // Shared storage failures fail closed: a Redis outage must not silently
            // disable brute-force protection on every backend replica.
            log.error("Rate limiter indisponível; bloqueando requisição protegida", exception);
            escreverErro(response, HttpStatus.SERVICE_UNAVAILABLE,
                    "Proteção temporariamente indisponível. Tente novamente em instantes.", 1);
            return;
        }

        if (decision.allowed()) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(decision.remainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long retryAfterSeconds = Math.max(1, (decision.retryAfterMillis() + 999) / 1000);
            log.warn("Rate limit excedido para IP {} na rota {}. Tente novamente em {}s", clientIp, path, retryAfterSeconds);
            response.setHeader("X-Rate-Limit-Remaining", "0");
            escreverErro(response, HttpStatus.TOO_MANY_REQUESTS,
                    "Limite de requisições excedido. Tente novamente em " + retryAfterSeconds + " segundo(s).",
                    retryAfterSeconds);
        }
    }

    private void escreverErro(HttpServletResponse response, HttpStatus status, String message, long retryAfterSeconds)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        String requestId = MDC.get("requestId");
        if (requestId != null) body.put("requestId", requestId);
        response.getWriter().write(objectMapper.writeValueAsString(body));
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
