package com.logitech.sgfl.dto;

import com.logitech.sgfl.me.Caminhao;
import com.logitech.sgfl.me.Furgao;
import com.logitech.sgfl.me.Veiculo;

public record VeiculoResponse(
        Long id,
        String placa,
        String modelo,
        double capacidadeCargaKg,
        String tipo,
        Integer quantidadeEixos,
        Double volumeM3
) {
    public static VeiculoResponse from(Veiculo veiculo) {
        if (veiculo == null) return null;
        if (veiculo instanceof Caminhao caminhao) {
            return new VeiculoResponse(veiculo.getId(), veiculo.getPlaca(), veiculo.getModelo(),
                    veiculo.getCapacidadeCargaKg(), "CAMINHAO", caminhao.getQuantidadeEixos(), null);
        }
        if (veiculo instanceof Furgao furgao) {
            return new VeiculoResponse(veiculo.getId(), veiculo.getPlaca(), veiculo.getModelo(),
                    veiculo.getCapacidadeCargaKg(), "FURGAO", null, furgao.getVolumeM3());
        }
        return new VeiculoResponse(veiculo.getId(), veiculo.getPlaca(), veiculo.getModelo(),
                veiculo.getCapacidadeCargaKg(), "VEICULO", null, null);
    }
}
