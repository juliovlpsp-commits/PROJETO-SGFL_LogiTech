package com.logitech.sgfl.dto;

import com.logitech.sgfl.me.ComprovanteEntrega;

import java.time.LocalDateTime;

public record ComprovanteEntregaResponse(
        Long id,
        Long entregaId,
        String fotoPath,
        boolean possuiAssinatura,
        String nomeRecebedor,
        LocalDateTime recebidoEm,
        String observacao
) {
    public static ComprovanteEntregaResponse from(ComprovanteEntrega comprovante) {
        return new ComprovanteEntregaResponse(
                comprovante.getId(),
                comprovante.getEntrega().getId(),
                comprovante.getFotoPath(),
                comprovante.getAssinatura() != null && !comprovante.getAssinatura().isBlank(),
                comprovante.getNomeRecebedor(),
                comprovante.getRecebidoEm(),
                comprovante.getObservacao()
        );
    }
}
