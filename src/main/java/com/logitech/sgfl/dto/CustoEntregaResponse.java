package com.logitech.sgfl.dto;

import com.logitech.sgfl.me.CustoEntrega;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustoEntregaResponse(
        Long id,
        Long entregaId,
        String tipo,
        String descricao,
        BigDecimal valor,
        LocalDateTime criadoEm
) {
    public static CustoEntregaResponse from(CustoEntrega c) {
        return new CustoEntregaResponse(
                c.getId(), c.getEntrega().getId(), c.getTipo(), c.getDescricao(), c.getValor(), c.getCriadoEm()
        );
    }
}
