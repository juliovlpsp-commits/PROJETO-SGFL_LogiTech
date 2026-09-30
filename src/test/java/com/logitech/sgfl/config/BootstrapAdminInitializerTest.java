package com.logitech.sgfl.config;

import com.logitech.sgfl.enums.Perfil;
import com.logitech.sgfl.me.Usuario;
import com.logitech.sgfl.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BootstrapAdminInitializerTest {

    private UsuarioRepository usuarioRepository;
    private PasswordEncoder passwordEncoder;
    private BootstrapAdminInitializer initializer;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        initializer = new BootstrapAdminInitializer(usuarioRepository, passwordEncoder);

        ReflectionTestUtils.setField(initializer, "enabled", true);
        ReflectionTestUtils.setField(initializer, "email", "admin@empresa.com");
        ReflectionTestUtils.setField(initializer, "username", "admin");
        ReflectionTestUtils.setField(initializer, "password", "SenhaForte123");
    }

    @Test
    void naoDeveFazerNadaQuandoDesabilitado() {
        ReflectionTestUtils.setField(initializer, "enabled", false);

        initializer.run();

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void deveCriarAdminComSenhaCriptografadaQuandoNaoExiste() {
        when(usuarioRepository.existsByEmail("admin@empresa.com")).thenReturn(false);
        when(usuarioRepository.existsByUsername("admin")).thenReturn(false);

        initializer.run();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());

        Usuario salvo = captor.getValue();
        assertThat(salvo.getEmail()).isEqualTo("admin@empresa.com");
        assertThat(salvo.getPerfil()).isEqualTo(Perfil.ROLE_ADMIN);
        assertThat(salvo.getPassword()).isNotEqualTo("SenhaForte123");
        assertThat(passwordEncoder.matches("SenhaForte123", salvo.getPassword())).isTrue();
    }

    @Test
    void nuncaDeveAlterarUmUsuarioQueJaExiste() {
        when(usuarioRepository.existsByEmail("admin@empresa.com")).thenReturn(true);

        initializer.run();

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void naoDeveCriarAdminComSenhaCurta() {
        ReflectionTestUtils.setField(initializer, "password", "123");

        initializer.run();

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void naoDeveCriarAdminSemEmailOuSenha() {
        ReflectionTestUtils.setField(initializer, "email", "");

        initializer.run();

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }
}
