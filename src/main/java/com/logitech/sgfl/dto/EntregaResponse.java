package com.logitech.sgfl.dto;

import com.logitech.sgfl.enums.StatusEntrega;
import com.logitech.sgfl.me.Entrega;

public record EntregaResponse(
        Long id,
        String descricao,
        String enderecoOrigem,
        String enderecoDestino,
        double pesoCargaKg,
        StatusEntrega status,
        VeiculoResponse veiculo,
        MotoristaResponse motorista
) {
    public static EntregaResponse from(Entrega entrega) {
        return new EntregaResponse(entrega.getId(), entrega.getDescricao(), entrega.getEnderecoOrigem(),
                entrega.getEnderecoDestino(), entrega.getPesoCargaKg(), entrega.getStatus(),
                VeiculoResponse.from(entrega.getVeiculo()),
                entrega.getMotorista() == null ? null : MotoristaResponse.from(entrega.getMotorista()));
    }
}
