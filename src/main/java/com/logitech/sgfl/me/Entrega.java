package com.logitech.sgfl.me;

import com.logitech.sgfl.enums.StatusEntrega;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(indexes = {
        @Index(name = "idx_entrega_status", columnList = "status")
})
public class Entrega {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descricao;
    private String enderecoOrigem;
    private String enderecoDestino;
    private double pesoCargaKg;

    @Column(name = "codigo_rastreio", nullable = false, unique = true, length = 40)
    private String codigoRastreio;

    @Column(name = "agendada_inicio")
    private LocalDateTime agendadaInicio;

    @Column(name = "agendada_fim")
    private LocalDateTime agendadaFim;

    @Column(name = "iniciada_em")
    private LocalDateTime iniciadaEm;

    @Column(name = "entregue_em")
    private LocalDateTime entregueEm;

    @Column(name = "latitude_origem", precision = 9, scale = 6)
    private Double latitudeOrigem;

    @Column(name = "longitude_origem", precision = 9, scale = 6)
    private Double longitudeOrigem;

    @Column(name = "latitude_destino", precision = 9, scale = 6)
    private Double latitudeDestino;

    @Column(name = "longitude_destino", precision = 9, scale = 6)
    private Double longitudeDestino;

    @Column(name = "valor_frete", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorFrete = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private StatusEntrega status;

    @ManyToOne
    private Veiculo veiculo;

    @ManyToOne
    private Motorista motorista;

    public Entrega() {}

    public Entrega(String enderecoOrigem, String enderecoDestino, double pesoCargaKg) {
        this.enderecoOrigem = enderecoOrigem;
        this.enderecoDestino = enderecoDestino;
        this.pesoCargaKg = pesoCargaKg;
        this.status = StatusEntrega.PENDENTE;
    }

    public Long getId() { return id; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getEnderecoOrigem() { return enderecoOrigem; }
    public void setEnderecoOrigem(String enderecoOrigem) { this.enderecoOrigem = enderecoOrigem; }
    public String getEnderecoDestino() { return enderecoDestino; }
    public void setEnderecoDestino(String enderecoDestino) { this.enderecoDestino = enderecoDestino; }
    public double getPesoCargaKg() { return pesoCargaKg; }
    public void setPesoCargaKg(double pesoCargaKg) { this.pesoCargaKg = pesoCargaKg; }
    public StatusEntrega getStatus() { return status; }
    public void setStatus(StatusEntrega status) { this.status = status; }
    public Veiculo getVeiculo() { return veiculo; }
    public void setVeiculo(Veiculo veiculo) { this.veiculo = veiculo; }
    public Motorista getMotorista() { return motorista; }
    public void setMotorista(Motorista motorista) { this.motorista = motorista; }

    public String getCodigoRastreio() { return codigoRastreio; }
    public void setCodigoRastreio(String codigoRastreio) { this.codigoRastreio = codigoRastreio; }
    public LocalDateTime getAgendadaInicio() { return agendadaInicio; }
    public void setAgendadaInicio(LocalDateTime agendadaInicio) { this.agendadaInicio = agendadaInicio; }
    public LocalDateTime getAgendadaFim() { return agendadaFim; }
    public void setAgendadaFim(LocalDateTime agendadaFim) { this.agendadaFim = agendadaFim; }
    public LocalDateTime getIniciadaEm() { return iniciadaEm; }
    public void setIniciadaEm(LocalDateTime iniciadaEm) { this.iniciadaEm = iniciadaEm; }
    public LocalDateTime getEntregueEm() { return entregueEm; }
    public void setEntregueEm(LocalDateTime entregueEm) { this.entregueEm = entregueEm; }
    public Double getLatitudeOrigem() { return latitudeOrigem; }
    public void setLatitudeOrigem(Double latitudeOrigem) { this.latitudeOrigem = latitudeOrigem; }
    public Double getLongitudeOrigem() { return longitudeOrigem; }
    public void setLongitudeOrigem(Double longitudeOrigem) { this.longitudeOrigem = longitudeOrigem; }
    public Double getLatitudeDestino() { return latitudeDestino; }
    public void setLatitudeDestino(Double latitudeDestino) { this.latitudeDestino = latitudeDestino; }
    public Double getLongitudeDestino() { return longitudeDestino; }
    public void setLongitudeDestino(Double longitudeDestino) { this.longitudeDestino = longitudeDestino; }
    public BigDecimal getValorFrete() { return valorFrete; }
    public void setValorFrete(BigDecimal valorFrete) { this.valorFrete = valorFrete == null ? BigDecimal.ZERO : valorFrete; }
}
