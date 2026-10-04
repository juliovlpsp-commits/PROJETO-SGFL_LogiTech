package com.logitech.sgfl.me;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "custo_entrega")
public class CustoEntrega {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrega_id", nullable = false)
    private Entrega entrega;

    @Column(nullable = false, length = 40)
    private String tipo;

    @Column(length = 255)
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    public CustoEntrega() {}

    public CustoEntrega(Entrega entrega, String tipo, String descricao, BigDecimal valor) {
        this.entrega = entrega;
        this.tipo = tipo;
        this.descricao = descricao;
        this.valor = valor;
        this.criadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Entrega getEntrega() { return entrega; }
    public String getTipo() { return tipo; }
    public String getDescricao() { return descricao; }
    public BigDecimal getValor() { return valor; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
