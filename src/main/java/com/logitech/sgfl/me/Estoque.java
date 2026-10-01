package com.logitech.sgfl.me;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "estoque",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_estoque_produto",
                        columnNames = "produto_id"
                )
        }
)
public class Estoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonBackReference("produto-estoque")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "produto_id",
            nullable = false,
            unique = true
    )
    private Produto produto;

    @Column(
            name = "quantidade_disponivel",
            nullable = false
    )
    private int quantidadeDisponivel;

    @Column(
            name = "atualizado_em",
            nullable = false
    )
    private LocalDateTime atualizadoEm;

    public Estoque() {
    }

    public Estoque(
            Produto produto,
            int quantidadeDisponivel
    ) {
        this.produto = produto;
        this.quantidadeDisponivel = quantidadeDisponivel;
        this.atualizadoEm = LocalDateTime.now();
    }

    @PrePersist
    @PreUpdate
    private void atualizarData() {
        atualizadoEm = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public int getQuantidadeDisponivel() {
        return quantidadeDisponivel;
    }

    public void setQuantidadeDisponivel(
            int quantidadeDisponivel
    ) {
        this.quantidadeDisponivel = quantidadeDisponivel;
        this.atualizadoEm = LocalDateTime.now();
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}