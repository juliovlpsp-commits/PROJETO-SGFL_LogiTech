package com.logitech.sgfl.dto;

import com.logitech.sgfl.me.Cliente;

public record ClienteResponse(
        Long id, String nome, String cpf, String email, String telefone, String cep,
        String logradouro, String numero, String complemento, String bairro,
        String cidade, String uf, boolean ativo
) {
    public static ClienteResponse from(Cliente c) {
        return new ClienteResponse(c.getId(), c.getNome(), c.getCpf(), c.getEmail(), c.getTelefone(), c.getCep(),
                c.getLogradouro(), c.getNumero(), c.getComplemento(), c.getBairro(), c.getCidade(), c.getUf(), c.isAtivo());
    }
}
