package com.logitech.sgfl.service;

import com.logitech.sgfl.enums.StatusEntrega;
import com.logitech.sgfl.me.Entrega;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Avisa por e-mail quando o status de uma entrega muda.
 *
 * Tudo é desligável por variável de ambiente: sem
 * {@code SGFL_MAIL_ENABLED=true} (ou sem host/destinatários) o serviço
 * apenas ignora os avisos, então o backend sobe igual em máquina de
 * desenvolvimento sem servidor SMTP.
 *
 * O conteúdo da mensagem é montado na hora, dentro da transação; só o
 * envio SMTP vai para a fila, para nunca travar o fluxo operacional nem
 * depender de entidade ainda ligada ao contexto de persistência.
 */
@Service
public class NotificacaoEmailService {

    private static final Logger log =
            LoggerFactory.getLogger(NotificacaoEmailService.class);

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final int TIMEOUT_MS = 10_000;

    private final boolean habilitado;
    private final String host;
    private final int porta;
    private final String usuario;
    private final String senha;
    private final String remetente;
    private final List<String> destinatarios;
    private final boolean startTls;
    private final String urlPublica;
    private final Executor executor;

    @Autowired
    public NotificacaoEmailService(
            @Value("${SGFL_MAIL_ENABLED:false}") boolean habilitado,
            @Value("${SGFL_MAIL_HOST:}") String host,
            @Value("${SGFL_MAIL_PORT:587}") int porta,
            @Value("${SGFL_MAIL_USERNAME:}") String usuario,
            @Value("${SGFL_MAIL_PASSWORD:}") String senha,
            @Value("${SGFL_MAIL_FROM:noreply@sgfl.local}") String remetente,
            @Value("${SGFL_MAIL_TO:}") String destinatarios,
            @Value("${SGFL_MAIL_STARTTLS:true}") boolean startTls,
            @Value("${SGFL_PUBLIC_URL:http://localhost:5173}") String urlPublica
    ) {
        this(
                habilitado, host, porta, usuario, senha, remetente,
                destinatarios, startTls, urlPublica,
                executorPadrao()
        );
    }

    NotificacaoEmailService(
            boolean habilitado,
            String host,
            int porta,
            String usuario,
            String senha,
            String remetente,
            String destinatarios,
            boolean startTls,
            String urlPublica,
            Executor executor
    ) {
        this.habilitado = habilitado;
        this.host = host == null ? "" : host.trim();
        this.porta = porta;
        this.usuario = usuario == null ? "" : usuario.trim();
        this.senha = senha;
        this.remetente = remetente;
        this.destinatarios = separarDestinatarios(destinatarios);
        this.startTls = startTls;
        this.urlPublica = urlPublica;
        this.executor = executor;

        if (!configurado()) {
            log.info(
                    "Avisos de status por e-mail desativados. "
                            + "Defina SGFL_MAIL_ENABLED=true, SGFL_MAIL_HOST e SGFL_MAIL_TO para ativar."
            );
        }
    }

    /**
     * Enfileira o aviso de mudança de status. Nunca lança exceção:
     * falha de e-mail não pode derrubar o fluxo de entrega.
     */
    public void notificarMudancaDeStatus(
            Entrega entrega,
            StatusEntrega anterior,
            StatusEntrega novo,
            String responsavel
    ) {

        if (!configurado()) {
            log.debug("Aviso de status ignorado: notificações por e-mail desativadas.");
            return;
        }

        /*
         * Conteúdo extraído agora: depois daqui a entidade pode estar
         * destacada do contexto e só o envio roda fora da transação.
         */
        final String codigo = entrega.getCodigoRastreio();
        final String assunto =
                "Entrega " + codigo + ": " + anterior + " -> " + novo;
        final String corpo = montarCorpo(entrega, anterior, novo, responsavel);

        executor.execute(() -> {
            try {
                enviar(assunto, corpo);
            } catch (RuntimeException e) {
                log.warn(
                        "Falha ao enviar aviso de status da entrega {}: {}",
                        codigo,
                        e.getMessage()
                );
            }
        });
    }

    boolean configurado() {
        return habilitado
                && !host.isBlank()
                && !destinatarios.isEmpty();
    }

    void enviar(String assunto, String corpo) {

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(porta);
        mailSender.setUsername(usuario);
        mailSender.setPassword(senha);

        var props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(!usuario.isBlank()));
        props.put("mail.smtp.starttls.enable", String.valueOf(startTls));
        props.put("mail.smtp.connectiontimeout", String.valueOf(TIMEOUT_MS));
        props.put("mail.smtp.timeout", String.valueOf(TIMEOUT_MS));
        props.put("mail.smtp.writetimeout", String.valueOf(TIMEOUT_MS));

        MimeMessage mensagem = mailSender.createMimeMessage();

        try {
            mensagem.setFrom(new InternetAddress(remetente));
            mensagem.setRecipients(
                    Message.RecipientType.TO,
                    destinatarios.stream()
                            .map(this::paraEndereco)
                            .toArray(InternetAddress[]::new)
            );
            mensagem.setSubject(assunto, StandardCharsets.UTF_8.name());
            mensagem.setText(corpo, StandardCharsets.UTF_8.name());
            mensagem.setHeader("X-SGFL-Mailer", "sgfl");
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Montagem do e-mail de aviso falhou: " + e.getMessage(),
                    e
            );
        }

        mailSender.send(mensagem);

        log.info("Aviso de status enviado para {}", destinatarios);
    }

    private InternetAddress paraEndereco(String endereco) {
        try {
            return new InternetAddress(endereco);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Destinatário de e-mail inválido: " + endereco,
                    e
            );
        }
    }

    private String montarCorpo(
            Entrega entrega,
            StatusEntrega anterior,
            StatusEntrega novo,
            String responsavel
    ) {

        StringBuilder corpo = new StringBuilder();

        corpo.append("Mudança de status de entrega\n");
        corpo.append("=============================\n\n");
        corpo.append("Código de rastreio: ")
                .append(entrega.getCodigoRastreio())
                .append('\n');
        corpo.append("Descrição: ")
                .append(nuloParaTraço(entrega.getDescricao()))
                .append('\n');
        corpo.append("Origem: ")
                .append(nuloParaTraço(entrega.getEnderecoOrigem()))
                .append('\n');
        corpo.append("Destino: ")
                .append(nuloParaTraço(entrega.getEnderecoDestino()))
                .append('\n');
        corpo.append("Status: ")
                .append(anterior)
                .append(" -> ")
                .append(novo)
                .append('\n');
        corpo.append("Responsável: ")
                .append(nuloParaTraço(responsavel))
                .append('\n');
        corpo.append("Data: ")
                .append(java.time.LocalDateTime.now().format(FORMATO_DATA))
                .append('\n');

        corpo.append('\n')
                .append("Acompanhe em: ")
                .append(urlPublica)
                .append("/rastreio/")
                .append(entrega.getCodigoRastreio())
                .append('\n');

        return corpo.toString();
    }

    private String nuloParaTraço(String valor) {
        return valor == null || valor.isBlank() ? "-" : valor;
    }

    private List<String> separarDestinatarios(String lista) {

        if (lista == null || lista.isBlank()) {
            return List.of();
        }

        List<String> resultado = new ArrayList<>();

        Arrays.stream(lista.split("[,;]"))
                .map(String::trim)
                .filter(destinatario -> !destinatario.isEmpty())
                .forEach(resultado::add);

        return List.copyOf(resultado);
    }

    private static Executor executorPadrao() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "sgfl-mail");
            thread.setDaemon(true);
            return thread;
        });
    }
}
