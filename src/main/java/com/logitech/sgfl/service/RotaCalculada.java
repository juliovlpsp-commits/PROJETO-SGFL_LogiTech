package com.logitech.sgfl.service;

import com.logitech.sgfl.dto.PontoRota;

import java.util.List;

/**
 * Resultado do cálculo de rota entre dois pontos.
 *
 * {@code fonte} diz qual provedor calculou: {@code OSRM} quando houve
 * roteamento real nas estradas, {@code TOMTOM} quando o provedor com
 * trânsito em tempo real respondeu, {@code HAVERSINE} quando caiu no
 * cálculo em linha reta (padrão, provedor desligado ou indisponível).
 *
 * {@code geometria} carrega os pontos da linha real da rota (para o
 * mapa); nulo quando o provedor não devolveu forma da rota.
 */
public record RotaCalculada(
        double distanciaKm,
        int duracaoMinutos,
        String fonte,
        List<PontoRota> geometria
) {

    public static final String FONTE_OSRM = "OSRM";
    public static final String FONTE_TOMTOM = "TOMTOM";
    public static final String FONTE_HAVERSINE = "HAVERSINE";

    /**
     * Mantém as chamadas antigas (sem geometria) funcionando.
     */
    public RotaCalculada(double distanciaKm, int duracaoMinutos, String fonte) {
        this(distanciaKm, duracaoMinutos, fonte, null);
    }
}
