package com.logitech.sgfl.integration;

import com.logitech.sgfl.dto.LoginRequest;
import com.logitech.sgfl.dto.LoginResponse;
import com.logitech.sgfl.enums.Perfil;
import com.logitech.sgfl.me.Usuario;
import com.logitech.sgfl.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre o fluxo completo de autenticação de ponta a ponta, com o contexto real
 * do Spring Security de pé (sem mockar filtros). É este teste que teria pegado,
 * antes de qualquer teste manual no navegador, os bugs de:
 *  - subject do JWT (username x email) inconsistente com o UserDetailsService;
 *  - rota protegida aceitando acesso sem autenticação.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AutenticacaoIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String EMAIL = "teste@exemplo.com";
    private static final String SENHA = "senha123";

    @BeforeEach
    void configurarAmbiente() {
        usuarioRepository.deleteAll();
        Usuario usuario = new Usuario();
        usuario.setUsername("teste");
        usuario.setEmail(EMAIL);
        usuario.setPassword(passwordEncoder.encode(SENHA));
        usuario.setPerfil(Perfil.ROLE_ADMIN);
        usuarioRepository.save(usuario);

        // O HttpURLConnection padrao do JDK tem um bug conhecido ao receber 401 em
        // requisicoes POST (HttpRetryException em streaming mode). Trocamos pelo
        // Apache HttpClient, que nao sofre desse problema.
        restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
    }

    @Test
    void deveRecusarAcessoARotaProtegidaSemToken() {
        ResponseEntity<String> resposta = restTemplate.getForEntity("/api/entregas", String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deveFazerLoginComEmailERetornarTokenValido() {
        LoginRequest request = new LoginRequest(EMAIL, SENHA);

        ResponseEntity<LoginResponse> resposta =
                restTemplate.postForEntity("/api/auth/login", request, LoginResponse.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().getToken()).isNotBlank();
    }

    @Test
    void deveRecusarLoginComSenhaErrada() {
        LoginRequest request = new LoginRequest(EMAIL, "senha-errada");

        ResponseEntity<String> resposta =
                restTemplate.postForEntity("/api/auth/login", request, String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deveAcessarRotaProtegidaUsandoOTokenDoLogin() {
        LoginRequest loginRequest = new LoginRequest(EMAIL, SENHA);
        ResponseEntity<LoginResponse> loginResposta =
                restTemplate.postForEntity("/api/auth/login", loginRequest, LoginResponse.class);
        String token = loginResposta.getBody().getToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> resposta =
                restTemplate.exchange("/api/entregas", HttpMethod.GET, entity, String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
