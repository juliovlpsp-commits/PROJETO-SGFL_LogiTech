package com.logitech.sgfl.config;

import com.logitech.sgfl.enums.Perfil;
import com.logitech.sgfl.me.Usuario;
import com.logitech.sgfl.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            Usuario admin = usuarioRepository.findByEmail("admin@gmail.com")
                    .orElseGet(Usuario::new);

            admin.setUsername("admin");
            admin.setEmail("admin@gmail.com");
            admin.setPassword(passwordEncoder.encode("123"));
            admin.setPerfil(Perfil.ROLE_ADMIN);

            usuarioRepository.save(admin);
            log.info("Utilizador administrador inicial verificado/registrado com sucesso (admin@gmail.com).");
        } catch (Exception e) {
            log.error("Erro ao inicializar dados padrão: {}", e.getMessage(), e);
        }
    }
}