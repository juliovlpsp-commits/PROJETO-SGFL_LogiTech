package com.logitech.sgfl.dto;

import com.logitech.sgfl.enums.StatusEntrega;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Contrato de entrada da API para criar uma entrega.
 * Mantido separado da entidade JPA de propósito: o cliente nunca deve conseguir
 * setar diretamente campos internos (ex: id, veiculo, motorista) só porque
 * eles existem na entidade — isso é o problema conhecido como "over-posting".
 */
public class EntregaRequest {

    @NotBlank(message = "A descrição é obrigatória")
    private String descricao;

    @NotBlank(message = "O endereço de destino é obrigatório")
    private String enderecoDestino;

    private String enderecoOrigem;

    private double pesoCargaKg;

    @NotNull(message = "O estado inicial é obrigatório")
    private StatusEntrega status;

    private LocalDateTime agendadaInicio;
    private LocalDateTime agendadaFim;
    private Double latitudeOrigem;
    private Double longitudeOrigem;
    private Double latitudeDestino;
    private Double longitudeDestino;
    private BigDecimal valorFrete = BigDecimal.ZERO;

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getEnderecoDestino() { return enderecoDestino; }
    public void setEnderecoDestino(String enderecoDestino) { this.enderecoDestino = enderecoDestino; }
    public String getEnderecoOrigem() { return enderecoOrigem; }
    public void setEnderecoOrigem(String enderecoOrigem) { this.enderecoOrigem = enderecoOrigem; }
    public double getPesoCargaKg() { return pesoCargaKg; }
    public void setPesoCargaKg(double pesoCargaKg) { this.pesoCargaKg = pesoCargaKg; }
    public StatusEntrega getStatus() { return status; }
    public void setStatus(StatusEntrega status) { this.status = status; }
    public LocalDateTime getAgendadaInicio() { return agendadaInicio; }
    public void setAgendadaInicio(LocalDateTime value) { this.agendadaInicio = value; }
    public LocalDateTime getAgendadaFim() { return agendadaFim; }
    public void setAgendadaFim(LocalDateTime value) { this.agendadaFim = value; }
    public Double getLatitudeOrigem() { return latitudeOrigem; }
    public void setLatitudeOrigem(Double value) { this.latitudeOrigem = value; }
    public Double getLongitudeOrigem() { return longitudeOrigem; }
    public void setLongitudeOrigem(Double value) { this.longitudeOrigem = value; }
    public Double getLatitudeDestino() { return latitudeDestino; }
    public void setLatitudeDestino(Double value) { this.latitudeDestino = value; }
    public Double getLongitudeDestino() { return longitudeDestino; }
    public void setLongitudeDestino(Double value) { this.longitudeDestino = value; }
    public BigDecimal getValorFrete() { return valorFrete; }
    public void setValorFrete(BigDecimal value) { this.valorFrete = value; }
}
