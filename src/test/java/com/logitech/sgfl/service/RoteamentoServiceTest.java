package com.logitech.sgfl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logitech.sgfl.dto.PontoRota;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RoteamentoServiceTest {

    private RoteamentoService servico(String baseUrl) {
        return new RoteamentoService(new ObjectMapper(), "OSRM", baseUrl, "");
    }

    private RoteamentoService servico(String provedor, String baseUrl, String chave) {
        return new RoteamentoService(new ObjectMapper(), provedor, baseUrl, chave);
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

            // Pedimos a forma da rota (polyline) para o mapa desenhar.
            assertThat(stub.ultimoCaminho())
                    .contains("overview=simplified");
        }
    }

    @Test
    void deveExtrairGeometriaDaRotaOsrm() throws IOException {

        // Polyline que descreve (0,0) -> (1,1).
        try (StubHttp stub = new StubHttp(200,
                "{\"code\":\"Ok\",\"routes\":[{"
                        + "\"distance\":1000,\"duration\":60,"
                        + "\"geometry\":\"??_ibE_ibE\"}]}")) {

            Optional<RotaCalculada> rota = servico(stub.url())
                    .rota(0, 0, 1, 1);

            assertThat(rota).isPresent();
            List<PontoRota> geometria = rota.get().geometria();
            assertThat(geometria).hasSize(2);
            assertThat(geometria.get(0).latitude()).isCloseTo(0.0, Offset.offset(0.000001));
            assertThat(geometria.get(0).longitude()).isCloseTo(0.0, Offset.offset(0.000001));
            assertThat(geometria.get(1).latitude()).isCloseTo(1.0, Offset.offset(0.000001));
            assertThat(geometria.get(1).longitude()).isCloseTo(1.0, Offset.offset(0.000001));
        }
    }

    @Test
    void deveDecodificarPolylineConhecida() {

        // Exemplo clássico da polyline do Google/OSRM.
        List<PontoRota> pontos = servico("")
                .decodificarPolyline("_p~iF~ps|U_ulLnnqC_mqNvxq`@");

        assertThat(pontos).isNotEmpty();
        assertThat(pontos.get(0).latitude()).isCloseTo(38.5, Offset.offset(0.001));
        assertThat(pontos.get(0).longitude()).isCloseTo(-120.2, Offset.offset(0.001));

        for (PontoRota ponto : pontos) {
            assertThat(ponto.latitude()).isBetween(-90.0, 90.0);
            assertThat(ponto.longitude()).isBetween(-180.0, 180.0);
        }
    }

    @Test
    void deveCalcularRotaComTransitoNoTomTom() throws IOException {

        try (StubHttp stub = new StubHttp(200,
                "{\"routes\":[{"
                        + "\"summary\":{"
                        + "\"lengthInMeters\":433620,"
                        + "\"travelTimeInSeconds\":20076},"
                        + "\"legs\":[{\"points\":["
                        + "{\"latitude\":-23.5505,\"longitude\":-46.6333},"
                        + "{\"latitude\":-22.9068,\"longitude\":-43.1729}"
                        + "]}]}]}")) {

            Optional<RotaCalculada> rota =
                    servico("TOMTOM", stub.url(), "chave-teste")
                            .rota(-23.55, -46.63, -22.90, -47.06);

            assertThat(rota).isPresent();
            assertThat(rota.get().distanciaKm())
                    .isCloseTo(433.62, Offset.offset(0.000001));
            // 20076 s = 334,6 min -> arredonda para cima.
            assertThat(rota.get().duracaoMinutos()).isEqualTo(335);
            assertThat(rota.get().fonte()).isEqualTo("TOMTOM");

            List<PontoRota> geometria = rota.get().geometria();
            assertThat(geometria).hasSize(2);
            assertThat(geometria.get(0).latitude()).isCloseTo(-23.5505, Offset.offset(0.0001));

            // URL do TomTom: chave presente e trânsito real ativado.
            assertThat(stub.ultimoCaminho()).contains("key=chave-teste");
            assertThat(stub.ultimoCaminho()).contains("traffic=true");
            assertThat(stub.ultimoMetodo()).isEqualTo("GET");
        }
    }

    @Test
    void tomTomSemChaveNemSeChama() throws IOException {

        try (StubHttp stub = new StubHttp(200, "{}")) {

            Optional<RotaCalculada> rota =
                    servico("TOMTOM", stub.url(), "")
                            .rota(-23.55, -46.63, -22.90, -47.06);

            assertThat(rota).isEmpty();
            assertThat(stub.quantidadeRequisicoes()).isZero();
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
