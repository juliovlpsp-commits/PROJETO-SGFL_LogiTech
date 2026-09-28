package com.logitech.sgfl.dto;

import com.logitech.sgfl.enums.StatusEntrega;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
}
