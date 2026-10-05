package com.logitech.sgfl.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private ResponseEntity<Map<String, Object>> tratar(String mensagemDoBanco) {
        return handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("erro", new RuntimeException(mensagemDoBanco))
        );
    }

    @Test
    void deveTraduzirDuplaAlocacaoDeVeiculoParaMensagemClara() {
        ResponseEntity<Map<String, Object>> resposta =
                tratar("ERROR: duplicate key value violates unique constraint \"uq_entrega_veiculo_em_transito\"");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().get("message"))
                .isEqualTo("O veículo já está alocado em outra entrega EM_TRANSITO.");
    }

    @Test
    void deveTraduzirDuplaAlocacaoDeMotoristaParaMensagemClara() {
        ResponseEntity<Map<String, Object>> resposta =
                tratar("ERROR: duplicate key value violates unique constraint \"uq_entrega_motorista_em_transito\"");

        assertThat(resposta.getBody().get("message"))
                .isEqualTo("O motorista já está alocado em outra entrega EM_TRANSITO.");
    }

    @Test
    void deveTraduzirPlacaECpfDuplicados() {
        assertThat(tratar("violates unique constraint \"uk_veiculo_placa\"").getBody().get("message"))
                .isEqualTo("Já existe um veículo cadastrado com esta placa.");

        assertThat(tratar("violates unique constraint \"uk_motorista_cpf\"").getBody().get("message"))
                .isEqualTo("Já existe um motorista cadastrado com este CPF.");
    }

    @Test
    void deveManterMensagemGenericaParaOutrasViolacoes() {
        ResponseEntity<Map<String, Object>> resposta = tratar("violates foreign key constraint \"fk_qualquer\"");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat((String) resposta.getBody().get("message")).contains("relacionado a outros dados");
    }

    @Test
    void deveDevolver404ParaRotaInexistente() {
        ResponseEntity<Map<String, Object>> resposta =
                handler.handleNoResourceFound(
                        new NoResourceFoundException(HttpMethod.GET, "/api/auth/me")
                );

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody().get("status")).isEqualTo(404);
        assertThat((String) resposta.getBody().get("message")).contains("/api/auth/me");
    }
}
