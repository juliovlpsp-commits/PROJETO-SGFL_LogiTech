package com.logitech.sgfl.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CustoEntregaRequest(
        @NotBlank String tipo,
        String descricao,
        @NotNull @DecimalMin(value = "0.00") BigDecimal valor
) {}
