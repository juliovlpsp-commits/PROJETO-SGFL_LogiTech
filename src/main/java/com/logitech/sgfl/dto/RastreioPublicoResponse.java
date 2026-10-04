package com.logitech.sgfl.dto;

import com.logitech.sgfl.enums.StatusEntrega;
import com.logitech.sgfl.me.Entrega;

import java.time.LocalDateTime;

public record RastreioPublicoResponse(
        String codigoRastreio,
        Long entregaId,
        String descricao,
        String origem,
        String destino,
        StatusEntrega status,
        LocalDateTime agendadaInicio,
        LocalDateTime agendadaFim,
        LocalDateTime iniciadaEm,
        LocalDateTime entregueEm,
        String veiculo,
        String motorista
) {
    public static RastreioPublicoResponse from(Entrega e) {
        return new RastreioPublicoResponse(
                e.getCodigoRastreio(),
                e.getId(),
                e.getDescricao(),
                e.getEnderecoOrigem(),
                e.getEnderecoDestino(),
                e.getStatus(),
                e.getAgendadaInicio(),
                e.getAgendadaFim(),
                e.getIniciadaEm(),
                e.getEntregueEm(),
                e.getVeiculo() == null ? null : e.getVeiculo().getPlaca(),
                e.getMotorista() == null ? null : e.getMotorista().getNome()
        );
    }
}
