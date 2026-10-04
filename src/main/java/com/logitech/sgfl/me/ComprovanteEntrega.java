package com.logitech.sgfl.me;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "comprovante_entrega")
public class ComprovanteEntrega {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrega_id", nullable = false, unique = true)
    private Entrega entrega;

    @Column(name = "foto_path", length = 500)
    private String fotoPath;

    @Column(columnDefinition = "TEXT")
    private String assinatura;

    @Column(name = "nome_recebedor", nullable = false)
    private String nomeRecebedor;

    @Column(name = "recebido_em", nullable = false)
    private LocalDateTime recebidoEm;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    public ComprovanteEntrega() {}

    public Long getId() { return id; }
    public Entrega getEntrega() { return entrega; }
    public void setEntrega(Entrega entrega) { this.entrega = entrega; }
    public String getFotoPath() { return fotoPath; }
    public void setFotoPath(String fotoPath) { this.fotoPath = fotoPath; }
    public String getAssinatura() { return assinatura; }
    public void setAssinatura(String assinatura) { this.assinatura = assinatura; }
    public String getNomeRecebedor() { return nomeRecebedor; }
    public void setNomeRecebedor(String nomeRecebedor) { this.nomeRecebedor = nomeRecebedor; }
    public LocalDateTime getRecebidoEm() { return recebidoEm; }
    public void setRecebidoEm(LocalDateTime recebidoEm) { this.recebidoEm = recebidoEm; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}
