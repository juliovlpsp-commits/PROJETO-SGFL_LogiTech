package com.logitech.sgfl.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class EstoqueRequest {

    @NotNull(message = "A quantidade de estoque é obrigatória")
    @PositiveOrZero(
            message = "A quantidade de estoque não pode ser negativa"
    )
    private Integer quantidade;

    public EstoqueRequest() {
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }
}