package com.logitech.sgfl.dto;

import com.logitech.sgfl.me.Produto;
import java.math.BigDecimal;

public record ProdutoResponse(
        Long id, String codigo, String nome, String descricao, BigDecimal preco,
        boolean ativo, EstoqueResponse estoque
) {
    public record EstoqueResponse(Long id, int quantidadeDisponivel) {}

    public static ProdutoResponse from(Produto p) {
        var estoque = p.getEstoque();
        return new ProdutoResponse(p.getId(), p.getCodigo(), p.getNome(), p.getDescricao(), p.getPreco(), p.isAtivo(),
                estoque == null ? null : new EstoqueResponse(estoque.getId(), estoque.getQuantidadeDisponivel()));
    }
}
