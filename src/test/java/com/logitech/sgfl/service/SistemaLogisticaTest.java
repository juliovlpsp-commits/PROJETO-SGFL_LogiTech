package com.logitech.sgfl.service;

import com.logitech.sgfl.enums.StatusEntrega;
import com.logitech.sgfl.enums.TipoCNH;
import com.logitech.sgfl.exceptions.RecursoNaoEncontradoException;
import com.logitech.sgfl.exceptions.RegraNegocioException;
import com.logitech.sgfl.exceptions.VeiculoIncompativelException;
import com.logitech.sgfl.me.Caminhao;
import com.logitech.sgfl.me.Entrega;
import com.logitech.sgfl.me.Motorista;
import com.logitech.sgfl.repository.EntregaRepository;
import com.logitech.sgfl.repository.MotoristaRepository;
import com.logitech.sgfl.repository.VeiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SistemaLogisticaTest {

    @Mock
    private EntregaRepository entregaRepository;

    @Mock
    private VeiculoRepository veiculoRepository;

    @Mock
    private MotoristaRepository motoristaRepository;

    @InjectMocks
    private SistemaLogistica sistemaLogistica;

    private Entrega entrega;
    private Caminhao caminhao;
    private Motorista motoristaCnhB;
    private Motorista motoristaCnhE;

    @BeforeEach
    void setUp() {
        entrega = new Entrega();
        entrega.setPesoCargaKg(5000.0);
        entrega.setStatus(StatusEntrega.PENDENTE);

        caminhao = new Caminhao();
        caminhao.setCapacidadeCargaKg(15000.0);

        motoristaCnhB = new Motorista();
        motoristaCnhB.setNome("Lucas");
        motoristaCnhB.setTipoCNH(TipoCNH.B);

        motoristaCnhE = new Motorista();
        motoristaCnhE.setNome("Carlos");
        motoristaCnhE.setTipoCNH(TipoCNH.E);
    }

    @Test
    @DisplayName("Deve alocar entrega com sucesso quando CNH for compatível")
    void deveAlocarEntregaComSucesso() {

        when(entregaRepository.findById(1L))
                .thenReturn(Optional.of(entrega));

        when(veiculoRepository.findById(5L))
                .thenReturn(Optional.of(caminhao));

        when(motoristaRepository.findById(1L))
                .thenReturn(Optional.of(motoristaCnhE));

        when(entregaRepository.save(any(Entrega.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        Entrega resultado =
                sistemaLogistica.alocarEntrega(
                        1L,
                        5L,
                        1L
                );

        assertNotNull(resultado);
        assertEquals(
                StatusEntrega.EM_TRANSITO,
                resultado.getStatus()
        );
        assertEquals(
                caminhao,
                resultado.getVeiculo()
        );
        assertEquals(
                motoristaCnhE,
                resultado.getMotorista()
        );

        verify(entregaRepository, times(1))
                .save(entrega);
    }

    @Test
    @DisplayName("Não deve alocar entrega que não esteja PENDENTE")
    void naoDeveAlocarEntregaForaDoStatusPendente() {

        entrega.setStatus(StatusEntrega.EM_TRANSITO);

        when(entregaRepository.findById(1L))
                .thenReturn(Optional.of(entrega));

        assertThrows(
                RegraNegocioException.class,
                () -> sistemaLogistica.alocarEntrega(
                        1L,
                        5L,
                        1L
                )
        );

        verify(veiculoRepository, never())
                .findById(anyLong());

        verify(motoristaRepository, never())
                .findById(anyLong());

        verify(entregaRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("Não deve alocar entrega já entregue")
    void naoDeveAlocarEntregaJaEntregue() {

        entrega.setStatus(StatusEntrega.ENTREGUE);

        when(entregaRepository.findById(1L))
                .thenReturn(Optional.of(entrega));

        assertThrows(
                RegraNegocioException.class,
                () -> sistemaLogistica.alocarEntrega(
                        1L,
                        5L,
                        1L
                )
        );

        verify(entregaRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("Não deve alocar entrega cancelada")
    void naoDeveAlocarEntregaCancelada() {

        entrega.setStatus(StatusEntrega.CANCELADA);

        when(entregaRepository.findById(1L))
                .thenReturn(Optional.of(entrega));

        assertThrows(
                RegraNegocioException.class,
                () -> sistemaLogistica.alocarEntrega(
                        1L,
                        5L,
                        1L
                )
        );

        verify(entregaRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando CNH B tentar alocar Caminhão")
    void deveLancarExcecaoQuandoCnhIncompativelComCaminhao() {

        when(entregaRepository.findById(1L))
                .thenReturn(Optional.of(entrega));

        when(veiculoRepository.findById(5L))
                .thenReturn(Optional.of(caminhao));

        when(motoristaRepository.findById(2L))
                .thenReturn(Optional.of(motoristaCnhB));

        VeiculoIncompativelException exception =
                assertThrows(
                        VeiculoIncompativelException.class,
                        () -> sistemaLogistica.alocarEntrega(
                                1L,
                                5L,
                                2L
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "não possui permissão para dirigir Caminhão"
                        )
        );

        verify(entregaRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("Não deve alocar quando carga exceder capacidade do veículo")
    void naoDeveAlocarQuandoCargaExcederCapacidade() {

        caminhao.setCapacidadeCargaKg(1000.0);

        when(entregaRepository.findById(1L))
                .thenReturn(Optional.of(entrega));

        when(veiculoRepository.findById(5L))
                .thenReturn(Optional.of(caminhao));

        when(motoristaRepository.findById(1L))
                .thenReturn(Optional.of(motoristaCnhE));

        assertThrows(
                VeiculoIncompativelException.class,
                () -> sistemaLogistica.alocarEntrega(
                        1L,
                        5L,
                        1L
                )
        );

        verify(entregaRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("Deve informar quando a entrega não existir")
    void deveLancarExcecaoQuandoEntregaNaoExistir() {

        when(entregaRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNaoEncontradoException.class,
                () -> sistemaLogistica.alocarEntrega(
                        99L,
                        5L,
                        1L
                )
        );
    }

    @Test
    @DisplayName("Deve informar quando o veículo não existir")
    void deveLancarExcecaoQuandoVeiculoNaoExistir() {

        when(entregaRepository.findById(1L))
                .thenReturn(Optional.of(entrega));

        when(veiculoRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNaoEncontradoException.class,
                () -> sistemaLogistica.alocarEntrega(
                        1L,
                        99L,
                        1L
                )
        );

        verify(entregaRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("Deve informar quando o motorista não existir")
    void deveLancarExcecaoQuandoMotoristaNaoExistir() {

        when(entregaRepository.findById(1L))
                .thenReturn(Optional.of(entrega));

        when(veiculoRepository.findById(5L))
                .thenReturn(Optional.of(caminhao));

        when(motoristaRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNaoEncontradoException.class,
                () -> sistemaLogistica.alocarEntrega(
                        1L,
                        5L,
                        99L
                )
        );

        verify(entregaRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("Deve finalizar entrega em trânsito")
    void deveFinalizarEntregaComSucesso() {

        entrega.setStatus(StatusEntrega.EM_TRANSITO);
        entrega.setVeiculo(caminhao);
        entrega.setMotorista(motoristaCnhE);

        when(entregaRepository.findById(1L))
                .thenReturn(Optional.of(entrega));

        when(entregaRepository.save(any(Entrega.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        Entrega resultado =
                sistemaLogistica.finalizarEntrega(1L);

        assertNotNull(resultado);
        assertEquals(
                StatusEntrega.ENTREGUE,
                resultado.getStatus()
        );

        verify(entregaRepository, times(1))
                .save(entrega);
    }

    @Test
    @DisplayName("Não deve finalizar entrega PENDENTE")
    void naoDeveFinalizarEntregaPendente() {

        entrega.setStatus(StatusEntrega.PENDENTE);

        when(entregaRepository.findById(1L))
                .thenReturn(Optional.of(entrega));

        assertThrows(
                RegraNegocioException.class,
                () -> sistemaLogistica.finalizarEntrega(1L)
        );

        verify(entregaRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("Não deve finalizar entrega CANCELADA")
    void naoDeveFinalizarEntregaCancelada() {

        entrega.setStatus(StatusEntrega.CANCELADA);

        when(entregaRepository.findById(1L))
                .thenReturn(Optional.of(entrega));

        assertThrows(
                RegraNegocioException.class,
                () -> sistemaLogistica.finalizarEntrega(1L)
        );

        verify(entregaRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("Não deve finalizar entrega ENTREGUE novamente")
    void naoDeveFinalizarEntregaJaEntregue() {

        entrega.setStatus(StatusEntrega.ENTREGUE);

        when(entregaRepository.findById(1L))
                .thenReturn(Optional.of(entrega));

        assertThrows(
                RegraNegocioException.class,
                () -> sistemaLogistica.finalizarEntrega(1L)
        );

        verify(entregaRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("Não deve finalizar entrega sem veículo")
    void naoDeveFinalizarSemVeiculo() {

        entrega.setStatus(StatusEntrega.EM_TRANSITO);
        entrega.setMotorista(motoristaCnhE);

        when(entregaRepository.findById(1L))
                .thenReturn(Optional.of(entrega));

        assertThrows(
                RegraNegocioException.class,
                () -> sistemaLogistica.finalizarEntrega(1L)
        );

        verify(entregaRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("Não deve finalizar entrega sem motorista")
    void naoDeveFinalizarSemMotorista() {

        entrega.setStatus(StatusEntrega.EM_TRANSITO);
        entrega.setVeiculo(caminhao);

        when(entregaRepository.findById(1L))
                .thenReturn(Optional.of(entrega));

        assertThrows(
                RegraNegocioException.class,
                () -> sistemaLogistica.finalizarEntrega(1L)
        );

        verify(entregaRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("Deve permitir cancelamento de entrega PENDENTE")
    void deveCancelarEntregaPendente() {

        when(entregaRepository.findById(1L))
                .thenReturn(Optional.of(entrega));

        when(entregaRepository.save(any(Entrega.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        Entrega resultado =
                sistemaLogistica.atualizarStatus(
                        1L,
                        StatusEntrega.CANCELADA
                );

        assertEquals(
                StatusEntrega.CANCELADA,
                resultado.getStatus()
        );

        verify(entregaRepository)
                .save(entrega);
    }

    @Test
    @DisplayName("Deve permitir cancelamento de entrega EM_TRANSITO")
    void deveCancelarEntregaEmTransito() {

        entrega.setStatus(StatusEntrega.EM_TRANSITO);
        entrega.setVeiculo(caminhao);
        entrega.setMotorista(motoristaCnhE);

        when(entregaRepository.findById(1L))
                .thenReturn(Optional.of(entrega));

        when(entregaRepository.save(any(Entrega.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        Entrega resultado =
                sistemaLogistica.atualizarStatus(
                        1L,
                        StatusEntrega.CANCELADA
                );

        assertEquals(
                StatusEntrega.CANCELADA,
                resultado.getStatus()
        );

        verify(entregaRepository)
                .save(entrega);
    }

    @Test
    @DisplayName("Não deve permitir PENDENTE diretamente para ENTREGUE")
    void naoDevePermitirPendenteParaEntregue() {

        when(entregaRepository.findById(1L))
                .thenReturn(Optional.of(entrega));

        assertThrows(
                RegraNegocioException.class,
                () -> sistemaLogistica.atualizarStatus(
                        1L,
                        StatusEntrega.ENTREGUE
                )
        );

        verify(entregaRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("Não deve permitir PENDENTE diretamente para EM_TRANSITO")
    void naoDevePermitirPendenteParaEmTransito() {

        when(entregaRepository.findById(1L))
                .thenReturn(Optional.of(entrega));

        assertThrows(
                RegraNegocioException.class,
                () -> sistemaLogistica.atualizarStatus(
                        1L,
                        StatusEntrega.EM_TRANSITO
                )
        );

        verify(entregaRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("Não deve reabrir uma entrega ENTREGUE")
    void naoDeveReabrirEntregaEntregue() {

        entrega.setStatus(StatusEntrega.ENTREGUE);

        when(entregaRepository.findById(1L))
                .thenReturn(Optional.of(entrega));

        assertThrows(
                RegraNegocioException.class,
                () -> sistemaLogistica.atualizarStatus(
                        1L,
                        StatusEntrega.PENDENTE
                )
        );

        verify(entregaRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("Não deve alterar o status quando ele já for o mesmo")
    void naoDeveSalvarQuandoStatusForIgual() {

        when(entregaRepository.findById(1L))
                .thenReturn(Optional.of(entrega));

        Entrega resultado =
                sistemaLogistica.atualizarStatus(
                        1L,
                        StatusEntrega.PENDENTE
                );

        assertSame(entrega, resultado);

        verify(entregaRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("Deve listar todas as entregas")
    void deveListarTodas() {

        List<Entrega> entregas = List.of(
                entrega,
                new Entrega()
        );

        when(entregaRepository.findAll())
                .thenReturn(entregas);

        List<Entrega> resultado =
                sistemaLogistica.listarTodas();

        assertEquals(
                2,
                resultado.size()
        );

        verify(entregaRepository)
                .findAll();
    }
}