package com.logitech.sgfl.dto;

import java.time.LocalDateTime;

public record RotaEstimativaResponse(
        Long entregaId,
        Double distanciaKm,
        Integer velocidadeMediaKmH,
        Integer duracaoMinutos,
        LocalDateTime previsaoChegada,
        boolean possuiCoordenadas
) {}
