package com.logitech.sgfl.service;

import com.logitech.sgfl.enums.StatusEntrega;
import com.logitech.sgfl.me.Entrega;
import com.logitech.sgfl.me.EntregaEvento;
import com.logitech.sgfl.repository.EntregaEventoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EntregaAuditoriaServiceTest {

    @Mock
    private EntregaEventoRepository repository;

    @Mock
    private NotificacaoEmailService notificacao;

    @InjectMocks
    private EntregaAuditoriaService service;

    private Entrega entrega() {
        Entrega entrega = new Entrega(
                "Rua A, 100",
                "Av. B, 200",
                10
        );
        entrega.setCodigoRastreio("SGFL-0001");
        return entrega;
    }

    @Test
    void deveNotificarQuandoStatusRealmenteMuda() {

        Entrega entrega = entrega();

        service.registrar(
                entrega,
                "ALOCADA",
                StatusEntrega.PENDENTE,
                StatusEntrega.EM_TRANSITO,
                "Veículo e motorista associados."
        );

        verify(repository).save(any(EntregaEvento.class));

        verify(notificacao).notificarMudancaDeStatus(
                eq(entrega),
                eq(StatusEntrega.PENDENTE),
                eq(StatusEntrega.EM_TRANSITO),
                anyString()
        );
    }

    @Test
    void naoNotificaQuandoOStatusNaoMuda() {

        service.registrar(
                entrega(),
                "COMPROVANTE_ANEXADO",
                StatusEntrega.ENTREGUE,
                StatusEntrega.ENTREGUE,
                "Comprovante anexado."
        );

        verify(repository).save(any(EntregaEvento.class));

        verify(notificacao, never()).notificarMudancaDeStatus(
                any(),
                any(),
                any(),
                anyString()
        );
    }

    @Test
    void naoNotificaNaCriacaoDaEntrega() {

        service.registrar(
                entrega(),
                "CRIADA",
                null,
                StatusEntrega.PENDENTE,
                "Entrega registrada."
        );

        verify(repository).save(any(EntregaEvento.class));

        verify(notificacao, never()).notificarMudancaDeStatus(
                any(),
                any(),
                any(),
                anyString()
        );
    }
}
