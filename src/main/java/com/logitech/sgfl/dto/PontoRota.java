package com.logitech.sgfl.dto;

/**
 * Ponto da geometria real de uma rota, usado para desenhar a linha
 * da rota no mapa (Leaflet) em vez de uma reta entre origem e destino.
 */
public record PontoRota(double latitude, double longitude) {}
