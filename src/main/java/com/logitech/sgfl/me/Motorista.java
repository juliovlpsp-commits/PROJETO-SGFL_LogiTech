package com.logitech.sgfl.me;

import com.logitech.sgfl.enums.TipoCNH;
import jakarta.persistence.*;

@Entity
public class Motorista {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String cpf;

    @Enumerated(EnumType.STRING)
    private TipoCNH tipoCNH;

    public Motorista() {}

    public Motorista(String nome, String cpf, TipoCNH tipoCNH) {
        this.nome = nome;
        this.cpf = cpf;
        this.tipoCNH = tipoCNH;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public TipoCNH getTipoCNH() { return tipoCNH; }
    public void setTipoCNH(TipoCNH tipoCNH) { this.tipoCNH = tipoCNH; }
}