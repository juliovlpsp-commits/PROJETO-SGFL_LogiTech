package com.logitech.sgfl.dto;

import com.logitech.sgfl.me.EntregaEta;

import java.time.LocalDateTime;

public record EntregaEtaResponse(
        Long id,
        Double distanciaKm,
        Integer duracaoMinutos,
        LocalDateTime previsaoChegada,
        String fonte,
        LocalDateTime criadoEm
) {
    public static EntregaEtaResponse from(EntregaEta eta) {
        return new EntregaEtaResponse(
                eta.getId(),
                eta.getDistanciaKm(),
                eta.getDuracaoMinutos(),
                eta.getPrevisaoChegada(),
                eta.getFonte(),
                eta.getCriadoEm()
        );
    }
}
