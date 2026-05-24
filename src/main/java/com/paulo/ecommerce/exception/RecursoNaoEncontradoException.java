// exception/RecursoNaoEncontradoException.java
package com.paulo.ecommerce.exception;

public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String recurso, Long id) {
        super(recurso + " com ID " + id + " não encontrado.");
    }
}