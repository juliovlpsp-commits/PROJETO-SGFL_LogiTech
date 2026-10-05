package com.logitech.sgfl.me;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Registro da auditoria transversal (cliente, produto e pedido).
 *
 * A linha do tempo de entrega fica em {@link EntregaEvento}; esta tabela
 * responde "quem alterou o quê" fora do fluxo operacional de entregas.
 */
@Entity
@Table(name = "auditoria_registro")
public class AuditoriaRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String entidade;

    @Column(name = "entidade_id")
    private Long entidadeId;

    @Column(nullable = false, length = 30)
    private String acao;

    @Column(length = 255)
    private String descricao;

    @Column(name = "dados_antes", columnDefinition = "TEXT")
    private String dadosAntes;

    @Column(name = "dados_depois", columnDefinition = "TEXT")
    private String dadosDepois;

    @Column(length = 120)
    private String usuario;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    public AuditoriaRegistro() {}

    public Long getId() { return id; }
    public String getEntidade() { return entidade; }
    public void setEntidade(String entidade) { this.entidade = entidade; }
    public Long getEntidadeId() { return entidadeId; }
    public void setEntidadeId(Long entidadeId) { this.entidadeId = entidadeId; }
    public String getAcao() { return acao; }
    public void setAcao(String acao) { this.acao = acao; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getDadosAntes() { return dadosAntes; }
    public void setDadosAntes(String dadosAntes) { this.dadosAntes = dadosAntes; }
    public String getDadosDepois() { return dadosDepois; }
    public void setDadosDepois(String dadosDepois) { this.dadosDepois = dadosDepois; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
