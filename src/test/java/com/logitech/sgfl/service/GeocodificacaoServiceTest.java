package com.logitech.sgfl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logitech.sgfl.dto.GeocodificacaoResponse;
import com.logitech.sgfl.exceptions.RecursoNaoEncontradoException;
import com.logitech.sgfl.exceptions.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeocodificacaoServiceTest {

    private GeocodificacaoService servico(String baseUrl) {
        return new GeocodificacaoService(
                new ObjectMapper(),
                baseUrl,
                "SGFL-Test/1.0"
        );
    }

    @Test
    void deveGeocodificarEnderecoQuandoProvedorResponde() throws IOException {

        try (StubHttp stub = new StubHttp(200,
                "[{\"lat\":\"-23.55052\",\"lon\":\"-46.63331\","
                        + "\"display_name\":\"São Paulo, Brasil\"}]")) {

            GeocodificacaoResponse resposta =
                    servico(stub.url()).geocodificar("São Paulo");

            assertThat(resposta.latitude()).isEqualTo(-23.55052);
            assertThat(resposta.longitude()).isEqualTo(-46.63331);
            assertThat(resposta.endereco()).contains("São Paulo");
            assertThat(resposta.fonte()).isEqualTo("NOMINATIM");
        }
    }

    @Test
    void deveUsarCacheNaSegundaConsultaDoMesmoEndereco() throws IOException {

        try (StubHttp stub = new StubHttp(200,
                "[{\"lat\":\"-23.55\",\"lon\":\"-46.63\",\"display_name\":\"Paulista\"}]")) {

            GeocodificacaoService service = servico(stub.url());

            service.geocodificar("Avenida Paulista");

            // Depois do cache, a resposta do servidor não importa mais.
            stub.responder(500, "erro");

            GeocodificacaoResponse segunda =
                    service.geocodificar("Avenida Paulista");

            assertThat(segunda.latitude()).isEqualTo(-23.55);
        }
    }

    @Test
    void deveRetornarNaoEncontradoQuandoProvedorNaoAchaOEndereco() throws IOException {

        try (StubHttp stub = new StubHttp(200, "[]")) {

            assertThrows(
                    RecursoNaoEncontradoException.class,
                    () -> servico(stub.url()).geocodificar("Rua que não existe")
            );
        }
    }

    @Test
    void deveReverterCoordenadaParaEndereco() throws IOException {

        try (StubHttp stub = new StubHttp(200,
                "{\"lat\":\"-23.56\",\"lon\":\"-46.65\","
                        + "\"display_name\":\"Av. Paulista, São Paulo\"}")) {

            GeocodificacaoResponse resposta =
                    servico(stub.url()).reverter(-23.56, -46.65);

            assertThat(resposta.endereco()).contains("Av. Paulista");
            assertThat(resposta.fonte()).isEqualTo("NOMINATIM");
        }
    }

    @Test
    void deveRetornarNaoEncontradoQuandoReversaoFalha() throws IOException {

        try (StubHttp stub = new StubHttp(200,
                "{\"error\":\"Unable to geocode\"}")) {

            assertThrows(
                    RecursoNaoEncontradoException.class,
                    () -> servico(stub.url()).reverter(-23.56, -46.65)
            );
        }
    }

    @Test
    void deveTratarProvedorForaDoArComoIndisponivel() {

        // Porta 1: conexão recusada imediatamente.
        GeocodificacaoService service = servico("http://127.0.0.1:1");

        RegraNegocioException excecao = assertThrows(
                RegraNegocioException.class,
                () -> service.geocodificar("São Paulo")
        );

        assertThat(excecao.getMessage()).contains("indisponível");
    }

    @Test
    void deveRecusarQuandoOGeocodificadorNaoEstaConfigurado() {

        GeocodificacaoService service = servico("");

        RegraNegocioException excecao = assertThrows(
                RegraNegocioException.class,
                () -> service.geocodificar("São Paulo")
        );

        assertThat(excecao.getMessage()).contains("SGFL_GEOCODER_URL");
    }

    @Test
    void deveRecusarCoordenadaInvalidaNaReversao() {

        GeocodificacaoService service = servico("http://127.0.0.1:1");

        RegraNegocioException excecao = assertThrows(
                RegraNegocioException.class,
                () -> service.reverter(999, 0)
        );

        assertThat(excecao.getMessage()).contains("inválidas");
    }

    @Test
    void deveRecusarEnderecoEmBranco() {

        GeocodificacaoService service = servico("http://127.0.0.1:1");

        assertThrows(
                RegraNegocioException.class,
                () -> service.geocodificar("   ")
        );
    }
}
