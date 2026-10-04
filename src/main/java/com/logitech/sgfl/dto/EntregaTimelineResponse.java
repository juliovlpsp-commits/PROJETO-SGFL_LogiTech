package com.logitech.sgfl.dto;

import com.logitech.sgfl.enums.StatusEntrega;
import com.logitech.sgfl.me.EntregaEvento;

import java.time.LocalDateTime;

public record EntregaTimelineResponse(
        Long id,
        String tipo,
        StatusEntrega statusAnterior,
        StatusEntrega statusNovo,
        LocalDateTime ocorridoEm,
        String responsavel,
        String observacao
) {
    public static EntregaTimelineResponse from(EntregaEvento evento) {
        return new EntregaTimelineResponse(
                evento.getId(),
                evento.getTipo(),
                evento.getStatusAnterior(),
                evento.getStatusNovo(),
                evento.getOcorridoEm(),
                evento.getResponsavel(),
                evento.getObservacao()
        );
    }
}
