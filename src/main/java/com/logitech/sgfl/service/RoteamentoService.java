package com.logitech.sgfl.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Roteamento por provedor externo, compatível com a API do OSRM.
 *
 * O provedor é plugável por URL: qualquer serviço que responda no formato
 * {@code GET /route/v1/driving/{lon},{lat},{lon},{lat}} funciona (inclui
 * instâncias OSRM próprias e serviços que agreguem trânsito, quando o
 * contrato for o mesmo). Sem chave de API.
 *
 * Configuração: {@code SGFL_ROUTER_URL} vazio desativa e o sistema passa
 * a calcular tudo por Haversine. Toda falha vira vazio e o chamador usa o
 * cálculo em linha reta — a estimativa nunca quebra por causa do provedor.
 */
@Service
public class RoteamentoService {

    private static final Logger log =
            LoggerFactory.getLogger(RoteamentoService.class);

    private static final Duration TIMEOUT = Duration.ofSeconds(4);

    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final HttpClient http;

    public RoteamentoService(
            ObjectMapper objectMapper,
            @Value("${SGFL_ROUTER_URL:https://router.project-osrm.org}") String baseUrl
    ) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    /**
     * Calcula a rota real entre dois pontos, se o provedor estiver
     * disponível. Optional vazio = usar Haversine.
     */
    public Optional<RotaCalculada> rota(
            double latitudeOrigem,
            double longitudeOrigem,
            double latitudeDestino,
            double longitudeDestino
    ) {

        if (baseUrl.isBlank()) {
            return Optional.empty();
        }

        String url = baseUrl.replaceAll("/+$", "")
                + "/route/v1/driving/"
                + longitudeOrigem + "," + latitudeOrigem + ","
                + longitudeDestino + "," + latitudeDestino
                + "?overview=false&alternatives=false&steps=false";

        try {
            HttpRequest requisicao = HttpRequest.newBuilder(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> resposta =
                    http.send(requisicao, HttpResponse.BodyHandlers.ofString());

            if (resposta.statusCode() != 200) {
                log.debug(
                        "Provedor de rota respondeu HTTP {}.",
                        resposta.statusCode()
                );
                return Optional.empty();
            }

            return interpretar(resposta.body());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (IOException e) {
            log.debug("Provedor de rota indisponível: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<RotaCalculada> interpretar(String corpo) {

        try {
            JsonNode raiz = objectMapper.readTree(corpo);

            if (!"Ok".equals(raiz.path("code").asText())) {
                return Optional.empty();
            }

            JsonNode rota = raiz.path("routes").path(0);

            double metros = rota.path("distance").asDouble();
            double segundos = rota.path("duration").asDouble();

            if (metros <= 0 || segundos <= 0) {
                return Optional.empty();
            }

            return Optional.of(
                    new RotaCalculada(
                            metros / 1000.0,
                            (int) Math.max(1, Math.ceil(segundos / 60.0)),
                            RotaCalculada.FONTE_OSRM
                    )
            );

        } catch (JsonProcessingException e) {
            log.debug("Resposta inválida do provedor de rota: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
