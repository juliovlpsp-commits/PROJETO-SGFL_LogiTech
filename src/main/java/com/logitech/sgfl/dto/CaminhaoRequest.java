package com.logitech.sgfl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class CaminhaoRequest {

    @NotBlank(message = "A placa é obrigatória")
    private String placa;

    @NotBlank(message = "O modelo é obrigatório")
    private String modelo;

    @Positive(message = "A capacidade de carga deve ser maior que zero")
    private double capacidadeCargaKg;

    @Positive(message = "A quantidade de eixos deve ser maior que zero")
    private int quantidadeEixos;

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public double getCapacidadeCargaKg() { return capacidadeCargaKg; }
    public void setCapacidadeCargaKg(double capacidadeCargaKg) { this.capacidadeCargaKg = capacidadeCargaKg; }
    public int getQuantidadeEixos() { return quantidadeEixos; }
    public void setQuantidadeEixos(int quantidadeEixos) { this.quantidadeEixos = quantidadeEixos; }
}
