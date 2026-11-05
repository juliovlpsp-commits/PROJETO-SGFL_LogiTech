package com.logitech.sgfl.service;

import com.logitech.sgfl.me.EmailVerificationToken;
import com.logitech.sgfl.me.Usuario;
import com.logitech.sgfl.repository.EmailVerificationTokenRepository;
import com.logitech.sgfl.repository.UsuarioRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class EmailVerificationService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final EmailVerificationTokenRepository tokens;
    private final UsuarioRepository users;
    private final boolean enabled;
    private final String host, username, password, from, publicUrl;
    private final int port;
    private final boolean startTls;

    public EmailVerificationService(EmailVerificationTokenRepository tokens, UsuarioRepository users,
            @Value("${SGFL_MAIL_ENABLED:false}") boolean enabled,
            @Value("${SGFL_MAIL_HOST:}") String host, @Value("${SGFL_MAIL_PORT:587}") int port,
            @Value("${SGFL_MAIL_USERNAME:}") String username, @Value("${SGFL_MAIL_PASSWORD:}") String password,
            @Value("${SGFL_MAIL_FROM:noreply@sgfl.local}") String from,
            @Value("${SGFL_MAIL_STARTTLS:true}") boolean startTls,
            @Value("${SGFL_PUBLIC_URL:http://localhost:5173}") String publicUrl) {
        this.tokens=tokens; this.users=users; this.enabled=enabled; this.host=host; this.port=port;
        this.username=username; this.password=password; this.from=from; this.startTls=startTls; this.publicUrl=publicUrl;
    }

    @Transactional
    public void register(Usuario user) {
        user.setEmailVerified(false);
        users.save(user);
        issue(user);
    }

    @Transactional
    public void resend(String email) {
        users.findByEmail(email).filter(user -> !user.isEmailVerified()).ifPresent(this::issue);
    }

    @Transactional
    public boolean verify(String rawToken) {
        if (rawToken == null || rawToken.length() > 128) return false;
        EmailVerificationToken token = tokens.findByTokenHash(hash(rawToken)).orElse(null);
        Instant now = Instant.now();
        if (token == null || token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)) return false;
        users.findById(token.getUserId()).ifPresent(user -> user.setEmailVerified(true));
        token.setUsedAt(now);
        return true;
    }

    private void issue(Usuario user) {
        String raw = token();
        Instant now = Instant.now();
        EmailVerificationToken token = tokens.findByUserId(user.getId())
                .orElseGet(() -> new EmailVerificationToken(user.getId(), hash(raw), now.plus(24, ChronoUnit.HOURS)));
        token.setTokenHash(hash(raw));
        token.setExpiresAt(now.plus(24, ChronoUnit.HOURS));
        token.setUsedAt(null);
        token = tokens.save(token);
        if (!enabled || host == null || host.isBlank()) {
            return; // No ambiente local sem SMTP, a conta fica pendente até a configuração do servidor de e-mail.
        }
        try {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(host); sender.setPort(port); sender.setUsername(username); sender.setPassword(password);
            var props=sender.getJavaMailProperties();
            props.put("mail.smtp.auth", String.valueOf(username != null && !username.isBlank()));
            props.put("mail.smtp.starttls.enable", String.valueOf(startTls));
            props.put("mail.smtp.connectiontimeout", "10000"); props.put("mail.smtp.timeout", "10000"); props.put("mail.smtp.writetimeout", "10000");
            MimeMessage message=sender.createMimeMessage();
            message.setFrom(from); message.setRecipients(MimeMessage.RecipientType.TO, user.getEmail());
            message.setSubject("Verifique seu e-mail - SGFL", StandardCharsets.UTF_8.name());
            message.setText("Para verificar seu e-mail, acesse: " + publicUrl.replaceAll("/$", "") + "/?token=" + raw + "\n\nO link expira em 24 horas.", StandardCharsets.UTF_8.name());
            sender.send(message);
        } catch (Exception e) { throw new IllegalStateException("Não foi possível enviar e-mail de verificação", e); }
    }

    private static String token() { byte[] bytes=new byte[32]; RANDOM.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
}
