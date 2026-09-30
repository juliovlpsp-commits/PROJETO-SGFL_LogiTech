package com.logitech.sgfl.config;

import com.logitech.sgfl.enums.Perfil;
import com.logitech.sgfl.me.Usuario;
import com.logitech.sgfl.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Cria o administrador inicial a partir de variáveis de ambiente.
 *
 * Regras de segurança:
 *  - desligado por padrão (BOOTSTRAP_ADMIN_ENABLED=false);
 *  - nenhuma credencial fica no código-fonte;
 *  - NUNCA altera um usuário que já existe (em especial, nunca redefine senha);
 *  - exige senha com pelo menos 8 caracteres.
 */
@Component
public class BootstrapAdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);
    static final int TAMANHO_MINIMO_SENHA = 8;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.enabled:false}")
    private boolean enabled;

    @Value("${app.bootstrap-admin.email:}")
    private String email;

    @Value("${app.bootstrap-admin.username:}")
    private String username;

    @Value("${app.bootstrap-admin.password:}")
    private String password;

    public BootstrapAdminInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            log.warn("Bootstrap do administrador habilitado, mas BOOTSTRAP_ADMIN_EMAIL e/ou "
                    + "BOOTSTRAP_ADMIN_PASSWORD não foram informados. Nenhum usuário foi criado.");
            return;
        }

        if (password.length() < TAMANHO_MINIMO_SENHA) {
            log.warn("BOOTSTRAP_ADMIN_PASSWORD tem menos de {} caracteres. Nenhum usuário foi criado.",
                    TAMANHO_MINIMO_SENHA);
            return;
        }

        String emailNormalizado = email.trim();

        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            log.info("Administrador inicial já existe ({}). Nada foi alterado.", emailNormalizado);
            return;
        }

        String usernameFinal = StringUtils.hasText(username) ? username.trim() : emailNormalizado;

        if (usuarioRepository.existsByUsername(usernameFinal)) {
            log.warn("O username '{}' já está em uso por outro usuário. Nenhum administrador foi criado.",
                    usernameFinal);
            return;
        }

        Usuario admin = new Usuario();
        admin.setEmail(emailNormalizado);
        admin.setUsername(usernameFinal);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setPerfil(Perfil.ROLE_ADMIN);
        usuarioRepository.save(admin);

        log.info("Administrador inicial criado com sucesso ({}).", emailNormalizado);
    }
}
