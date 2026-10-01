package com.logitech.sgfl.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class ProdutoRequest {

    @NotBlank(message = "O código do produto é obrigatório")
    @Size(max = 50)
    private String codigo;

    @NotBlank(message = "O nome do produto é obrigatório")
    @Size(max = 255)
    private String nome;

    @Size(max = 5000)
    private String descricao;

    @NotNull(message = "O preço é obrigatório")
    @DecimalMin(
            value = "0.00",
            message = "O preço não pode ser negativo"
    )
    private BigDecimal preco;

    @NotNull(message = "O estoque inicial é obrigatório")
    @PositiveOrZero(
            message = "O estoque inicial não pode ser negativo"
    )
    private Integer quantidadeEstoqueInicial;

    private boolean ativo = true;

    public ProdutoRequest() {
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public Integer getQuantidadeEstoqueInicial() {
        return quantidadeEstoqueInicial;
    }

    public void setQuantidadeEstoqueInicial(
            Integer quantidadeEstoqueInicial
    ) {
        this.quantidadeEstoqueInicial =
                quantidadeEstoqueInicial;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}