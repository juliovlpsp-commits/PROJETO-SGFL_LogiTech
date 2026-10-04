package com.logitech.sgfl.dto;

import com.logitech.sgfl.enums.TipoCNH;
import com.logitech.sgfl.me.Motorista;

public record MotoristaResponse(Long id, String nome, String cpf, TipoCNH tipoCNH) {
    public static MotoristaResponse from(Motorista motorista) {
        return new MotoristaResponse(motorista.getId(), motorista.getNome(), motorista.getCpf(), motorista.getTipoCNH());
    }
}
