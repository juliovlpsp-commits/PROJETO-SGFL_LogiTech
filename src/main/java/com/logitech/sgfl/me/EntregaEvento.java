package com.logitech.sgfl.me;

import com.logitech.sgfl.enums.StatusEntrega;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "entrega_evento", indexes = {
        @Index(name = "idx_entrega_evento_entrega", columnList = "entrega_id, ocorrido_em")
})
public class EntregaEvento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrega_id", nullable = false)
    private Entrega entrega;

    @Column(nullable = false, length = 40)
    private String tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 30)
    private StatusEntrega statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", length = 30)
    private StatusEntrega statusNovo;

    @Column(name = "ocorrido_em", nullable = false)
    private LocalDateTime ocorridoEm;

    @Column(length = 150)
    private String responsavel;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    public EntregaEvento() {}

    public EntregaEvento(Entrega entrega, String tipo, StatusEntrega anterior, StatusEntrega novo,
                         LocalDateTime ocorridoEm, String responsavel, String observacao) {
        this.entrega = entrega;
        this.tipo = tipo;
        this.statusAnterior = anterior;
        this.statusNovo = novo;
        this.ocorridoEm = ocorridoEm;
        this.responsavel = responsavel;
        this.observacao = observacao;
    }

    public Long getId() { return id; }
    public Entrega getEntrega() { return entrega; }
    public String getTipo() { return tipo; }
    public StatusEntrega getStatusAnterior() { return statusAnterior; }
    public StatusEntrega getStatusNovo() { return statusNovo; }
    public LocalDateTime getOcorridoEm() { return ocorridoEm; }
    public String getResponsavel() { return responsavel; }
    public String getObservacao() { return observacao; }
}
