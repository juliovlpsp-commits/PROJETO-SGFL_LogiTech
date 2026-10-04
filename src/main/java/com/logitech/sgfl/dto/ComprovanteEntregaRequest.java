package com.logitech.sgfl.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record ComprovanteEntregaRequest(
        @NotBlank(message = "O nome de quem recebeu é obrigatório") String nomeRecebedor,
        String assinatura,
        LocalDateTime recebidoEm,
        String observacao
) {}
