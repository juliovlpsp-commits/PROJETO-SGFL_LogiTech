package com.logitech.sgfl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logitech.sgfl.enums.StatusEntrega;
import com.logitech.sgfl.me.Entrega;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class NotificacaoWhatsappServiceTest {

    private Entrega entrega() {
        Entrega entrega = new Entrega(
                "Rua A, 100 - São Paulo",
                "Av. B, 200 - Campinas",
                12.5
        );
        entrega.setCodigoRastreio("SGFL-TESTE-001");
        return entrega;
    }

    private NotificacaoWhatsappService servico(
            boolean habilitado,
            String numeros
    ) {
        return new NotificacaoWhatsappService(
                habilitado,
                habilitado ? "http://127.0.0.1:9/webhook" : "",
                "",
                "SGFL",
                numeros,
                "https://sgfl.exemplo",
                new ObjectMapper(),
                Runnable::run
        );
    }

    @Test
    void naoEnviaNadaQuandoDesabilitado() {

        NotificacaoWhatsappService service =
                spy(servico(false, "11999998888"));

        service.notificarMudancaDeStatus(
                entrega(),
                StatusEntrega.EM_TRANSITO,
                StatusEntrega.ENTREGUE,
                "admin@gmail.com"
        );

        verify(service, never()).enviar(anyString(), anyString());
    }

    @Test
    void naoEnviaNadaQuandoNaoHaNumeros() {

        NotificacaoWhatsappService service =
                spy(servico(true, "  ,  "));

        service.notificarMudancaDeStatus(
                entrega(),
                StatusEntrega.EM_TRANSITO,
                StatusEntrega.ENTREGUE,
                "admin@gmail.com"
        );

        verify(service, never()).enviar(anyString(), anyString());
    }

    @Test
    void enviaAvisoComCodigoStatusELinkPublico() {

        NotificacaoWhatsappService service =
                spy(servico(true, "11999998888, +55 11 97777-6666"));

        doNothing().when(service).enviar(anyString(), anyString());

        service.notificarMudancaDeStatus(
                entrega(),
                StatusEntrega.EM_TRANSITO,
                StatusEntrega.ENTREGUE,
                "admin@gmail.com"
        );

        // Números normalizados: DDI 55 para brasileiros.
        verify(service).enviar(
                eq("5511999998888"),
                contains("SGFL-TESTE-001")
        );
        verify(service).enviar(
                eq("5511977776666"),
                contains("EM_TRANSITO -> ENTREGUE")
        );

        verify(service, org.mockito.Mockito.times(2)).enviar(
                anyString(),
                contains("https://sgfl.exemplo/rastreio/SGFL-TESTE-001")
        );
    }

    @Test
    void falhaDeGatewayNaoPropagaParaQuemRegistrouAEntrega() {

        NotificacaoWhatsappService service =
                spy(servico(true, "11999998888"));

        doThrow(new RuntimeException("gateway fora do ar"))
                .when(service).enviar(anyString(), anyString());

        assertDoesNotThrow(() ->
                service.notificarMudancaDeStatus(
                        entrega(),
                        StatusEntrega.PENDENTE,
                        StatusEntrega.CANCELADA,
                        "admin@gmail.com"
                )
        );
    }

    @Test
    void configuradoExigeHabilitadoUrlENumero() {

        assertThat(servico(false, "11999998888").configurado())
                .isFalse();
        assertThat(servico(true, "").configurado())
                .isFalse();
        assertThat(servico(true, "11999998888").configurado())
                .isTrue();
    }

    @Test
    void postRealLevaPayloadEsperadoPeloGateway() throws IOException {

        try (StubHttp stub = new StubHttp(200, "{\"ok\":true}")) {

            NotificacaoWhatsappService service = new NotificacaoWhatsappService(
                    true,
                    stub.url(),
                    "token-secreto",
                    "SGFL",
                    "+55 (11) 97777-6666",
                    "https://sgfl.exemplo",
                    new ObjectMapper(),
                    Runnable::run
            );

            service.notificarMudancaDeStatus(
                    entrega(),
                    StatusEntrega.PENDENTE,
                    StatusEntrega.EM_TRANSITO,
                    "admin@gmail.com"
            );

            assertThat(stub.quantidadeRequisicoes()).isEqualTo(1);
            assertThat(stub.ultimoMetodo()).isEqualTo("POST");

            String corpo = stub.ultimoCorpo();
            assertThat(corpo).contains("\"to\":\"5511977776666\"");
            assertThat(corpo).contains("\"from\":\"SGFL\"");
            assertThat(corpo).contains("SGFL-TESTE-001");
            assertThat(corpo).contains("PENDENTE -> EM_TRANSITO");
            assertThat(corpo).contains("https://sgfl.exemplo/rastreio/SGFL-TESTE-001");
        }
    }
}
