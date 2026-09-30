package com.logitech.sgfl.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StructuredLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(StructuredLoggingFilter.class);

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_CLIENT_IP = "clientIp";
    public static final String MDC_HTTP_METHOD = "httpMethod";
    public static final String MDC_URI = "uri";
    public static final String MDC_STATUS = "status";
    public static final String MDC_DURATION = "durationMs";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        String requestId = resolveRequestId(request);
        String clientIp = resolveClientIp(request);
        String method = request.getMethod();
        String uri = request.getRequestURI();

        MDC.put(MDC_REQUEST_ID, requestId);
        MDC.put(MDC_CLIENT_IP, clientIp);
        MDC.put(MDC_HTTP_METHOD, method);
        MDC.put(MDC_URI, uri);

        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            log.info("Iniciando requisição HTTP: {} {}", method, uri);
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            MDC.put(MDC_STATUS, String.valueOf(status));
            MDC.put(MDC_DURATION, String.valueOf(duration));

            log.info("Concluída requisição HTTP: {} {} com status {} em {}ms", method, uri, status, duration);

            MDC.clear();
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String reqId = request.getHeader(REQUEST_ID_HEADER);
        if (StringUtils.hasText(reqId)) {
            return reqId.trim();
        }
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (StringUtils.hasText(correlationId)) {
            return correlationId.trim();
        }
        return UUID.randomUUID().toString();
    }

    /**
     * Mesmo critério do RateLimitingFilter: só o endereço remoto da conexão. Atrás de
     * proxy confiável, o Tomcat (forward-headers-strategy=native) já o substitui pelo IP
     * real do cliente. Ler X-Forwarded-For aqui permitiria forjar o IP nos logs.
     */
    private String resolveClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
