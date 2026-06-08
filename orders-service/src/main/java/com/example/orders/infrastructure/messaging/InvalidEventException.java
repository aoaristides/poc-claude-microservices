package com.example.orders.infrastructure.messaging;

/**
 * Mensagem recebida é inválida (header ausente, tipo desconhecido, payload corrompido).
 * Não deve ser retentada — vai direto para a DLT.
 */
public class InvalidEventException extends RuntimeException {
    public InvalidEventException(String message) {
        super(message);
    }

    public InvalidEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
