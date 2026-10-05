package com.logitech.sgfl.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/**
 * Coordenadas de rota de uma entrega (origem e destino), preenchidas a
 * partir da geocodificação dos endereços.
 *
 * Regras: campo nulo mantém o valor atual; latitude e longitude do mesmo
 * ponto precisam vir juntos; ao menos um dos pontos é obrigatório.
 */
public record CoordenadasEntregaRequest(
        @DecimalMin(value = "-90")
        @DecimalMax(value = "90")
        Double latitudeOrigem,

        @DecimalMin(value = "-180")
        @DecimalMax(value = "180")
        Double longitudeOrigem,

        @DecimalMin(value = "-90")
        @DecimalMax(value = "90")
        Double latitudeDestino,

        @DecimalMin(value = "-180")
        @DecimalMax(value = "180")
        Double longitudeDestino
) {}
