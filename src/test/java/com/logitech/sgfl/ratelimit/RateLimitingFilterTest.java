package com.logitech.sgfl.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RateLimitingFilterTest {

    private RateLimitingFilter rateLimitingFilter;
    private FilterChain filterChain;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        rateLimitingFilter = new RateLimitingFilter(objectMapper, new InMemoryRateLimiter());
        filterChain = mock(FilterChain.class);

        ReflectionTestUtils.setField(rateLimitingFilter, "enabled", true);
        ReflectionTestUtils.setField(rateLimitingFilter, "authCapacity", 3L);
        ReflectionTestUtils.setField(rateLimitingFilter, "authTokensPerMinute", 3L);
        ReflectionTestUtils.setField(rateLimitingFilter, "apiCapacity", 5L);
        ReflectionTestUtils.setField(rateLimitingFilter, "apiTokensPerMinute", 5L);
    }

    @Test
    void devePermitirRequisicoesAbaixoDoLimite() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/entregas");
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-Rate-Limit-Remaining")).isEqualTo("4");
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void deveBloquearCom429QuandoLimiteForExcedido() throws ServletException, IOException {
        String clientIp = "192.168.1.200";

        // Consome as 3 requisições permitidas para rota de auth
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRemoteAddr(clientIp);
            MockHttpServletResponse res = new MockHttpServletResponse();
            rateLimitingFilter.doFilter(req, res, filterChain);
            assertThat(res.getStatus()).isEqualTo(200);
        }

        // A 4ª requisição deve ser bloqueada com 429
        MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/api/auth/login");
        blockedReq.setRemoteAddr(clientIp);
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(blockedReq, blockedRes, filterChain);

        assertThat(blockedRes.getStatus()).isEqualTo(429);
        assertThat(blockedRes.getHeader("Retry-After")).isNotNull();
        assertThat(blockedRes.getHeader("X-Rate-Limit-Remaining")).isEqualTo("0");
        assertThat(blockedRes.getContentAsString()).contains("Too Many Requests");
        assertThat(blockedRes.getContentAsString()).contains("Limite de requisições excedido");
        verify(filterChain, times(3)).doFilter(any(), any());
    }

    @Test
    void deveIgnorarRateLimiterSeEstiverDesativado() throws ServletException, IOException {
        ReflectionTestUtils.setField(rateLimitingFilter, "enabled", false);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/entregas");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void naoDevePermitirBurlarOLimiteForjandoXForwardedFor() throws ServletException, IOException {
        String clientIp = "192.168.1.250";

        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRemoteAddr(clientIp);
            req.addHeader("X-Forwarded-For", "10.0.0." + i);
            req.addHeader("X-Real-IP", "10.1.0." + i);
            rateLimitingFilter.doFilter(req, new MockHttpServletResponse(), filterChain);
        }

        MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/api/auth/login");
        blockedReq.setRemoteAddr(clientIp);
        blockedReq.addHeader("X-Forwarded-For", "10.0.0.99");
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(blockedReq, blockedRes, filterChain);

        assertThat(blockedRes.getStatus()).isEqualTo(429);
    }

    @Test
    void clientesDiferentesDevemTerLimitesIndependentes() throws ServletException, IOException {
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRemoteAddr("172.16.0.1");
            rateLimitingFilter.doFilter(req, new MockHttpServletResponse(), filterChain);
        }

        MockHttpServletRequest outro = new MockHttpServletRequest("POST", "/api/auth/login");
        outro.setRemoteAddr("172.16.0.2");
        MockHttpServletResponse res = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(outro, res, filterChain);

        assertThat(res.getStatus()).isEqualTo(200);
    }
}
