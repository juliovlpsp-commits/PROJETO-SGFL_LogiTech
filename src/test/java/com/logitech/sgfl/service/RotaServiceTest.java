package com.logitech.sgfl.service;

import com.logitech.sgfl.dto.EntregaEtaResponse;
import com.logitech.sgfl.dto.RotaEstimativaResponse;
import com.logitech.sgfl.exceptions.RecursoNaoEncontradoException;
import com.logitech.sgfl.me.Entrega;
import com.logitech.sgfl.me.EntregaEta;
import com.logitech.sgfl.repository.EntregaEtaRepository;
import com.logitech.sgfl.repository.EntregaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RotaServiceTest {

    @Mock
    private EntregaRepository entregaRepository;

    @Mock
    private EntregaEtaRepository etaRepository;

    @Mock
    private RoteamentoService roteamento;

    private RotaService service;

    @BeforeEach
    void configurar() {
        service = new RotaService(
                entregaRepository,
                etaRepository,
                roteamento,
                50
        );
    }

    private Entrega entregaComCoordenadas() {
        Entrega entrega = new Entrega(
                "São Paulo",
                "Campinas",
                10
        );
        ReflectionTestUtils.setField(entrega, "id", 7L);
        entrega.setLatitudeOrigem(-23.55052);
        entrega.setLongitudeOrigem(-46.63331);
        entrega.setLatitudeDestino(-22.90683);
        entrega.setLongitudeDestino(-47.06157);
        return entrega;
    }

    private Entrega entregaSemCoordenadas() {
        Entrega entrega = new Entrega("Origem", "Destino", 10);
        ReflectionTestUtils.setField(entrega, "id", 8L);
        return entrega;
    }

    @Test
    void deveUsarHaversineQuandoNaoHaProvedor() {

        Entrega entrega = entregaComCoordenadas();

        when(entregaRepository.findById(7L))
                .thenReturn(Optional.of(entrega));
        when(roteamento.rota(
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble()
        ))
                .thenReturn(Optional.empty());
        when(etaRepository.findTopByEntregaIdOrderByCriadoEmDesc(7L))
                .thenReturn(Optional.empty());

        RotaEstimativaResponse resposta = service.estimar(7L);

        assertThat(resposta.possuiCoordenadas()).isTrue();
        assertThat(resposta.fonte()).isEqualTo("HAVERSINE");
        assertThat(resposta.distanciaKm()).isBetween(70.0, 95.0);
        assertThat(resposta.velocidadeMediaKmH()).isEqualTo(50);
        assertThat(resposta.duracaoMinutos()).isBetween(98, 104);
        assertThat(resposta.previsaoChegada()).isNotNull();

        // O fallback de linha reta também devolve a forma para o mapa.
        assertThat(resposta.geometria()).hasSize(2);
        assertThat(resposta.geometria().get(0).latitude()).isEqualTo(-23.55052);

        verify(etaRepository).save(any(EntregaEta.class));
    }

    @Test
    void deveUsarRotaRealQuandoProvedorDisponivel() {

        Entrega entrega = entregaComCoordenadas();

        when(entregaRepository.findById(7L))
                .thenReturn(Optional.of(entrega));
        when(roteamento.rota(
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble()
        ))
                .thenReturn(Optional.of(
                        new RotaCalculada(
                                120.5,
                                90,
                                "OSRM",
                                List.of(
                                        new com.logitech.sgfl.dto.PontoRota(-23.55, -46.63),
                                        new com.logitech.sgfl.dto.PontoRota(-23.40, -46.50),
                                        new com.logitech.sgfl.dto.PontoRota(-22.90, -47.06)
                                )
                        )
                ));
        when(etaRepository.findTopByEntregaIdOrderByCriadoEmDesc(7L))
                .thenReturn(Optional.empty());

        RotaEstimativaResponse resposta = service.estimar(7L);

        assertThat(resposta.fonte()).isEqualTo("OSRM");
        assertThat(resposta.distanciaKm()).isEqualTo(120.5);
        assertThat(resposta.duracaoMinutos()).isEqualTo(90);
        // Velocidade efetiva: 120,5 km em 90 min = 80 km/h.
        assertThat(resposta.velocidadeMediaKmH()).isEqualTo(80);
        // A geometria do provedor chega intacta na resposta da API.
        assertThat(resposta.geometria()).hasSize(3);
        assertThat(resposta.geometria().get(2).latitude()).isEqualTo(-22.90);

        verify(etaRepository).save(any(EntregaEta.class));
    }

    @Test
    void naoDeveCalcularRotaSemCoordenadas() {

        Entrega entrega = entregaSemCoordenadas();

        when(entregaRepository.findById(8L))
                .thenReturn(Optional.of(entrega));

        RotaEstimativaResponse resposta = service.estimar(8L);

        assertThat(resposta.possuiCoordenadas()).isFalse();
        assertThat(resposta.fonte()).isNull();
        assertThat(resposta.distanciaKm()).isNull();

        verify(etaRepository, never()).save(any());
    }

    @Test
    void naoDeveGravarHistoricoNovoDentroDaJanelaDeCincoMinutos() {

        Entrega entrega = entregaComCoordenadas();

        EntregaEta ultimo = new EntregaEta(
                entrega, 80.0, 96, LocalDateTime.now(), "OSRM",
                LocalDateTime.now().minusMinutes(2)
        );

        when(entregaRepository.findById(7L))
                .thenReturn(Optional.of(entrega));
        when(roteamento.rota(
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble()
        ))
                .thenReturn(Optional.empty());
        when(etaRepository.findTopByEntregaIdOrderByCriadoEmDesc(7L))
                .thenReturn(Optional.of(ultimo));

        service.estimar(7L);

        verify(etaRepository, never()).save(any());
    }

    @Test
    void deveListarHistoricoDaEntrega() {

        Entrega entrega = entregaComCoordenadas();

        EntregaEta eta = new EntregaEta(
                entrega, 81.2, 98, LocalDateTime.now(), "OSRM",
                LocalDateTime.now().minusHours(1)
        );

        when(entregaRepository.existsById(7L)).thenReturn(true);
        when(etaRepository.findByEntregaIdOrderByCriadoEmDesc(7L))
                .thenReturn(List.of(eta));

        List<EntregaEtaResponse> historico = service.historico(7L);

        assertThat(historico).hasSize(1);
        assertThat(historico.get(0).fonte()).isEqualTo("OSRM");
        assertThat(historico.get(0).distanciaKm()).isEqualTo(81.2);
    }

    @Test
    void deveFalharCom404QuandoEntregaNaoExiste() {

        when(entregaRepository.existsById(99L)).thenReturn(false);

        assertThrows(
                RecursoNaoEncontradoException.class,
                () -> service.historico(99L)
        );
    }

    @Test
    void deveFalharCom404QuandoEstimativaNaoAchaAEntrega() {

        when(entregaRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNaoEncontradoException.class,
                () -> service.estimar(99L)
        );
    }
}
