package com.example.orders.domain.exception;

/** Pedido viola uma regra de domínio (ex.: sem itens). */
public class InvalidOrderException extends RuntimeException {
    public InvalidOrderException(String message) {
        super(message);
    }
}
