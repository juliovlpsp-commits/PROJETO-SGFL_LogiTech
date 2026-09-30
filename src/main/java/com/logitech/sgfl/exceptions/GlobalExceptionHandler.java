package com.logitech.sgfl.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private Map<String, Object> baseBody(
            HttpStatus status,
            String error,
            String message
    ) {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);

        String reqId = MDC.get("requestId");

        if (reqId != null) {
            body.put("requestId", reqId);
        }

        return body;
    }

    @ExceptionHandler(VeiculoIncompativelException.class)
    public ResponseEntity<Map<String, Object>> handleVeiculoIncompativel(
            VeiculoIncompativelException ex
    ) {
        return ResponseEntity.badRequest()
                .body(
                        baseBody(
                                HttpStatus.BAD_REQUEST,
                                "Veículo Incompatível",
                                ex.getMessage()
                        )
                );
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Map<String, Object>> handleRegraNegocio(
            RegraNegocioException ex
    ) {
        return ResponseEntity.badRequest()
                .body(
                        baseBody(
                                HttpStatus.BAD_REQUEST,
                                "Regra de negócio inválida",
                                ex.getMessage()
                        )
                );
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleRecursoNaoEncontrado(
            RecursoNaoEncontradoException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                        baseBody(
                                HttpStatus.NOT_FOUND,
                                "Recurso não encontrado",
                                ex.getMessage()
                        )
                );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex
    ) {
        return ResponseEntity.badRequest()
                .body(
                        baseBody(
                                HttpStatus.BAD_REQUEST,
                                "Requisição inválida",
                                ex.getMessage()
                        )
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(
                        fieldError ->
                                fieldErrors.put(
                                        fieldError.getField(),
                                        fieldError.getDefaultMessage()
                                )
                );

        Map<String, Object> body =
                baseBody(
                        HttpStatus.BAD_REQUEST,
                        "Dados inválidos",
                        "Um ou mais campos são inválidos"
                );

        body.put("campos", fieldErrors);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(
            AuthenticationException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                        baseBody(
                                HttpStatus.UNAUTHORIZED,
                                "Não autorizado",
                                "Credenciais inválidas."
                        )
                );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonMalformado(
            HttpMessageNotReadableException ex
    ) {
        return ResponseEntity.badRequest()
                .body(
                        baseBody(
                                HttpStatus.BAD_REQUEST,
                                "Requisição inválida",
                                "O corpo da requisição está mal formado ou contém um valor inválido."
                        )
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(
            Exception ex
    ) {
        log.error("Erro não tratado", ex);

        return ResponseEntity
                .internalServerError()
                .body(
                        baseBody(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "Erro interno",
                                "Ocorreu um erro inesperado. Tente novamente mais tarde."
                        )
                );
    }
}