package com.logitech.sgfl.integration;

import com.logitech.sgfl.dto.LoginRequest;
import com.logitech.sgfl.dto.RegistroRequest;
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
    void deveRecusarAcessoARotaProtegidaSemSessao() {
        ResponseEntity<String> resposta = restTemplate.getForEntity("/api/entregas", String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deveRecusarTokenInvalidoComUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("token.invalido.qualquer");

        ResponseEntity<String> resposta =
                restTemplate.exchange("/api/entregas", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void operadorNaoPodeExcluirEntrega() {
        criarUsuario("operador@exemplo.com", SENHA, Perfil.ROLE_OPERADOR);
        String cookie = fazerLogin("operador@exemplo.com", SENHA);

        ResponseEntity<String> resposta = restTemplate.exchange(
                "/api/entregas/999", HttpMethod.DELETE, new HttpEntity<>(comCookie(cookie)), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void operadorNaoPodeCadastrarMotorista() {
        criarUsuario("operador@exemplo.com", SENHA, Perfil.ROLE_OPERADOR);
        String cookie = fazerLogin("operador@exemplo.com", SENHA);

        HttpHeaders headers = comCookie(cookie);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> resposta = restTemplate.exchange(
                "/api/motoristas", HttpMethod.POST,
                new HttpEntity<>("{\"nome\":\"Fulano\",\"cpf\":\"52998224725\",\"tipoCNH\":\"B\"}", headers),
                String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void operadorPodeListarEntregas() {
        criarUsuario("operador@exemplo.com", SENHA, Perfil.ROLE_OPERADOR);
        String cookie = fazerLogin("operador@exemplo.com", SENHA);

        ResponseEntity<String> resposta = restTemplate.exchange(
                "/api/entregas", HttpMethod.GET, new HttpEntity<>(comCookie(cookie)), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void adminPassaPelaAutorizacaoDeExclusao() {
        String cookie = fazerLogin(EMAIL, SENHA);

        // A entrega 999 não existe: passar pela autorização e chegar no 404 prova que não houve 403.
        ResponseEntity<String> resposta = restTemplate.exchange(
                "/api/entregas/999", HttpMethod.DELETE, new HttpEntity<>(comCookie(cookie)), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cadastroPublicoDeveCriarUsuarioComoOperador() {
        ResponseEntity<String> resposta = restTemplate.postForEntity(
                "/api/auth/registrar", new RegistroRequest("novo@exemplo.com", "senhaSegura1"), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(usuarioRepository.findByEmail("novo@exemplo.com"))
                .get()
                .extracting(Usuario::getPerfil)
                .isEqualTo(Perfil.ROLE_OPERADOR);
    }

    @Test
    void cadastroPublicoDeveRecusarSenhaCurta() {
        ResponseEntity<String> resposta = restTemplate.postForEntity(
                "/api/auth/registrar", new RegistroRequest("novo@exemplo.com", "123"), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(usuarioRepository.findByEmail("novo@exemplo.com")).isEmpty();
    }

    @Test
    void loginSemCredenciaisDeveRetornar400() {
        ResponseEntity<String> resposta = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest("", ""), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private void criarUsuario(String email, String senha, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setUsername(email);
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(senha));
        usuario.setPerfil(perfil);
        usuarioRepository.save(usuario);
    }

    private String fazerLogin(String email, String senha) {
        ResponseEntity<Void> resposta = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(email, senha), Void.class);
        return cookieDaResposta(resposta);
    }

    private HttpHeaders comCookie(String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        return headers;
    }

    private String cookieDaResposta(ResponseEntity<?> resposta) {
        String setCookie = resposta.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotBlank();
        return setCookie.split(";", 2)[0];
    }

    @Test
    void deveFazerLoginComEmailEConfigurarCookieSeguro() {
        LoginRequest request = new LoginRequest(EMAIL, SENHA);

        ResponseEntity<Void> resposta =
                restTemplate.postForEntity("/api/auth/login", request, Void.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("HttpOnly", "SameSite=Lax", "Path=/api");
    }

    @Test
    void deveRecusarLoginComSenhaErrada() {
        LoginRequest request = new LoginRequest(EMAIL, "senha-errada");

        ResponseEntity<String> resposta =
                restTemplate.postForEntity("/api/auth/login", request, String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deveAcessarRotaProtegidaUsandoCookieDoLogin() {
        LoginRequest loginRequest = new LoginRequest(EMAIL, SENHA);
        ResponseEntity<Void> loginResposta =
                restTemplate.postForEntity("/api/auth/login", loginRequest, Void.class);

        HttpHeaders headers = comCookie(cookieDaResposta(loginResposta));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> resposta =
                restTemplate.exchange("/api/entregas", HttpMethod.GET, entity, String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
