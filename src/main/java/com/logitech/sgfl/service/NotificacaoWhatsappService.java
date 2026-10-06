package com.logitech.sgfl.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logitech.sgfl.enums.StatusEntrega;
import com.logitech.sgfl.me.Entrega;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Avisa por WhatsApp quando o status de uma entrega muda.
 *
 * O envio é por endpoint HTTP configurável ({@code SGFL_WHATSAPP_API_URL})
 * no contrato JSON {@code {"to", "from", "message"}} — qualquer gateway
 * que exponha um webhook nesse formato serve (n8n, ponte própria,
 * provedores com API genérica). Token opcional via
 * {@code SGFL_WHATSAPP_TOKEN} (cabeçalho {@code Authorization: Bearer}).
 *
 * Como no e-mail, tudo é desligável por variável de ambiente: sem
 * {@code SGFL_WHATSAPP_ENABLED=true} (ou sem URL/números) o serviço
 * apenas ignora os avisos.
 *
 * Números de destino: {@code SGFL_WHATSAPP_TO} (separados por vírgula).
 * Formatos comuns são aceitos — "11 99999-8888", "+55 11 99999-8888" —
 * e viram só dígitos com DDI 55 quando brasileiros.
 *
 * O texto é montado na hora, dentro da transação; só o POST HTTP roda
 * fora, para nunca travar o fluxo operacional.
 */
@Service
public class NotificacaoWhatsappService {

    private static final Logger log =
            LoggerFactory.getLogger(NotificacaoWhatsappService.class);

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final int TIMEOUT_MS = 10_000;

    private final boolean habilitado;
    private final String apiUrl;
    private final String token;
    private final String remetente;
    private final List<String> numeros;
    private final String urlPublica;
    private final ObjectMapper objectMapper;
    private final HttpClient http;
    private final Executor executor;

    @Autowired
    public NotificacaoWhatsappService(
            @Value("${SGFL_WHATSAPP_ENABLED:false}") boolean habilitado,
            @Value("${SGFL_WHATSAPP_API_URL:}") String apiUrl,
            @Value("${SGFL_WHATSAPP_TOKEN:}") String token,
            @Value("${SGFL_WHATSAPP_FROM:SGFL}") String remetente,
            @Value("${SGFL_WHATSAPP_TO:}") String numeros,
            @Value("${SGFL_PUBLIC_URL:http://localhost:5173}") String urlPublica,
            ObjectMapper objectMapper
    ) {
        this(
                habilitado, apiUrl, token, remetente, numeros,
                urlPublica, objectMapper, executorPadrao()
        );
    }

    NotificacaoWhatsappService(
            boolean habilitado,
            String apiUrl,
            String token,
            String remetente,
            String numeros,
            String urlPublica,
            ObjectMapper objectMapper,
            Executor executor
    ) {
        this.habilitado = habilitado;
        this.apiUrl = apiUrl == null ? "" : apiUrl.trim();
        this.token = token == null ? "" : token.trim();
        this.remetente = remetente == null || remetente.isBlank() ? "SGFL" : remetente.trim();
        this.numeros = normalizarNumeros(numeros);
        this.urlPublica = urlPublica;
        this.objectMapper = objectMapper;
        this.executor = executor;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(TIMEOUT_MS))
                .build();

        if (!configurado()) {
            log.info(
                    "Avisos de status por WhatsApp desativados. "
                            + "Defina SGFL_WHATSAPP_ENABLED=true, SGFL_WHATSAPP_API_URL "
                            + "e SGFL_WHATSAPP_TO para ativar."
            );
        }
    }

    /**
     * Enfileira o aviso de mudança de status. Nunca lança exceção:
     * falha de WhatsApp não pode derrubar o fluxo de entrega.
     */
    public void notificarMudancaDeStatus(
            Entrega entrega,
            StatusEntrega anterior,
            StatusEntrega novo,
            String responsavel
    ) {

        if (!configurado()) {
            log.debug("Aviso de status ignorado: notificações por WhatsApp desativadas.");
            return;
        }

        /*
         * Conteúdo extraído agora: depois daqui a entidade pode estar
         * destacada do contexto e só o envio roda fora da transação.
         */
        final String codigo = entrega.getCodigoRastreio();
        final String texto = montarMensagem(entrega, anterior, novo, responsavel);

        executor.execute(() -> {
            for (String numero : numeros) {
                try {
                    enviar(numero, texto);
                } catch (RuntimeException e) {
                    log.warn(
                            "Falha ao enviar aviso de WhatsApp da entrega {} para {}: {}",
                            codigo,
                            numero,
                            e.getMessage()
                    );
                }
            }
        });
    }

    boolean configurado() {
        return habilitado
                && !apiUrl.isBlank()
                && !numeros.isEmpty();
    }

    void enviar(String numero, String texto) {

        HttpRequest.BodyPublisher publisher;

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("to", numero);
            payload.put("from", remetente);
            payload.put("message", texto);

            publisher = HttpRequest.BodyPublishers.ofString(
                    objectMapper.writeValueAsString(payload)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Montagem do aviso de WhatsApp falhou: " + e.getMessage(),
                    e
            );
        }

        HttpRequest.Builder requisicao = HttpRequest.newBuilder(URI.create(apiUrl))
                .timeout(Duration.ofMillis(TIMEOUT_MS))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(publisher);

        if (!token.isBlank()) {
            requisicao.header("Authorization", "Bearer " + token);
        }

        try {
            HttpResponse<String> resposta = http.send(
                    requisicao.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (resposta.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Gateway de WhatsApp respondeu HTTP " + resposta.statusCode()
                );
            }

            log.info("Aviso de status enviado por WhatsApp para {}", numero);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Gateway de WhatsApp inacessível: " + e.getMessage(),
                    e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Envio de WhatsApp interrompido.", e);
        }
    }

    private String montarMensagem(
            Entrega entrega,
            StatusEntrega anterior,
            StatusEntrega novo,
            String responsavel
    ) {

        StringBuilder texto = new StringBuilder();

        texto.append("SGFL LogiTech — entrega ")
                .append(entrega.getCodigoRastreio())
                .append('\n');
        texto.append("Status: ")
                .append(anterior)
                .append(" -> ")
                .append(novo)
                .append('\n');
        texto.append("Responsável: ")
                .append(responsavel == null || responsavel.isBlank() ? "-" : responsavel)
                .append('\n');
        texto.append("Data: ")
                .append(java.time.LocalDateTime.now().format(FORMATO_DATA))
                .append('\n');
        texto.append("Acompanhe: ")
                .append(urlPublica)
                .append("/rastreio/")
                .append(entrega.getCodigoRastreio());

        return texto.toString();
    }

    /**
     * Aceita "11 99999-8888", "+55 (11) 99999-8888" ou "5511999998888".
     * Brasileiros (10-11 dígitos) ganham o DDI 55; o resto vira dígitos.
     */
    private List<String> normalizarNumeros(String lista) {

        if (lista == null || lista.isBlank()) {
            return List.of();
        }

        List<String> resultado = new ArrayList<>();

        for (String bruto : lista.split("[,;]")) {
            String digitos = bruto.replaceAll("\\D", "");

            if (digitos.isEmpty()) {
                continue;
            }

            if (digitos.length() >= 10 && digitos.length() <= 11) {
                digitos = "55" + digitos;
            }

            resultado.add(digitos);
        }

        return List.copyOf(resultado);
    }

    private static Executor executorPadrao() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "sgfl-whatsapp");
            thread.setDaemon(true);
            return thread;
        });
    }
}
