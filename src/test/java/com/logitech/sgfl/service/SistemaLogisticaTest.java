package com.logitech.sgfl.service;

import com.logitech.sgfl.enums.StatusEntrega;
import com.logitech.sgfl.enums.TipoCNH;
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
        when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
        when(veiculoRepository.findById(5L)).thenReturn(Optional.of(caminhao));
        when(motoristaRepository.findById(1L)).thenReturn(Optional.of(motoristaCnhE));
        when(entregaRepository.save(any(Entrega.class))).thenAnswer(i -> i.getArguments()[0]);

        Entrega resultado = sistemaLogistica.alocarEntrega(1L, 5L, 1L);

        assertNotNull(resultado);
        assertEquals(StatusEntrega.EM_TRANSITO, resultado.getStatus());
        assertEquals(caminhao, resultado.getVeiculo());
        assertEquals(motoristaCnhE, resultado.getMotorista());

        verify(entregaRepository, times(1)).save(entrega);
    }

    @Test
    @DisplayName("Deve lançar exceção quando CNH B tentar alocar Caminhão")
    void deveLancarExcecaoQuandoCnhIncompativelComCaminhao() {
        when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
        when(veiculoRepository.findById(5L)).thenReturn(Optional.of(caminhao));
        when(motoristaRepository.findById(2L)).thenReturn(Optional.of(motoristaCnhB));

        VeiculoIncompativelException exception = assertThrows(
                VeiculoIncompativelException.class,
                () -> sistemaLogistica.alocarEntrega(1L, 5L, 2L)
        );

        assertTrue(exception.getMessage().contains("não possui permissão para dirigir Caminhão"));
        verify(entregaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve finalizar entrega com sucesso")
    void deveFinalizarEntregaComSucesso() {
        when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
        when(entregaRepository.save(any(Entrega.class))).thenAnswer(i -> i.getArguments()[0]);

        Entrega resultado = sistemaLogistica.finalizarEntrega(1L);

        assertNotNull(resultado);
        assertEquals(StatusEntrega.ENTREGUE, resultado.getStatus());
        verify(entregaRepository, times(1)).save(entrega);
    }
}