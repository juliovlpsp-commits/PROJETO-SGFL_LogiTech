package com.logitech.sgfl.dto;

import com.logitech.sgfl.enums.StatusPedido;
import com.logitech.sgfl.me.Pedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(Long id, ClienteResumo cliente, StatusPedido status, LocalDateTime criadoEm, List<ItemResumo> itens) {
    public record ClienteResumo(Long id, String nome, String email) {}
    public record ProdutoResumo(Long id, String codigo, String nome) {}
    public record ItemResumo(Long id, ProdutoResumo produto, int quantidade, BigDecimal precoUnitario) {}

    public static PedidoResponse from(Pedido p) {
        var c = p.getCliente();
        var itens = p.getItens().stream().map(i -> new ItemResumo(i.getId(),
                new ProdutoResumo(i.getProduto().getId(), i.getProduto().getCodigo(), i.getProduto().getNome()),
                i.getQuantidade(), i.getPrecoUnitario())).toList();
        return new PedidoResponse(p.getId(), new ClienteResumo(c.getId(), c.getNome(), c.getEmail()),
                p.getStatus(), p.getCriadoEm(), itens);
    }
}
