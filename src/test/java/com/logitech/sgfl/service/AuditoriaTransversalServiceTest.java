package com.logitech.sgfl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logitech.sgfl.enums.StatusPedido;
import com.logitech.sgfl.me.AuditoriaRegistro;
import com.logitech.sgfl.repository.AuditoriaRegistroRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditoriaTransversalServiceTest {

    @Mock
    private AuditoriaRegistroRepository repository;

    private AuditoriaTransversalService service;

    @BeforeEach
    void configurar() {
        service = new AuditoriaTransversalService(
                repository,
                new ObjectMapper()
        );
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveSalvarRegistroComUsuarioDaSessao() {

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin@gmail.com",
                        "sem-senha",
                        List.of()
                )
        );

        service.registrar(
                "CLIENTE",
                7L,
                "CRIADO",
                "Cliente Ana cadastrado.",
                null,
                novoModelo()
        );

        AuditoriaRegistro registro = capturar();

        assertThat(registro.getEntidade()).isEqualTo("CLIENTE");
        assertThat(registro.getEntidadeId()).isEqualTo(7L);
        assertThat(registro.getAcao()).isEqualTo("CRIADO");
        assertThat(registro.getDescricao())
                .isEqualTo("Cliente Ana cadastrado.");
        assertThat(registro.getUsuario()).isEqualTo("admin@gmail.com");
        assertThat(registro.getCriadoEm()).isNotNull();
        assertThat(registro.getDadosAntes()).isNull();
        assertThat(registro.getDadosDepois()).contains("\"nome\":\"Ana\"");
    }

    @Test
    void deveUsarSistemaQuandoNaoHaSessao() {

        service.registrar("PRODUTO", 1L, "EXCLUIDO", "Produto removido.");

        AuditoriaRegistro registro = capturar();

        assertThat(registro.getUsuario()).isEqualTo("sistema");
        assertThat(registro.getDadosAntes()).isNull();
        assertThat(registro.getDadosDepois()).isNull();
    }

    @Test
    void deveResumirApenasCamposSimples() {

        Object antes = service.fotografia(novoModelo());

        service.registrar(
                "PEDIDO",
                3L,
                "ATUALIZADO",
                "Pedido alterado.",
                antes,
                novoModelo()
        );

        AuditoriaRegistro registro = capturar();
        String dados = registro.getDadosDepois();

        // Campos simples entram.
        assertThat(dados).contains("\"nome\":\"Ana\"");
        assertThat(dados).contains("\"idade\":30");
        assertThat(dados).contains("\"status\":\"ABERTO\"");
        assertThat(dados).contains("\"criadoEm\":\"2026-01-01T10:00\"");

        // Relação, coleção e campo estático ficam de fora:
        // serializar o grafo inteiro estouraria o lazy loading.
        assertThat(dados).doesNotContain("relacao");
        assertThat(dados).doesNotContain("lista");
        assertThat(dados).doesNotContain("ESTATICO");
    }

    @Test
    void deveNormalizarEnumERejeitarNulos() {

        Object antes = service.fotografia(null);

        service.registrar(
                "PEDIDO",
                9L,
                "ATUALIZADO",
                "Sem antes.",
                antes,
                null
        );

        AuditoriaRegistro registro = capturar();

        assertThat(registro.getDadosAntes()).isNull();
        assertThat(registro.getDadosDepois()).isNull();
    }

    private AuditoriaRegistro capturar() {

        ArgumentCaptor<AuditoriaRegistro> captor =
                ArgumentCaptor.forClass(AuditoriaRegistro.class);

        verify(repository).save(captor.capture());

        return captor.getValue();
    }

    private Modelo novoModelo() {
        return new Modelo();
    }

    /**
     * Espelho de uma entidade real: campos simples + relação + coleção +
     * campo estático, para provar o recorte da auditoria.
     */
    static class Modelo {

        private static final String ESTATICO = "nao-auditado";

        private String nome = "Ana";
        private int idade = 30;
        private StatusPedido status = StatusPedido.ABERTO;
        private LocalDateTime criadoEm = LocalDateTime.of(2026, 1, 1, 10, 0);
        private Object relacao = new Object();
        private List<String> lista = List.of("item");
    }
}
