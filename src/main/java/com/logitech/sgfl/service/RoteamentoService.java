package com.logitech.sgfl.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logitech.sgfl.dto.PontoRota;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Roteamento por provedor externo, com dois formatos suportados:
 *
 * <ul>
 *   <li>{@code OSRM} (padrão, sem chave): rota real nas estradas pelo
 *       formato {@code GET /route/v1/driving/{lon},{lat};{lon},{lat}}.
 *       Instâncias OSRM próprias também servem via {@code SGFL_ROUTER_URL}.</li>
 *   <li>{@code TOMTOM} (requer {@code SGFL_ROUTER_API_KEY}, chave gratuita):
 *       rota real <strong>com trânsito em tempo real</strong> — a duração
 *       vem do provedor já considerando o trânsito no momento da chamada.</li>
 * </ul>
 *
 * Configuração: {@code SGFL_ROUTER_URL} vazio desativa o provedor e o
 * sistema passa a calcular tudo por Haversine. Toda falha vira vazio e o
 * chamador usa o cálculo em linha reta — a estimativa nunca quebra por
 * causa do provedor.
 *
 * A resposta carrega a geometria da rota (polyline do OSRM decodificada
 * ou os pontos do TomTom) para o mapa desenhar a linha real.
 */
@Service
public class RoteamentoService {

    private static final Logger log =
            LoggerFactory.getLogger(RoteamentoService.class);

    private static final Duration TIMEOUT = Duration.ofSeconds(4);

    static final String PROVEDOR_OSRM = "OSRM";
    static final String PROVEDOR_TOMTOM = "TOMTOM";
    static final String URL_PADRAO_OSRM = "https://router.project-osrm.org";
    static final String URL_PADRAO_TOMTOM =
            "https://api.tomtom.com/routing/1/calculateRoute";

    private final ObjectMapper objectMapper;
    private final boolean tomTom;
    private final String baseUrl;
    private final String chave;
    private final HttpClient http;

    public RoteamentoService(
            ObjectMapper objectMapper,
            @Value("${SGFL_ROUTER_PROVIDER:OSRM}") String provedor,
            @Value("${SGFL_ROUTER_URL:" + URL_PADRAO_OSRM + "}") String baseUrl,
            @Value("${SGFL_ROUTER_API_KEY:}") String chave
    ) {
        this.objectMapper = objectMapper;

        String provedorNormalizado = provedor == null ? "" : provedor.trim().toUpperCase();
        this.tomTom = PROVEDOR_TOMTOM.equals(provedorNormalizado);
        if (!tomTom && !PROVEDOR_OSRM.equals(provedorNormalizado) && !provedorNormalizado.isEmpty()) {
            log.warn(
                    "SGFL_ROUTER_PROVIDER desconhecido: '{}'. Usando OSRM.",
                    provedor
            );
        }

        String url = baseUrl == null ? "" : baseUrl.trim();
        if (tomTom && (url.isBlank() || URL_PADRAO_OSRM.equals(url))) {
            url = URL_PADRAO_TOMTOM;
        }
        this.baseUrl = url;
        this.chave = chave == null ? "" : chave.trim();

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

        if (tomTom && chave.isBlank()) {
            log.debug(
                    "TomTom selecionado sem SGFL_ROUTER_API_KEY: usando Haversine."
            );
            return Optional.empty();
        }

        String url = tomTom
                ? urlTomTom(latitudeOrigem, longitudeOrigem, latitudeDestino, longitudeDestino)
                : urlOsrm(latitudeOrigem, longitudeOrigem, latitudeDestino, longitudeDestino);

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

            return tomTom
                    ? interpretarTomTom(resposta.body())
                    : interpretarOsrm(resposta.body());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (IOException e) {
            log.debug("Provedor de rota indisponível: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * A API do OSRM separa os pares de coordenadas com ponto e vírgula:
     * /route/v1/driving/{lon},{lat};{lon},{lat}
     * {@code overview=simplified} devolve a forma real (polyline) da rota.
     */
    private String urlOsrm(double latOrigem, double lonOrigem,
                           double latDestino, double lonDestino) {
        return baseUrl.replaceAll("/+$", "")
                + "/route/v1/driving/"
                + lonOrigem + "," + latOrigem + ";"
                + lonDestino + "," + latDestino
                + "?overview=simplified&alternatives=false&steps=false";
    }

    /**
     * Formato do TomTom Routing API:
     * /calculateRoute/{lat},{lon}:{lat},{lon}/json?key=...
     * {@code traffic=true} torna a duração baseada no trânsito real.
     */
    private String urlTomTom(double latOrigem, double lonOrigem,
                             double latDestino, double lonDestino) {
        return baseUrl.replaceAll("/+$", "")
                + "/" + latOrigem + "," + lonOrigem
                + ":" + latDestino + "," + lonDestino
                + "/json?key=" + chave
                + "&traffic=true&routeType=fastest";
    }

    private Optional<RotaCalculada> interpretarOsrm(String corpo) {

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
                            RotaCalculada.FONTE_OSRM,
                            geometriaOsrm(rota.path("geometry"))
                    )
            );

        } catch (JsonProcessingException e) {
            log.debug("Resposta inválida do provedor de rota: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<RotaCalculada> interpretarTomTom(String corpo) {

        try {
            JsonNode raiz = objectMapper.readTree(corpo);

            JsonNode resumo = raiz.path("routes").path(0).path("summary");

            double metros = resumo.path("lengthInMeters").asDouble();
            double segundos = resumo.path("travelTimeInSeconds").asDouble();

            if (metros <= 0 || segundos <= 0) {
                return Optional.empty();
            }

            return Optional.of(
                    new RotaCalculada(
                            metros / 1000.0,
                            (int) Math.max(1, Math.ceil(segundos / 60.0)),
                            RotaCalculada.FONTE_TOMTOM,
                            geometriaTomTom(raiz.path("routes").path(0))
                    )
            );

        } catch (JsonProcessingException e) {
            log.debug("Resposta inválida do TomTom: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * O OSRM devolve a geometria como polyline codificada (precisão 5).
     * Algumas instâncias devolvem GeoJSON ({@code coordinates}); os dois
     * formatos são aceitos. Qualquer problema vira lista vazia — a linha
     * reta do mapa é um plano B aceitável.
     */
    private List<PontoRota> geometriaOsrm(JsonNode geometria) {

        if (geometria.isTextual()) {
            return decodificarPolyline(geometria.asText());
        }

        if (geometria.isArray()) {
            List<PontoRota> pontos = new ArrayList<>();
            for (JsonNode par : geometria) {
                if (par.isArray() && par.size() >= 2) {
                    pontos.add(new PontoRota(
                            par.path(1).asDouble(),
                            par.path(0).asDouble()
                    ));
                }
            }
            return pontos;
        }

        return List.of();
    }

    private List<PontoRota> geometriaTomTom(JsonNode rota) {

        List<PontoRota> pontos = new ArrayList<>();

        for (JsonNode trecho : rota.path("legs")) {
            for (JsonNode ponto : trecho.path("points")) {
                double lat = ponto.path("latitude").asDouble();
                double lon = ponto.path("longitude").asDouble();
                if (lat != 0 || lon != 0) {
                    pontos.add(new PontoRota(lat, lon));
                }
            }
        }

        return pontos;
    }

    /**
     * Decodifica a polyline do Google/OSRM: cada caractere carrega 5 bits
     * (offset 63); o delta é um zigzag de latitude/longitude em 1e-5 grau.
     */
    List<PontoRota> decodificarPolyline(String codigo) {

        if (codigo == null || codigo.isEmpty()) {
            return List.of();
        }

        List<PontoRota> pontos = new ArrayList<>();
        int indice = 0;
        int latitude = 0;
        int longitude = 0;

        try {
            while (indice < codigo.length()) {
                int resultadoLat = 0;
                int deslocamento = 0;
                int byteAtual;
                do {
                    byteAtual = codigo.charAt(indice++) - 63;
                    resultadoLat |= (byteAtual & 0x1f) << deslocamento;
                    deslocamento += 5;
                } while (byteAtual >= 0x20 && indice < codigo.length());
                latitude += (resultadoLat & 1) != 0
                        ? ~(resultadoLat >> 1)
                        : (resultadoLat >> 1);

                if (indice >= codigo.length()) {
                    break;
                }

                int resultadoLon = 0;
                deslocamento = 0;
                do {
                    byteAtual = codigo.charAt(indice++) - 63;
                    resultadoLon |= (byteAtual & 0x1f) << deslocamento;
                    deslocamento += 5;
                } while (byteAtual >= 0x20 && indice < codigo.length());
                longitude += (resultadoLon & 1) != 0
                        ? ~(resultadoLon >> 1)
                        : (resultadoLon >> 1);

                double lat = latitude / 1e5;
                double lon = longitude / 1e5;

                if (lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180) {
                    pontos.add(new PontoRota(lat, lon));
                }
            }
        } catch (RuntimeException e) {
            log.debug("Polyline mal formada: {}", e.getMessage());
            return List.of();
        }

        return pontos;
    }
}
