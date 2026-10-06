package com.logitech.sgfl.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Estimativa de rota de uma entrega.
 *
 * {@code fonte} diz como o cálculo foi feito: {@code OSRM} (rota real),
 * {@code TOMTOM} (rota real com trânsito em tempo real), {@code HAVERSINE}
 * (linha reta) ou nulo quando não há coordenadas.
 *
 * {@code geometria} traz os pontos da rota real para o mapa desenhar a
 * linha nas estradas; nulo quando não há coordenadas.
 */
public record RotaEstimativaResponse(
        Long entregaId,
        Double distanciaKm,
        Integer velocidadeMediaKmH,
        Integer duracaoMinutos,
        LocalDateTime previsaoChegada,
        boolean possuiCoordenadas,
        String fonte,
        List<PontoRota> geometria
) {}
