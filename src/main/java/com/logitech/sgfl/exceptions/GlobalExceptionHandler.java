package com.logitech.sgfl.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    GlobalExceptionHandler.class
            );

    private Map<String, Object> baseBody(
            HttpStatus status,
            String error,
            String message
    ) {

        Map<String, Object> body =
                new LinkedHashMap<>();

        body.put(
                "timestamp",
                LocalDateTime.now()
        );

        body.put(
                "status",
                status.value()
        );

        body.put(
                "error",
                error
        );

        body.put(
                "message",
                message
        );

        String requestId =
                MDC.get("requestId");

        if (requestId != null) {
            body.put(
                    "requestId",
                    requestId
            );
        }

        return body;
    }

    @ExceptionHandler(
            VeiculoIncompativelException.class
    )
    public ResponseEntity<Map<String, Object>>
    handleVeiculoIncompativel(
            VeiculoIncompativelException ex
    ) {

        return ResponseEntity
                .badRequest()
                .body(
                        baseBody(
                                HttpStatus.BAD_REQUEST,
                                "Veículo incompatível",
                                ex.getMessage()
                        )
                );
    }

    @ExceptionHandler(
            RegraNegocioException.class
    )
    public ResponseEntity<Map<String, Object>>
    handleRegraNegocio(
            RegraNegocioException ex
    ) {

        return ResponseEntity
                .badRequest()
                .body(
                        baseBody(
                                HttpStatus.BAD_REQUEST,
                                "Regra de negócio inválida",
                                ex.getMessage()
                        )
                );
    }

    @ExceptionHandler(
            RecursoNaoEncontradoException.class
    )
    public ResponseEntity<Map<String, Object>>
    handleRecursoNaoEncontrado(
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

    /**
     * Rotas inexistentes em /api/** chegavam aqui como erro interno (500)
     * porque o Spring devolve NoResourceFoundException. Mantém o contrato
     * de erro da API com 404.
     */
    @ExceptionHandler(
            NoResourceFoundException.class
    )
    public ResponseEntity<Map<String, Object>>
    handleNoResourceFound(
            NoResourceFoundException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                        baseBody(
                                HttpStatus.NOT_FOUND,
                                "Recurso não encontrado",
                                "Não existe recurso em " + ex.getResourcePath() + "."
                        )
                );
    }

    @ExceptionHandler(
            DataIntegrityViolationException.class
    )
    public ResponseEntity<Map<String, Object>>
    handleDataIntegrityViolation(
            DataIntegrityViolationException ex
    ) {

        String mensagem =
                mensagemParaViolacaoConhecida(ex);

        if (mensagem == null) {
            mensagem =
                    "O registro está relacionado a outros dados do sistema e não pode ser removido desta forma.";
        }

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        baseBody(
                                HttpStatus.CONFLICT,
                                "Operação não permitida",
                                mensagem
                        )
                );
    }

    /**
     * Traduz as constraints criadas na migration V5 para mensagens claras.
     * É a rede de segurança para requisições concorrentes: a checagem no
     * serviço pode passar nas duas ao mesmo tempo, mas o banco só aceita uma.
     */
    private String mensagemParaViolacaoConhecida(
            DataIntegrityViolationException ex
    ) {

        String detalhe =
                ex.getMostSpecificCause().getMessage();

        if (detalhe == null) {
            return null;
        }

        String texto =
                detalhe.toLowerCase();

        if (texto.contains("uq_entrega_veiculo_em_transito")) {
            return "O veículo já está alocado em outra entrega EM_TRANSITO.";
        }

        if (texto.contains("uq_entrega_motorista_em_transito")) {
            return "O motorista já está alocado em outra entrega EM_TRANSITO.";
        }

        if (texto.contains("uk_veiculo_placa")) {
            return "Já existe um veículo cadastrado com esta placa.";
        }

        if (texto.contains("uk_motorista_cpf")) {
            return "Já existe um motorista cadastrado com este CPF.";
        }

        return null;
    }

    @ExceptionHandler(
            IllegalArgumentException.class
    )
    public ResponseEntity<Map<String, Object>>
    handleIllegalArgument(
            IllegalArgumentException ex
    ) {

        return ResponseEntity
                .badRequest()
                .body(
                        baseBody(
                                HttpStatus.BAD_REQUEST,
                                "Requisição inválida",
                                ex.getMessage()
                        )
                );
    }

    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ResponseEntity<Map<String, Object>>
    handleValidation(
            MethodArgumentNotValidException ex
    ) {

        Map<String, String> fieldErrors =
                new LinkedHashMap<>();

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
                        "Um ou mais campos são inválidos."
                );

        body.put(
                "campos",
                fieldErrors
        );

        return ResponseEntity
                .badRequest()
                .body(body);
    }

    @ExceptionHandler(
            AuthenticationException.class
    )
    public ResponseEntity<Map<String, Object>>
    handleAuthentication(
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

    @ExceptionHandler(
            HttpMessageNotReadableException.class
    )
    public ResponseEntity<Map<String, Object>>
    handleJsonMalformado(
            HttpMessageNotReadableException ex
    ) {

        return ResponseEntity
                .badRequest()
                .body(
                        baseBody(
                                HttpStatus.BAD_REQUEST,
                                "Requisição inválida",
                                "O corpo da requisição está mal formado ou contém um valor inválido."
                        )
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>>
    handleUnexpected(
            Exception ex
    ) {

        log.error(
                "Erro não tratado",
                ex
        );

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