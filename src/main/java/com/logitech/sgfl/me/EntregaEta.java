package com.logitech.sgfl.me;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Histórico de previsões de ETA de uma entrega.
 *
 * Cada vez que o sistema calcula a rota (dentro de uma janela de 5
 * minutos) ele guarda aqui distância, duração, previsão de chegada e a
 * fonte do cálculo — permitindo comparar a previsão feita no início da
 * operação com a entregue de verdade.
 */
@Entity
@Table(name = "entrega_eta")
public class EntregaEta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entrega_id", nullable = false)
    private Entrega entrega;

    @Column(name = "distancia_km")
    private Double distanciaKm;

    @Column(name = "duracao_minutos")
    private Integer duracaoMinutos;

    @Column(name = "previsao_chegada")
    private LocalDateTime previsaoChegada;

    @Column(nullable = false, length = 20)
    private String fonte;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    public EntregaEta() {}

    public EntregaEta(
            Entrega entrega,
            Double distanciaKm,
            Integer duracaoMinutos,
            LocalDateTime previsaoChegada,
            String fonte,
            LocalDateTime criadoEm
    ) {
        this.entrega = entrega;
        this.distanciaKm = distanciaKm;
        this.duracaoMinutos = duracaoMinutos;
        this.previsaoChegada = previsaoChegada;
        this.fonte = fonte;
        this.criadoEm = criadoEm;
    }

    public Long getId() { return id; }
    public Entrega getEntrega() { return entrega; }
    public Double getDistanciaKm() { return distanciaKm; }
    public Integer getDuracaoMinutos() { return duracaoMinutos; }
    public LocalDateTime getPrevisaoChegada() { return previsaoChegada; }
    public String getFonte() { return fonte; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
