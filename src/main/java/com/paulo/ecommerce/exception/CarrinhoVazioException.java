// exception/CarrinhoVazioException.java
package com.paulo.ecommerce.exception;

public class CarrinhoVazioException extends RuntimeException {
    public CarrinhoVazioException() {
        super("O carrinho está vazio.");
    }
}