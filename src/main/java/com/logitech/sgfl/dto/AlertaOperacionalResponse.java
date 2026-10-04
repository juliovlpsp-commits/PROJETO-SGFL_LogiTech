package com.logitech.sgfl.dto;

public record AlertaOperacionalResponse(
        String id,
        String severidade,
        String titulo,
        String descricao,
        Long entregaId
) {}
