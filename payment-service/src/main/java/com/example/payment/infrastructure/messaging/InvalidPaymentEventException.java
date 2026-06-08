package com.example.payment.infrastructure.messaging;

/**
 * Lançada quando o payload ou header da mensagem Kafka é inválido.
 * Marcada como não-retentável no {@code DefaultErrorHandler}.
 */
public class InvalidPaymentEventException extends RuntimeException {

    public InvalidPaymentEventException(String message) {
        super(message);
    }

    public InvalidPaymentEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
