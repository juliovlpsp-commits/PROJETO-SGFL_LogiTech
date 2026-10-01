package com.logitech.sgfl.me;
import jakarta.persistence.Column;

import jakarta.persistence.Entity;

@Entity
public class Furgao extends Veiculo {

    @Column(name = "volume_m3")
    private double volumeM3;

    public Furgao() {}

    public Furgao(String placa, String modelo, double capacidadeCargaKg, double volumeM3) {
        super(placa, modelo, capacidadeCargaKg);
        this.volumeM3 = volumeM3;
    }

    public double getVolumeM3() { return volumeM3; }
    public void setVolumeM3(double volumeM3) { this.volumeM3 = volumeM3; }
}