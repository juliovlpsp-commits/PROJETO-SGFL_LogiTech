package com.logitech.sgfl.service;

/**
 * Resultado do cálculo de rota entre dois pontos.
 *
 * {@code fonte} diz qual provedor calculou: {@code OSRM} quando houve
 * roteamento real nas estradas, {@code HAVERSINE} quando caiu no cálculo
 * em linha reta (padrão, provedor desligado ou indisponível).
 */
public record RotaCalculada(
        double distanciaKm,
        int duracaoMinutos,
        String fonte
) {

    public static final String FONTE_OSRM = "OSRM";
    public static final String FONTE_HAVERSINE = "HAVERSINE";
}
