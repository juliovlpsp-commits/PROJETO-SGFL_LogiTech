package com.logitech.sgfl.dto;

/**
 * Resultado de geocodificação (endereço -> coordenada ou o inverso).
 *
 * {@code fonte} identifica o provedor usado (NOMINATIM) para o usuário
 * saber que o endereço veio de um serviço externo.
 */
public record GeocodificacaoResponse(
        Double latitude,
        Double longitude,
        String endereco,
        String fonte
) {}
