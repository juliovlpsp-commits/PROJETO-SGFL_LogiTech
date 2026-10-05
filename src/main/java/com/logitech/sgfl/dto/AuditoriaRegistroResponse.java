package com.logitech.sgfl.dto;

import com.logitech.sgfl.me.AuditoriaRegistro;

import java.time.LocalDateTime;

public record AuditoriaRegistroResponse(
        Long id,
        String entidade,
        Long entidadeId,
        String acao,
        String descricao,
        String dadosAntes,
        String dadosDepois,
        String usuario,
        LocalDateTime criadoEm
) {
    public static AuditoriaRegistroResponse from(AuditoriaRegistro registro) {
        return new AuditoriaRegistroResponse(
                registro.getId(),
                registro.getEntidade(),
                registro.getEntidadeId(),
                registro.getAcao(),
                registro.getDescricao(),
                registro.getDadosAntes(),
                registro.getDadosDepois(),
                registro.getUsuario(),
                registro.getCriadoEm()
        );
    }
}
