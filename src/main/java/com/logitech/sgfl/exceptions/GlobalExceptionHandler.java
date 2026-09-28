package com.logitech.sgfl.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private Map<String, Object> baseBody(HttpStatus status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        return body;
    }

    @ExceptionHandler(VeiculoIncompativelException.class)
    public ResponseEntity<Map<String, Object>> handleVeiculoIncompativel(VeiculoIncompativelException ex) {
        return ResponseEntity.badRequest().body(baseBody(HttpStatus.BAD_REQUEST, "Veículo Incompatível", ex.getMessage()));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(baseBody(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage()));
    }

    // Cobre, por exemplo, StatusEntrega.valueOf("valor-invalido") e outros argumentos mal formados
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(baseBody(HttpStatus.BAD_REQUEST, "Requisição inválida", ex.getMessage()));
    }

    // Falhas de @Valid nos DTOs de entrada: devolve os campos que falharam, não uma mensagem genérica
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                fieldErrors.put(fe.getField(), fe.getDefaultMessage()));

        Map<String, Object> body = baseBody(HttpStatus.BAD_REQUEST, "Dados inválidos", "Um ou mais campos são inválidos");
        body.put("campos", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    // Falha de autenticação (ex.: credenciais erradas no /api/auth/login).
    // Mensagem genérica de propósito: não revela se foi "usuário não existe" ou "senha errada".
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(baseBody(HttpStatus.UNAUTHORIZED, "Não autorizado", "Credenciais inválidas."));
    }

    // Rede de segurança final: qualquer coisa não prevista vira 500 genérico.
    // O detalhe real vai só pro log do servidor, nunca pro cliente (evita vazar stack trace/estrutura interna).
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Erro não tratado", ex);
        return ResponseEntity.internalServerError()
                .body(baseBody(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Ocorreu um erro inesperado. Tente novamente mais tarde."));
    }
}
