package com.logitech.sgfl.me;

import com.logitech.sgfl.enums.StatusEntrega;
import jakarta.persistence.*;

@Entity
public class Entrega {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String enderecoOrigem;
    private String enderecoDestino;
    private double pesoCargaKg;

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
}