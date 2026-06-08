package com.example.orders.domain.exception;

/**
 * Transição de estado inválida no aggregate Order ou na saga.
 * Também serve de barreira de idempotência: uma resposta repetida da saga
 * cai num estado que não é mais o esperado e é rejeitada aqui.
 */
public class InvalidTransitionException extends RuntimeException {
    public InvalidTransitionException(String message) {
        super(message);
    }
}
