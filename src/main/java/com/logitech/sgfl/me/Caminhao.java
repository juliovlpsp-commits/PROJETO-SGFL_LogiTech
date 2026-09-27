package com.logitech.sgfl.me;

import jakarta.persistence.Entity;

@Entity
public class Caminhao extends Veiculo {
    private int quantidadeEixos;

    public Caminhao() {}

    public Caminhao(String placa, String modelo, double capacidadeCargaKg, int quantidadeEixos) {
        super(placa, modelo, capacidadeCargaKg);
        this.quantidadeEixos = quantidadeEixos;
    }

    public int getQuantidadeEixos() { return quantidadeEixos; }
    public void setQuantidadeEixos(int quantidadeEixos) { this.quantidadeEixos = quantidadeEixos; }
}