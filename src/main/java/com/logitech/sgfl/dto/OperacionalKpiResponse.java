package com.logitech.sgfl.dto;

import java.math.BigDecimal;

public record OperacionalKpiResponse(
        long entregasHoje,
        long atrasadas,
        long concluidasHoje,
        double pesoTransportadoKg,
        long motoristasAtivos,
        long veiculosDisponiveis,
        BigDecimal faturamentoHoje,
        BigDecimal custosHoje,
        BigDecimal margemHoje,
        String motoristaMaisAtivo
) {}
