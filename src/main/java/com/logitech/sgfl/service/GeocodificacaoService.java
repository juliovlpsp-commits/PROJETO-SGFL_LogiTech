package com.logitech.sgfl.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logitech.sgfl.dto.GeocodificacaoResponse;
import com.logitech.sgfl.exceptions.RecursoNaoEncontradoException;
import com.logitech.sgfl.exceptions.RegraNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Geocodificação endereço <-> coordenada usando Nominatim (OpenStreetMap).
 *
 * O provedor não exige chave de API; por isso as chamadas respeitam
 * User-Agent, timeout curto e cache local, e toda falha vira mensagem
 * clara de regra de negócio em vez de erro interno.
 *
 * Configuração:
 * <ul>
 *   <li>{@code SGFL_GEOCODER_URL} — vazio desativa o recurso</li>
 *   <li>{@code SGFL_GEOCODER_USER_AGENT} — identificação exigida pelo Nominatim</li>
 * </ul>
 */
@Service
public class GeocodificacaoService {

    private static final Logger log =
            LoggerFactory.getLogger(GeocodificacaoService.class);

    private static final String FONTE = "NOMINATIM";
    private static final int LIMITE_CACHE = 500;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String userAgent;
    private final HttpClient http;
    private final Map<String, GeocodificacaoResponse> cache =
            new ConcurrentHashMap<>();

    public GeocodificacaoService(
            ObjectMapper objectMapper,
            @Value("${SGFL_GEOCODER_URL:https://nominatim.openstreetmap.org}") String baseUrl,
            @Value("${SGFL_GEOCODER_USER_AGENT:SGFL-LogiTech/1.0}") String userAgent
    ) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.userAgent = userAgent;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        if (desativado()) {
            log.info(
                    "Geocodificação desativada. Defina SGFL_GEOCODER_URL para ativar."
            );
        }
    }

    /**
     * Endereço -> coordenadas.
     */
    public GeocodificacaoResponse geocodificar(String endereco) {

        verificarConfigurado();

        if (endereco == null || endereco.isBlank()) {
            throw new RegraNegocioException(
                    "Informe o endereço que deseja geocodificar."
            );
        }

        String normalizado = endereco.trim();
        String chave = "d:" + normalizado.toLowerCase(Locale.ROOT);

        GeocodificacaoResponse cacheado = cache.get(chave);
        if (cacheado != null) {
            return cacheado;
        }

        JsonNode resultados = buscar(
                "/search?format=jsonv2&limit=1&q="
                        + codificar(normalizado)
        );

        if (resultados == null
                || !resultados.isArray()
                || resultados.isEmpty()) {

            throw new RecursoNaoEncontradoException(
                    "Endereço não encontrado: " + normalizado
            );
        }

        JsonNode item = resultados.get(0);

        GeocodificacaoResponse resposta = new GeocodificacaoResponse(
                paraDouble(item, "lat"),
                paraDouble(item, "lon"),
                item.path("display_name").asText(null),
                FONTE
        );

        guardar(chave, resposta);

        return resposta;
    }

    /**
     * Coordenadas -> endereço.
     */
    public GeocodificacaoResponse reverter(
            double latitude,
            double longitude
    ) {

        verificarConfigurado();
        validarCoordenada(latitude, longitude);

        String chave = "r:" + latitude + "," + longitude;

        GeocodificacaoResponse cacheado = cache.get(chave);
        if (cacheado != null) {
            return cacheado;
        }

        JsonNode resultado = buscar(
                "/reverse?format=jsonv2&zoom=18&lat="
                        + latitude + "&lon=" + longitude
        );

        if (resultado == null
                || resultado.has("error")
                || resultado.path("display_name").isMissingNode()
                || resultado.path("display_name").isNull()) {

            throw new RecursoNaoEncontradoException(
                    "Nenhum endereço encontrado para as coordenadas informadas."
            );
        }

        GeocodificacaoResponse resposta = new GeocodificacaoResponse(
                resultado.path("lat").isNumber()
                        ? resultado.path("lat").asDouble()
                        : latitude,
                resultado.path("lon").isNumber()
                        ? resultado.path("lon").asDouble()
                        : longitude,
                resultado.path("display_name").asText(null),
                FONTE
        );

        guardar(chave, resposta);

        return resposta;
    }

    private boolean desativado() {
        return baseUrl.isBlank();
    }

    private void verificarConfigurado() {
        if (desativado()) {
            throw new RegraNegocioException(
                    "Serviço de geocodificação não configurado (SGFL_GEOCODER_URL)."
            );
        }
    }

    private JsonNode buscar(String caminho) {

        String url = baseUrl.replaceAll("/+$", "") + caminho;

        try {
            HttpRequest requisicao = HttpRequest.newBuilder(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("User-Agent", userAgent)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> resposta =
                    http.send(requisicao, HttpResponse.BodyHandlers.ofString());

            if (resposta.statusCode() == 429) {
                throw new RegraNegocioException(
                        "O serviço de geocodificação recebeu muitas consultas. " +
                                "Tente novamente em instantes."
                );
            }

            if (resposta.statusCode() < 200 || resposta.statusCode() >= 300) {
                throw new RegraNegocioException(
                        "Serviço de geocodificação indisponível (HTTP " +
                                resposta.statusCode() + ")."
                );
            }

            return objectMapper.readTree(resposta.body());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw servicoIndisponivel();
        } catch (JsonProcessingException e) {
            log.debug("Resposta inválida do geocodificador: {}", e.getMessage());
            throw servicoIndisponivel();
        } catch (IOException e) {
            log.debug("Falha de rede no geocodificador: {}", e.getMessage());
            throw servicoIndisponivel();
        }
    }

    private RegraNegocioException servicoIndisponivel() {
        return new RegraNegocioException(
                "Serviço de geocodificação indisponível. Tente novamente em instantes."
        );
    }

    private void validarCoordenada(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) {
            throw new RegraNegocioException(
                    "Coordenadas geográficas inválidas."
            );
        }
    }

    private Double paraDouble(JsonNode no, String campo) {
        JsonNode valor = no.path(campo);
        if (valor.isNumber()) {
            return valor.asDouble();
        }
        try {
            return Double.parseDouble(valor.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String codificar(String valor) {
        return URLEncoder.encode(valor, java.nio.charset.StandardCharsets.UTF_8);
    }

    private void guardar(String chave, GeocodificacaoResponse resposta) {
        if (cache.size() >= LIMITE_CACHE) {
            cache.clear();
        }
        cache.put(chave, resposta);
    }
}
