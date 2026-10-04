package com.logitech.sgfl.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários puros (sem subir o contexto do Spring) para a lógica de
 * geração/validação de JWT. Cobrem justamente a classe de bugs que apareceu
 * durante o desenvolvimento: subject do token inconsistente com o que o
 * UserDetailsService busca depois.
 */
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey",
                "chave-de-teste-com-tamanho-minimo-de-256-bits-1234567890");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3_600_000L);
    }

    @Test
    void deveGerarTokenUsandoOEmailComoSubject() {
        UserDetails usuario = new User("admin@gmail.com", "hashDaSenha", Collections.emptyList());

        String token = jwtService.generateToken(usuario);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin@gmail.com");
    }

    @Test
    void deveValidarTokenParaOMesmoUsuario() {
        UserDetails usuario = new User("admin@gmail.com", "hashDaSenha", Collections.emptyList());
        String token = jwtService.generateToken(usuario);

        assertThat(jwtService.isTokenValid(token, usuario)).isTrue();
    }

    @Test
    void naoDeveValidarTokenParaUsuarioDiferente() {
        UserDetails usuario = new User("admin@gmail.com", "hashDaSenha", Collections.emptyList());
        UserDetails outroUsuario = new User("outro@gmail.com", "outroHash", Collections.emptyList());
        String token = jwtService.generateToken(usuario);

        assertThat(jwtService.isTokenValid(token, outroUsuario)).isFalse();
    }

    @Test
    void naoDeveValidarTokenExpirado() {
        // Gera o token já expirado (data de expiração no passado), em vez de usar
        // uma expiração de poucos milissegundos + sleep: o JWT só guarda a data de
        // expiração com precisão de SEGUNDOS, então um teste baseado em timing de
        // milissegundos é instável (pode passar ou falhar dependendo da velocidade
        // da máquina que roda o teste).
        ReflectionTestUtils.setField(jwtService, "expirationMs", -5000L);
        UserDetails usuario = new User("admin@gmail.com", "hashDaSenha", Collections.emptyList());
        String token = jwtService.generateToken(usuario);

        assertThat(jwtService.isTokenValid(token, usuario)).isFalse();
    }
}
