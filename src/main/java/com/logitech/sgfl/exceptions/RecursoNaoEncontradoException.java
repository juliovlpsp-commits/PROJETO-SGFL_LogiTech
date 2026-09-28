package com.logitech.sgfl.exceptions;

/**
 * Lançada quando um recurso buscado por ID (ou outra chave) não existe.
 * Mapeada para HTTP 404 pelo GlobalExceptionHandler.
 */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String message) {
        super(message);
    }
}
