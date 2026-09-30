package com.logitech.sgfl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class FurgaoRequest {

    @NotBlank(message = "A placa é obrigatória")
    private String placa;

    @NotBlank(message = "O modelo é obrigatório")
    private String modelo;

    @Positive(message = "A capacidade de carga deve ser maior que zero")
    private double capacidadeCargaKg;

    @Positive(message = "O volume do furgão deve ser maior que zero")
    private double volumeM3;

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public double getCapacidadeCargaKg() { return capacidadeCargaKg; }
    public void setCapacidadeCargaKg(double capacidadeCargaKg) { this.capacidadeCargaKg = capacidadeCargaKg; }
    public double getVolumeM3() { return volumeM3; }
    public void setVolumeM3(double volumeM3) { this.volumeM3 = volumeM3; }
}
