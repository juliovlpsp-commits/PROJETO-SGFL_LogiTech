package com.logitech.sgfl.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.ArrayList;
import java.util.List;

public class PedidoRequest {

    @NotNull(message = "O cliente é obrigatório")
    private Long clienteId;

    @NotEmpty(message = "O pedido deve possuir pelo menos um produto")
    @Valid
    private List<ItemRequest> itens =
            new ArrayList<>();

    public PedidoRequest() {
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public List<ItemRequest> getItens() {
        return itens;
    }

    public void setItens(List<ItemRequest> itens) {
        this.itens = itens;
    }

    public static class ItemRequest {

        @NotNull
        private Long produtoId;

        @NotNull
        @Positive(
                message = "A quantidade deve ser maior que zero"
        )
        private Integer quantidade;

        public ItemRequest() {
        }

        public Long getProdutoId() {
            return produtoId;
        }

        public void setProdutoId(Long produtoId) {
            this.produtoId = produtoId;
        }

        public Integer getQuantidade() {
            return quantidade;
        }

        public void setQuantidade(Integer quantidade) {
            this.quantidade = quantidade;
        }
    }
}