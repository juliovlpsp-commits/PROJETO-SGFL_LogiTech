package com.logitech.sgfl.service;

import com.logitech.sgfl.enums.StatusEntrega;
import com.logitech.sgfl.me.Entrega;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class NotificacaoEmailServiceTest {

    private Entrega entrega() {
        Entrega entrega = new Entrega(
                "Rua A, 100 - São Paulo",
                "Av. B, 200 - Campinas",
                12.5
        );
        entrega.setCodigoRastreio("SGFL-TESTE-001");
        entrega.setDescricao("Entrega de testes");
        return entrega;
    }

    private NotificacaoEmailService servico(
            boolean habilitado,
            String destinatarios
    ) {
        return new NotificacaoEmailService(
                habilitado,
                habilitado ? "smtp.example.com" : "",
                587,
                "usuario",
                "senha",
                "noreply@sgfl.local",
                destinatarios,
                true,
                "https://sgfl.exemplo",
                Runnable::run
        );
    }

    @Test
    void naoEnviaNadaQuandoDesabilitado() {

        NotificacaoEmailService service =
                spy(servico(false, "ops@sgfl.local"));

        service.notificarMudancaDeStatus(
                entrega(),
                StatusEntrega.EM_TRANSITO,
                StatusEntrega.ENTREGUE,
                "admin@gmail.com"
        );

        verify(service, never()).enviar(anyString(), anyString());
    }

    @Test
    void naoEnviaNadaQuandoNaoHaDestinatarios() {

        NotificacaoEmailService service =
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

        NotificacaoEmailService service =
                spy(servico(true, "ops@sgfl.local, gestor@sgfl.local"));

        doNothing().when(service).enviar(anyString(), anyString());

        service.notificarMudancaDeStatus(
                entrega(),
                StatusEntrega.EM_TRANSITO,
                StatusEntrega.ENTREGUE,
                "admin@gmail.com"
        );

        verify(service).enviar(
                contains("SGFL-TESTE-001"),
                contains("EM_TRANSITO -> ENTREGUE")
        );

        verify(service).enviar(
                anyString(),
                contains("https://sgfl.exemplo/rastreio/SGFL-TESTE-001")
        );
    }

    @Test
    void falhaDeSmtpNaoPropagaParaQuemRegistrouAEntrega() {

        NotificacaoEmailService service =
                spy(servico(true, "ops@sgfl.local"));

        doThrow(new RuntimeException("smtp fora do ar"))
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
    void configuradoExigeHabilitadoHostEDestinatario() {

        assertThat(servico(false, "ops@sgfl.local").configurado())
                .isFalse();
        assertThat(servico(true, "").configurado())
                .isFalse();
        assertThat(servico(true, "ops@sgfl.local").configurado())
                .isTrue();
    }
}
