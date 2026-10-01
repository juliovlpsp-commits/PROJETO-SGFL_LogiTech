package com.logitech.sgfl.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ClienteRequest {

    @NotBlank(message = "O nome do cliente é obrigatório")
    @Size(max = 255)
    private String nome;

    @NotBlank(message = "O CPF do cliente é obrigatório")
    private String cpf;

    @NotBlank(message = "O e-mail do cliente é obrigatório")
    @Email(message = "E-mail do cliente inválido")
    @Size(max = 255)
    private String email;

    @Size(max = 30)
    private String telefone;

    @Size(max = 20)
    private String cep;

    @Size(max = 255)
    private String logradouro;

    @Size(max = 30)
    private String numero;

    @Size(max = 255)
    private String complemento;

    @Size(max = 255)
    private String bairro;

    @Size(max = 255)
    private String cidade;

    @Pattern(
            regexp = "^$|^[A-Za-z]{2}$",
            message = "UF deve possuir 2 letras"
    )
    private String uf;

    public ClienteRequest() {
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public void setLogradouro(String logradouro) {
        this.logradouro = logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public void setComplemento(String complemento) {
        this.complemento = complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }
}