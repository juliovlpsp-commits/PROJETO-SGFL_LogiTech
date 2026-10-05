package com.logitech.sgfl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RoteamentoServiceTest {

    private RoteamentoService servico(String baseUrl) {
        return new RoteamentoService(new ObjectMapper(), baseUrl);
    }

    @Test
    void deveCalcularRotaQuandoProvedorResponde() throws IOException {

        try (StubHttp stub = new StubHttp(200,
                "{\"code\":\"Ok\",\"routes\":[{"
                        + "\"distance\":12345.6,\"duration\":1500.7}]}")) {

            Optional<RotaCalculada> rota = servico(stub.url())
                    .rota(-23.55, -46.63, -22.90, -47.06);

            assertThat(rota).isPresent();
            assertThat(rota.get().distanciaKm())
                    .isCloseTo(12.3456, Offset.offset(0.000001));
            // 1500,7 s = 25,01 min -> arredonda para cima.
            assertThat(rota.get().duracaoMinutos()).isEqualTo(26);
            assertThat(rota.get().fonte()).isEqualTo("OSRM");

            /*
             * Regressão: o OSRM separa os pares de coordenadas com ";".
             * Com "," o provedor responde 404 e o sistema caía no
             * Haversine mesmo com o provedor no ar.
             */
            assertThat(stub.ultimoCaminho())
                    .contains("-46.63,-23.55;-47.06,-22.9");
        }
    }

    @Test
    void deveVoltarVazioQuandoProvedorEstaDesligado() {

        Optional<RotaCalculada> rota = servico("")
                .rota(-23.55, -46.63, -22.90, -47.06);

        assertThat(rota).isEmpty();
    }

    @Test
    void deveVoltarVazioQuandoProvedorEstaForaDoAr() {

        // Porta 1: conexão recusada imediatamente.
        Optional<RotaCalculada> rota = servico("http://127.0.0.1:1")
                .rota(-23.55, -46.63, -22.90, -47.06);

        assertThat(rota).isEmpty();
    }

    @Test
    void deveVoltarVazioQuandoNaoHaRota() throws IOException {

        try (StubHttp stub = new StubHttp(200,
                "{\"code\":\"NoRoute\",\"routes\":[]}")) {

            Optional<RotaCalculada> rota = servico(stub.url())
                    .rota(-23.55, -46.63, -22.90, -47.06);

            assertThat(rota).isEmpty();
        }
    }

    @Test
    void deveVoltarVazioQuandoProvedorErro() throws IOException {

        try (StubHttp stub = new StubHttp(500, "erro interno")) {

            Optional<RotaCalculada> rota = servico(stub.url())
                    .rota(-23.55, -46.63, -22.90, -47.06);

            assertThat(rota).isEmpty();
        }
    }
}
