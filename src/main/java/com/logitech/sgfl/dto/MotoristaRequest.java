package com.logitech.sgfl.dto;

import com.logitech.sgfl.enums.TipoCNH;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Contrato de entrada da API para criar/atualizar um motorista.
 * Mantido separado da entidade JPA de propósito (o cliente da API nunca deve
 * conseguir setar diretamente um campo interno só porque ele existe na entidade).
 */
public class MotoristaRequest {

    @NotBlank(message = "O nome do motorista é obrigatório")
    private String nome;

    @NotBlank(message = "O CPF do motorista é obrigatório")
    private String cpf;

    @NotNull(message = "O tipo da CNH é obrigatório")
    private TipoCNH tipoCNH;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public TipoCNH getTipoCNH() { return tipoCNH; }
    public void setTipoCNH(TipoCNH tipoCNH) { this.tipoCNH = tipoCNH; }
}
