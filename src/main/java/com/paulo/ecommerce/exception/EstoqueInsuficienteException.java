// exception/EstoqueInsuficienteException.java
package com.paulo.ecommerce.exception;

public class EstoqueInsuficienteException extends RuntimeException {
    public EstoqueInsuficienteException(String produto) {
        super("Estoque insuficiente para o produto: " + produto);
    }
}