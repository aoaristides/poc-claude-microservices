package com.example.shipping.infrastructure.messaging;

/**
 * Lançada quando o payload ou header de um evento recebido é inválido.
 *
 * <p>Classificada como não-retryável no {@code DefaultErrorHandler}: payload corrompido
 * não melhora com retry — vai direto à DLT.
 */
public class InvalidEventException extends RuntimeException {

    public InvalidEventException(String message) {
        super(message);
    }

    public InvalidEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
