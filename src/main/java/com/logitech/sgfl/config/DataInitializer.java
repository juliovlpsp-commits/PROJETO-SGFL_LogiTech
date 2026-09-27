package com.logitech.sgfl.config;

import com.logitech.sgfl.enums.Perfil;
import com.logitech.sgfl.me.Usuario;
import com.logitech.sgfl.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

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
            System.out.println("UTILIZADOR ADMIN COM E-MAIL REGISTRADO.");
        } catch (Exception e) {
            System.err.println("Erro ao inicializar: " + e.getMessage());
        }
    }
}