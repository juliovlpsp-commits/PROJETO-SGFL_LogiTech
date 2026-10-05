package com.logitech.sgfl.dto;

import java.time.LocalDateTime;

/**
 * Estimativa de rota de uma entrega.
 *
 * {@code fonte} diz como o cálculo foi feito: {@code OSRM} (rota real),
 * {@code HAVERSINE} (linha reta) ou nulo quando não há coordenadas.
 */
public record RotaEstimativaResponse(
        Long entregaId,
        Double distanciaKm,
        Integer velocidadeMediaKmH,
        Integer duracaoMinutos,
        LocalDateTime previsaoChegada,
        boolean possuiCoordenadas,
        String fonte
) {}
