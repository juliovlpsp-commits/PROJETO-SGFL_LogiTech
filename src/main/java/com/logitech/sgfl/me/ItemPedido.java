package com.logitech.sgfl.me;
import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "item_pedido",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_item_pedido_produto",
                        columnNames = {
                                "pedido_id",
                                "produto_id"
                        }
                )
        }
)
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonBackReference("pedido-itens")
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "pedido_id",
            nullable = false
    )
    private Pedido pedido;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "produto_id",
            nullable = false
    )
    private Produto produto;

    @Column(nullable = false)
    private int quantidade;

    @Column(
            name = "preco_unitario",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal precoUnitario;

    public ItemPedido() {
    }

    public ItemPedido(
            Produto produto,
            int quantidade,
            BigDecimal precoUnitario
    ) {
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }

    public Long getId() {
        return id;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public Produto getProduto() {
        return produto;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }
}